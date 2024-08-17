package com.example.rawaudioplay;

import android.os.Bundle;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import com.example.rawaudioplay.databinding.ActivityMainBinding;
import java.io.IOException;
import java.io.InputStream;
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private AudioTrack audioTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playRawAudio();
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