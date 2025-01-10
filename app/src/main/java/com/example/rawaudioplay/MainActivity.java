package com.example.rawaudioplay;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.os.Bundle;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.rawaudioplay.databinding.ActivityMainBinding;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private AudioTrack audioTrack;

    private Button startRecorder;
    private Button stopRecorder;

    private Button playButton;
    private TextView recordingStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // 权限未被授予，进行请求
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        } else {
            // 权限已经被授予，可以进行文件写入操作
        }



        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stopRecorder = findViewById(R.id.stopButton);
        playButton = findViewById(R.id.playButton);
        startRecorder = findViewById(R.id.startRecorder);
        recordingStatus = findViewById(R.id.recordingStatus);

        RawAudioRecorder recorder = new RawAudioRecorder(this);

        startRecorder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 开始录音时更新UI状态
                startRecorder.setEnabled(false);  // 禁用开始按钮
                stopRecorder.setEnabled(true);    // 启用停止按钮
                playButton.setEnabled(false);     // 禁用播放按钮
                recordingStatus.setVisibility(View.VISIBLE);  // 显示录音状态
                
                recorder.startRecording(MainActivity.this);
            }
        });

        stopRecorder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 停止录音时更新UI状态
                startRecorder.setEnabled(true);   // 重新启用开始按钮
                stopRecorder.setEnabled(false);   // 禁用停止按钮
                playButton.setEnabled(true);      // 启用播放按钮
                recordingStatus.setVisibility(View.INVISIBLE);  // 隐藏录音状态
                
                recorder.stopRecording();
            }
        });

        // 初始状态设置
        stopRecorder.setEnabled(false);  // 初始时停止按钮不可用
        
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 替换为你的音频文件的网络地址
               playRawAudio();
            }
        });
    }

    private void playRawAudio() {
        // 音频参数
        int sampleRate = 8000; // 采样率
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO; // 单声道
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT; // 16位PCM

        // 计算缓冲区大小
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig, audioFormat, bufferSize, AudioTrack.MODE_STREAM);

        // 开始播放
        audioTrack.play();

        // 读取raw资源文件
        InputStream inputStream = getResources().openRawResource(R.raw.sample);
        byte[] buffer = new byte[bufferSize];
        int read;

        try {
            while ((read = inputStream.read(buffer)) > 0) {
                audioTrack.write(buffer, 0, read);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading raw audio file", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing input stream", e);
            }
            audioTrack.stop();
            audioTrack.release();
        }
    }
}