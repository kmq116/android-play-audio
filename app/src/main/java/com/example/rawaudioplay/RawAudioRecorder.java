package com.example.rawaudioplay;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class RawAudioRecorder extends AppCompatActivity {
    private static final int SAMPLE_RATE = 8000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread;
    private Context context;

    public RawAudioRecorder(Context context) {
        this.context = context;
    }

    public void startRecording(Context context) {

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);

        audioRecord.startRecording();
        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("录制中");
                writeAudioDataToFile(context);
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }


    private void writeAudioDataToFile(Context context) {
        FileOutputStream os = null;
        byte[] data = new byte[1024];

        // 创建应用的目录
        File appDir = new File(context.getExternalFilesDir(null), "rawaudioplay");
        if (!appDir.exists()) {
            appDir.mkdirs(); // 创建目录
        }



        try {
            long lastFileCreationTime = System.currentTimeMillis();
            int fileIndex = 0; // 用于生成文件名
            while (isRecording) {
// 检查是否超过一秒
                // 获取当前时间
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastFileCreationTime >= 1000) {
                    // 更新上次文件创建时间
                    lastFileCreationTime = currentTime;

                    // 创建新的录音文件
                    File audioFile = new File(appDir, "test_" + fileIndex + ".raw");
                    fileIndex++; // 增加文件索引

                    try {
                        os = new FileOutputStream(audioFile);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        continue; // 如果文件创建失败，跳过当前循环
                    }
                }

                // 读取录音数据
                int read = audioRecord.read(data, 0, data.length);
                if (read > 0 && os != null) {
                    try {
                        os.write(data, 0, read);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public void stopRecording() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (recordingThread != null) {
            try {
                recordingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            recordingThread = null;
        }
    }
}