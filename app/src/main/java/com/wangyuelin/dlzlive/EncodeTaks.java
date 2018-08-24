package com.wangyuelin.dlzlive;

import android.media.MediaCodec;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.wangyuelin.dlzlive.helper.EncoderHelper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
import static android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME;

/**
 * 编码线程
 */
public class EncodeTaks implements Runnable {
    private boolean isRunning;
    private FrameQueue mFrameQueue;
    private int w;
    private int h;
    private int framerate;
    private int bitrate;
    private MediaCodec mMediaCodec;
    private long pts;
    private long generateIndex;
    private int TIMEOUT_USEC = 12000;
    private byte[] configbyte;
    private static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test1.h264";
    private BufferedOutputStream outputStream;


    public EncodeTaks(FrameQueue mFrameQueue, int w, int h, int framerate, int bitrate) {
        this.mFrameQueue = mFrameQueue;
        this.w = w;
        this.h = h;
        this.framerate = framerate;
        this.bitrate = bitrate;
        isRunning = true;
    }


    private void createfile() {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void run() {
        if (EncoderHelper.getInstance().getMediaCodec() == null) {
            EncoderHelper.getInstance().initEncoder(h, w, framerate, bitrate);
        }
        mMediaCodec = EncoderHelper.getInstance().getMediaCodec();
        if (mMediaCodec == null) {
            Log.d("wyl", "编码器初始化失败");
            return;
        }

        createfile();

        while (isRunning) {
            byte[] frame = mFrameQueue.getFrame();
            if (frame == null) {
                continue;
            }
            byte[] nv12Frame = new byte[w * h * 3 / 2];
            EncoderHelper.getInstance().nV21ToNV12(frame, nv12Frame, w, h);
            //编码器输入缓冲区
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            //编码器输出缓冲区
            ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                pts = computePresentationTime(generateIndex);
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                //把转换后的YUV420格式的视频帧放到编码器输入缓冲区中
                inputBuffer.put(nv12Frame);
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, nv12Frame.length, pts, 0);
                generateIndex += 1;
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            try {
                while (outputBufferIndex >= 0) {
                    //Log.i("AvcEncoder", "Get H264 Buffer Success! flag = "+bufferInfo.flags+",pts = "+bufferInfo.presentationTimeUs+"");
                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                    byte[] outData = new byte[bufferInfo.size];
                    outputBuffer.get(outData);
                    if (bufferInfo.flags == BUFFER_FLAG_CODEC_CONFIG) {
                        configbyte = outData;
                    } else if (bufferInfo.flags == BUFFER_FLAG_KEY_FRAME) {
                        byte[] keyframe = new byte[bufferInfo.size + configbyte.length];
                        System.arraycopy(configbyte, 0, keyframe, 0, configbyte.length);
                        //把编码后的视频帧从编码器输出缓冲区中拷贝出来
                        System.arraycopy(outData, 0, keyframe, configbyte.length, outData.length);


                        outputStream.write(keyframe, 0, keyframe.length);

                    } else {
                        //写到文件中
                        outputStream.write(outData, 0, outData.length);
                    }

                    mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / framerate;
    }


    public void stop() {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        isRunning = false;
    }
}
