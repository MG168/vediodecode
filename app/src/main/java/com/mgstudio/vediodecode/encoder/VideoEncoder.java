package com.mgstudio.vediodecode.encoder;


import android.hardware.Camera;
import android.media.MediaCodec;
import android.os.Looper;
import android.view.Surface;
import android.view.SurfaceView;

import java.nio.ByteBuffer;

//视频编码器
public class VideoEncoder {

    private static final String TAG = "VIDEO_ENCODER_TAG";

    private Worker mWorker;

//    Video width
    private int mWidth;

//    Video Height
    private int mHeight;

    private SurfaceView mSurfaceview;

    protected Camera camera;
    protected Thread cameraThread;
    protected Looper cameraLooper;
    protected boolean cameraOpenedManually = true;
    protected int cameraId = 0;

    protected boolean surfaceReady = false;
    protected boolean unlocked = false;
    protected boolean previewStarted = false;
    protected boolean updated = false;
    protected boolean streaming = false;

    public VideoEncoder(SurfaceView surfaceView, int width, int height) {
        this.mSurfaceview = surfaceView;
        this.mWidth = width;
        this.mHeight = height;
    }

    // will call when surface will be created
    //将在创建表面时调用
    protected void onSurfaceCreated(Surface surface) {
        startPreview();
    }

    // will call before surface will be destroyed
    //将在表面被破坏之前调用
    protected void onSurfaceDestroyed(Surface surface) {
        stopPreview();
    }

    protected void onEncodedSample(MediaCodec.BufferInfo info, ByteBuffer data) {
    }

    public void start() {
        if (mWorker == null) {
            mWorker = new Worker();
            mWorker.setIsRunning(true);
            mWorker.start();
        }
    }





    // background thread which prepare MediaCodec and start encoding using surface-to-buffer method
    //后台线程，它准备MediaCodec并使用表面到缓冲区方法开始编码
    protected class Worker extends Thread {
    }
}
