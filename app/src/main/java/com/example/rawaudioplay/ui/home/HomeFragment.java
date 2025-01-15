package com.example.rawaudioplay.ui.home;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.rawaudioplay.AudioStreamManager;
import com.example.rawaudioplay.R;
import com.example.rawaudioplay.RawAudioRecorder;

import java.io.File;
import java.io.IOException;

public class HomeFragment extends Fragment {
    private Button startRecorder;
    private Button stopRecorder;
    private Button startStreamButton;
    private Button stopStreamButton;
    private Button playButton;
    private TextView recordingStatus;
    private MediaPlayer mediaPlayer;
    private String audioFilePath;
    private SeekBar playbackSeekBar;
    private TextView currentTimeText;
    private TextView totalTimeText;
    private Handler handler = new Handler();
    private boolean isTracking = false;
    private AudioStreamManager audioStreamManager;

    public View onCreateView(@NonNull LayoutInflater inflater,
                            ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        audioStreamManager = new AudioStreamManager(requireContext());

        // 初始化视图
        stopRecorder = root.findViewById(R.id.stopButton);
        playButton = root.findViewById(R.id.playButton);
        startRecorder = root.findViewById(R.id.startRecorder);
        recordingStatus = root.findViewById(R.id.recordingStatus);
        startStreamButton = root.findViewById(R.id.startStreamButton);
        stopStreamButton = root.findViewById(R.id.stopStreamButton);
        playbackSeekBar = root.findViewById(R.id.playbackSeekBar);
        currentTimeText = root.findViewById(R.id.currentTimeText);
        totalTimeText = root.findViewById(R.id.totalTimeText);

        RawAudioRecorder recorder = new RawAudioRecorder(requireContext());

        startRecorder.setOnClickListener(v -> {
            startRecorder.setEnabled(false);
            stopRecorder.setEnabled(true);
            playButton.setEnabled(false);
            recordingStatus.setVisibility(View.VISIBLE);
            
            audioFilePath = requireContext().getExternalCacheDir().getAbsolutePath() + "/recorded_audio.m4a";
            recorder.setOutputFile(audioFilePath);
            recorder.startRecording(requireContext());
        });

        stopRecorder.setOnClickListener(v -> {
            startRecorder.setEnabled(true);
            stopRecorder.setEnabled(false);
            playButton.setEnabled(true);
            recordingStatus.setVisibility(View.INVISIBLE);
            
            recorder.stopRecording();
        });

        // 初始状态设置
        stopRecorder.setEnabled(false);

        // 设置SeekBar监听器
        playbackSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    updateCurrentTimeText(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                }
                isTracking = false;
            }
        });

        playButton.setOnClickListener(v -> {
            if (audioFilePath != null) {
                File audioFile = new File(audioFilePath);
                if (!audioFile.exists()) {
                    Toast.makeText(requireContext(), "录音文件不存在", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                    }
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(audioFilePath);
                    mediaPlayer.prepareAsync();

                    mediaPlayer.setOnPreparedListener(mp -> {
                        playbackSeekBar.setMax(mp.getDuration());
                        updateTotalTimeText(mp.getDuration());
                        mp.start();
                        startProgressUpdate();
                    });

                    mediaPlayer.setOnCompletionListener(mp -> {
                        mp.release();
                        mediaPlayer = null;
                    });

                    mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                        Toast.makeText(requireContext(), "播放出错: " + what, Toast.LENGTH_SHORT).show();
                        if (mp != null) {
                            mp.release();
                            mediaPlayer = null;
                        }
                        return true;
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "播放失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                }
            } else {
                Toast.makeText(requireContext(), "请先录制音频", Toast.LENGTH_SHORT).show();
            }
        });

        startStreamButton.setOnClickListener(v -> {
            String rtmpUrl = "rtmp://imn.tiananborui.com:1935/x";
            String streamKey = "h77KWA6Xz9Pa";
            String fullRtmpUrl = rtmpUrl + "/" + streamKey;
            
            audioStreamManager.startStreaming(fullRtmpUrl);
            startStreamButton.setEnabled(false);
            stopStreamButton.setEnabled(true);
            Toast.makeText(requireContext(), "开始推流", Toast.LENGTH_SHORT).show();
        });

        stopStreamButton.setOnClickListener(v -> {
            audioStreamManager.stopStreaming();
            startStreamButton.setEnabled(true);
            stopStreamButton.setEnabled(false);
            Toast.makeText(requireContext(), "停止推流", Toast.LENGTH_SHORT).show();
        });

        return root;
    }

    private void startProgressUpdate() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying() && !isTracking) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    playbackSeekBar.setProgress(currentPosition);
                    updateCurrentTimeText(currentPosition);
                    handler.postDelayed(this, 100);
                }
            }
        }, 100);
    }

    private void updateCurrentTimeText(int milliseconds) {
        currentTimeText.setText(formatTime(milliseconds));
    }

    private void updateTotalTimeText(int milliseconds) {
        totalTimeText.setText(formatTime(milliseconds));
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (audioStreamManager != null) {
            audioStreamManager.release();
        }
    }
}