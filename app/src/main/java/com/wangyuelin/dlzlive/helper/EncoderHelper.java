package com.wangyuelin.dlzlive.helper;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.wangyuelin.dlzlive.EncodeTaks;
import com.wangyuelin.dlzlive.FrameQueue;

import java.io.IOException;

public class EncoderHelper {
    private static class EncoderHelperHolder {
        static EncoderHelper encoderHelper = new EncoderHelper();
    }

    private EncoderHelper() {
    }

    public static EncoderHelper getInstance() {
        return EncoderHelperHolder.encoderHelper;
    }

    private int mWidth;
    private int mHeight;
    private int mFrameRate;
    private int mBitRate;
    private MediaCodec mMediaCodec;
    private FrameQueue mFrameQueue = new FrameQueue();
    private EncodeTaks mEncodeTaks;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void initEncoder(int width, int height, int framerate, int bitrate) {
        if (mMediaCodec != null) {
            return;
        }

        mWidth = width;
        mHeight = height;
        mFrameRate = framerate;
        mBitRate = bitrate;

        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        try {
            mMediaCodec = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.d("wyl", e.getLocalizedMessage());
        }
        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
    }

    /**
     * 将nv21转为nv12
     *
     * @param nv21
     * @param nv12
     * @param width
     * @param height
     */
    public void nV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) return;
        int framesize = width * height;
        int i = 0, j = 0;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for (i = 0; i < framesize; i++) {
            nv12[i] = nv21[i];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j - 1] = nv21[j + framesize];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j] = nv21[j + framesize - 1];
        }
    }

    /**
     * 开始编码
     */
    public void startEncode() {
        if (mEncodeTaks == null) {
            mEncodeTaks = new EncodeTaks(mFrameQueue, mWidth, mHeight, mFrameRate, mBitRate);
            new Thread(mEncodeTaks).start();
        }


    }


    public MediaCodec getMediaCodec() {
        return mMediaCodec;
    }

    public FrameQueue getFrameQueue() {
        return mFrameQueue;
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void release() {
        if (mEncodeTaks != null) {
            mEncodeTaks.stop();
        }

        if (mMediaCodec == null) {
            return;
        }
        mMediaCodec.stop();
        mMediaCodec.release();

    }
}
