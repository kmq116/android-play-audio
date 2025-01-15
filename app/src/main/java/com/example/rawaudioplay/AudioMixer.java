package com.example.rawaudioplay;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioMixer {
    private static final String TAG = "AudioMixer";
    private File pcmFile;
    private FileInputStream fileInputStream;
    private boolean isLooping = true;
    private float mixVolume = 0.5f; // PCM文件的音量，范围0-1

    public AudioMixer(String pcmFilePath) {
        pcmFile = new File(pcmFilePath);
        try {
            fileInputStream = new FileInputStream(pcmFile);
        } catch (IOException e) {
            Log.e(TAG, "Failed to open PCM file", e);
        }
    }

    public void setMixVolume(float volume) {
        this.mixVolume = Math.max(0, Math.min(1, volume));
    }

    public byte[] mixAudio(byte[] micBuffer, int micBufferSize) {
        if (fileInputStream == null) return micBuffer;

        // 将字节数组转换为short数组，因为PCM 16bit数据是short类型
        short[] micShorts = new short[micBufferSize / 2];
        short[] fileShorts = new short[micBufferSize / 2];
        ByteBuffer.wrap(micBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(micShorts);

        try {
            byte[] fileBuffer = new byte[micBufferSize];
            int bytesRead = fileInputStream.read(fileBuffer);

            // 如果读到文件末尾，重新开始
            if (bytesRead < 0) {
                if (isLooping) {
                    fileInputStream.getChannel().position(0);
                    bytesRead = fileInputStream.read(fileBuffer);
                } else {
                    return micBuffer;
                }
            }

            // 将PCM文件数据转换为short数组
            ByteBuffer.wrap(fileBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(fileShorts);

            // 混音处理
            for (int i = 0; i < micShorts.length; i++) {
                // 将两个音频信号按照设定的音量比例混合
                float mixed = (micShorts[i] * (1 - mixVolume) + fileShorts[i] * mixVolume);
                // 防止音频信号超出范围
                if (mixed > Short.MAX_VALUE) mixed = Short.MAX_VALUE;
                if (mixed < Short.MIN_VALUE) mixed = Short.MIN_VALUE;
                micShorts[i] = (short) mixed;
            }

            // 将混音后的short数组转换回字节数组
            ByteBuffer mixedBuffer = ByteBuffer.allocate(micBufferSize);
            mixedBuffer.order(ByteOrder.LITTLE_ENDIAN);
            for (short s : micShorts) {
                mixedBuffer.putShort(s);
            }

            return mixedBuffer.array();
        } catch (IOException e) {
            Log.e(TAG, "Error reading PCM file", e);
            return micBuffer;
        }
    }

    public void release() {
        if (fileInputStream != null) {
            try {
                fileInputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing PCM file", e);
            }
        }
    }
} 