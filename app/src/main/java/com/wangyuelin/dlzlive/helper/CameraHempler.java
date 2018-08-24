package com.wangyuelin.dlzlive.helper;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

public class CameraHempler {
    public enum CameraPos{
        BACK(0),//后置摄像头
        PRE(1);//前置摄像头

        CameraPos(int whitch) {
            this.whitch = whitch;
        }

        int whitch;
    }

    private Camera mCamera;

    private CameraHempler() {
    }

    private static class CameraHemplerHolder{
        private static  CameraHempler helper = new CameraHempler();
    }

    public static CameraHempler getInstance() {
        return CameraHemplerHolder.helper;
    }


    /**
     * 开始预览
     * @param cameraPos
     * @param surfaceHolder
     * @param w
     * @param h
     * @param cb
     */
    public void startPreview(CameraPos cameraPos , SurfaceHolder surfaceHolder, int w, int h, Camera.PreviewCallback cb) {
        if (surfaceHolder == null) {
            return;
        }
        if (cameraPos == null) {
            cameraPos = CameraPos.BACK;
        }




        mCamera = Camera.open(cameraPos.whitch);
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
        if (sizeList != null && sizeList.size() > 0) {
            w = sizeList.get(0).width;
            h = sizeList.get(0).height;
        }
        Log.d("wyl", "尺寸：w:" + w + " h:" + h);
        parameters.setPreviewSize(w, h);
        parameters.setPictureSize(w, h);
        parameters.setPreviewFormat(ImageFormat.NV21);
        parameters.setRotation(90);

        mCamera.setDisplayOrientation(90);
        mCamera.setParameters(parameters);
        if (cb != null) {
            mCamera.setPreviewCallback(cb);
        }
        mCamera.startPreview();

    }

    public void release() {

        if (mCamera == null) {
            return;
        }
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;

    }


}
