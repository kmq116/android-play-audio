package com.example.rawaudioplay;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import com.pedro.rtmp.rtmp.RtmpClient;
import com.pedro.rtmp.utils.ConnectCheckerRtmp;

import java.nio.ByteBuffer;

public class AudioStreamManager implements ConnectCheckerRtmp {
    private static final String TAG = "AudioStreamManager";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BITRATE = 128 * 1024;  // 128 kbps

    private Context context;
    private RtmpClient rtmpClient;
    private boolean isStreaming = false;
    private AudioRecord audioRecord;
    private MediaCodec audioEncoder;
    private Thread audioThread;
    private long presentationTimeUs = 0;

    public AudioStreamManager(Context context) {
        this.context = context;
        rtmpClient = new RtmpClient(this);
        
        // 初始化AudioRecord
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);
                
        try {
            // 初始化音频编码器
            MediaFormat audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 1);
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BITRATE);
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192);
            
            audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            audioEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create audio encoder", e);
        }
    }

    public void startStreaming(String rtmpUrl) {
        if (isStreaming) return;

        try {
            rtmpClient.connect(rtmpUrl);
            audioEncoder.start();
            isStreaming = true;
            
            // 开始录音并推流
            audioRecord.startRecording();
            audioThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[2048];
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    
                    while (isStreaming && !Thread.interrupted()) {
                        int len = audioRecord.read(buffer, 0, buffer.length);
                        if (len > 0 && rtmpClient.isStreaming()) {
                            // 将PCM数据送入编码器
                            int inputBufferIndex = audioEncoder.dequeueInputBuffer(-1);
                            if (inputBufferIndex >= 0) {
                                ByteBuffer inputBuffer = audioEncoder.getInputBuffer(inputBufferIndex);
                                inputBuffer.clear();
                                inputBuffer.put(buffer, 0, len);
                                audioEncoder.queueInputBuffer(inputBufferIndex, 0, len, presentationTimeUs, 0);
                                presentationTimeUs += (len * 1000000L) / (SAMPLE_RATE * 2);
                            }
                            
                            // 获取编码后的AAC数据
                            int outputBufferIndex = audioEncoder.dequeueOutputBuffer(bufferInfo, 0);
                            while (outputBufferIndex >= 0) {
                                ByteBuffer outputBuffer = audioEncoder.getOutputBuffer(outputBufferIndex);
                                
                                // 发送AAC数据
                                if (bufferInfo.size > 0 && outputBuffer != null) {
                                    outputBuffer.position(bufferInfo.offset);
                                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                                    
                                    // 添加ADTS头
                                    byte[] adtsHeader = createAdtsHeader(bufferInfo.size);
                                    ByteBuffer data = ByteBuffer.allocate(adtsHeader.length + bufferInfo.size);
                                    data.put(adtsHeader);
                                    data.put(outputBuffer);
                                    data.flip();
                                    
                                    // 创建新的BufferInfo
                                    MediaCodec.BufferInfo newInfo = new MediaCodec.BufferInfo();
                                    newInfo.offset = 0;
                                    newInfo.size = data.limit();
                                    newInfo.presentationTimeUs = bufferInfo.presentationTimeUs;
                                    newInfo.flags = bufferInfo.flags;
                                    
                                    rtmpClient.sendAudio(data, newInfo);
                                }
                                
                                audioEncoder.releaseOutputBuffer(outputBufferIndex, false);
                                outputBufferIndex = audioEncoder.dequeueOutputBuffer(bufferInfo, 0);
                            }
                        }
                    }
                }
            });
            audioThread.start();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start streaming", e);
            isStreaming = false;
        }
    }

    // 创建ADTS头
    private byte[] createAdtsHeader(int length) {
        int frameLength = length + 7; // ADTS头长度为7
        byte[] header = new byte[7];
        
        // Sync word
        header[0] = (byte) 0xFF;
        header[1] = (byte) 0xF1;
        
        // Profile, Sampling frequency, Channel config
        header[2] = (byte) ((MediaCodecInfo.CodecProfileLevel.AACObjectLC - 1) << 6);
        header[2] |= (4 << 2);  // 44100Hz
        header[2] |= (1 >> 2);  // 单声道
        
        header[3] = (byte) ((1 & 3) << 6);
        header[3] |= (frameLength >> 11);
        header[4] = (byte) ((frameLength >> 3) & 0xFF);
        header[5] = (byte) (((frameLength & 7) << 5) | 0x1F);
        header[6] = (byte) 0xFC;
        
        return header;
    }

    public void stopStreaming() {
        if (!isStreaming) return;
        
        isStreaming = false;
        if (audioThread != null) {
            audioThread.interrupt();
            audioThread = null;
        }
        
        if (audioRecord != null) {
            audioRecord.stop();
        }
        
        if (audioEncoder != null) {
            audioEncoder.stop();
            audioEncoder.flush();
        }
        
        rtmpClient.disconnect();
    }

    public void release() {
        stopStreaming();
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
        if (audioEncoder != null) {
            audioEncoder.release();
            audioEncoder = null;
        }
    }

    public boolean isStreaming() {
        return isStreaming;
    }

    @Override
    public void onConnectionSuccessRtmp() {
        Log.d(TAG, "RTMP connection success");
    }

    @Override
    public void onConnectionFailedRtmp(String reason) {
        Log.e(TAG, "RTMP connection failed: " + reason);
        isStreaming = false;
    }

    @Override
    public void onNewBitrateRtmp(long bitrate) {
        Log.d(TAG, "RTMP new bitrate: " + bitrate);
    }

    @Override
    public void onDisconnectRtmp() {
        Log.d(TAG, "RTMP disconnected");
        isStreaming = false;
    }

    @Override
    public void onAuthErrorRtmp() {
        Log.e(TAG, "RTMP auth error");
        isStreaming = false;
    }

    @Override
    public void onAuthSuccessRtmp() {
        Log.d(TAG, "RTMP auth success");
    }

    @Override
    public void onConnectionStartedRtmp(String rtmpUrl) {
        Log.d(TAG, "RTMP connection started");
    }
} 