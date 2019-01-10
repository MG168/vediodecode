package com.mgstudio.vediodecode.encoder;


import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import com.mgstudio.vediodecode.VideoCodecConstants;
import com.mgstudio.vediodecode.surface.SurfaceView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

//视频编码器
public class VideoEncoder {

    private static final String TAG = "VIDEO_ENCODER_TAG";

    private Worker mWorker;

//    Video width
    private int mWidth;

//    Video Height
    private int mHeight;

    private SurfaceView mSurfaceView;

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
        this.mSurfaceView = surfaceView;
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

    public void stop() {
        if (mWorker == null) {
            mWorker.setIsRunning(true);
            mWorker = null;
        }
    }

    public synchronized void startPreview() throws RuntimeException {
        cameraOpenedManually = true;
        if (!previewStarted) {
            createCamera();
            updateCamera();
        }
    }

    /**
     * Stops the preview.停止预览。
     */
    public synchronized void stopPreview() {
        cameraOpenedManually = false;
        stop();
    }

    private void openCamera() throws RuntimeException {
        final Semaphore lock = new Semaphore(0);
        final RuntimeException[] exception = new RuntimeException[1];
        cameraThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                cameraLooper = Looper.myLooper();
                try {
                    camera = Camera.open(cameraId);
                } catch (RuntimeException e) {
                    exception[0] = e;
                } finally {
                    lock.release();
                    Looper.loop();
                }
            }
        });
        cameraThread.start();
        lock.acquireUninterruptibly();
    }

    protected synchronized void createCamera() throws RuntimeException {
        if (mSurfaceView == null) {
            //throw new InvalidSurfaceException("Invalid surface !");
        }
        if (mSurfaceView.getHolder() == null || !surfaceReady) {
            // throw new InvalidSurfaceException("Invalid surface !");
        }

        if (camera == null) {
            openCamera();
            updated = false;
            unlocked = false;
            camera.setErrorCallback(new Camera.ErrorCallback() {

                @Override
                public void onError(int error, Camera camera) {
                    // On some phones when trying to use the camera facing front the media server
                    // 在某些手机上尝试使用面向媒体服务器前方的相机时
                    // will die. Whether or not this callback may be called really depends on the
                    // 会死。 是否可以调用此回调实际上取决于
                    // phone // 电话                
                    if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
                        // In this case the application must release the camera and instantiate a
                        //在这种情况下，应用程序必须释放相机并实例化a
                        // new one // 新的一个                       
                        Log.e(TAG, "Media server died !");
                        // We don't know in what thread we are so stop needs to be synchronized
                        //我们不知道我们在什么线程中所以需要同步停止
                        cameraOpenedManually = false;
                        stop();
                    } else {
                        Log.e(TAG, "Error unknown with the camera: " + error);
                    }
                }
            });

            try {
                // If the phone has a flash, we turn it on/off according to flashEnabled
                //如果手机有闪光灯，我们会根据flashEnabled打开/关闭它
                // setRecordingHint(true) is a very nice optimization if you plane to only use
                // setRecordingHint（true）是一个非常好的优化，如果你只使用飞机
                // the Camera for recording   //用于录制的相机            
                Camera.Parameters parameters = camera.getParameters();
                if (parameters.getFlashMode() != null) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                }
                parameters.setRecordingHint(true);
                camera.setParameters(parameters);
                camera.setDisplayOrientation(270);

                try {
                    mSurfaceView.startGLThread();
                    camera.setPreviewTexture(mSurfaceView.getSurfaceTexture());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (RuntimeException e) {
                destroyCamera();
                throw e;
            }
        }
    }

    protected synchronized void destroyCamera() {
        if (camera != null) {
            if (streaming) {
                //super.stop();
            }
            lockCamera();
            camera.stopPreview();
            try {
                camera.release();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage() != null ? e.getMessage() : "unknown error");
            }
            camera = null;
            cameraLooper.quit();
            unlocked = false;
            previewStarted = false;
        }
    }

    protected synchronized void updateCamera() throws RuntimeException {
        // The camera is already correctly configured//相机已正确配置
        if (updated) {
            return;
        }

        if (previewStarted) {
            previewStarted = false;
            camera.stopPreview();
        }

        Camera.Parameters parameters = camera.getParameters();

        mSurfaceView.requestAspectRatio(mWidth / mHeight);

        //parameters.setPreviewFormat(ImageFormat.YUV_420_888);
        parameters.setPreviewSize(mWidth, mHeight);
        //parameters.setPreviewFpsRange(30, 30);

        try {
            camera.setParameters(parameters);

            camera.setDisplayOrientation(270);

            camera.startPreview();
            previewStarted = true;
            updated = true;
        } catch (RuntimeException e) {
            destroyCamera();
            throw e;
        }
    }

    protected void lockCamera() {
        if (unlocked) {
            try {
                camera.reconnect();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            unlocked = false;
        }
    }

    protected void unlockCamera() {
        if (!unlocked) {
            Log.d(TAG, "Unlocking camera");
            try {
                camera.unlock();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            unlocked = true;
        }
    }

    // background thread which prepare MediaCodec and start encoding using surface-to-buffer method
    //后台线程，它准备MediaCodec并使用表面到缓冲区方法开始编码
    protected class Worker extends Thread {

        //
        private MediaCodec.BufferInfo mBufferInfo;

        // video codec which get access to hardware codec
        //可以访问硬件编解码器的视频编解码器
        private MediaCodec mCodec;

        // indicator for inner loop//内循环指标
        @NonNull
        private final AtomicBoolean mIsRunning = new AtomicBoolean(false);

        private Surface mSurface;

        private final long mTimeoutUsec;

        public Worker() {
            this.mBufferInfo = new MediaCodec.BufferInfo();
            this.mTimeoutUsec = 10000L;
        }

        public void setIsRunning(boolean running) {
            mIsRunning.set(running);
        }

        @NonNull
        public AtomicBoolean isRunning() {
            return mIsRunning;
        }

        @Override
        public void run() {
            // prepare video codec//准备视频编解码器
            prepare();

            try {
                while (mIsRunning.get()) {
                    // encode video sources from input buffer//从输入缓冲区编码视频源
                    encode();
                }

                encode();
            } finally {
                // release video codec resourses//释放视频编解码器资源
                release();
            }
        }

        void encode() {
            if (!mIsRunning.get()) {
                // if not running anymore, complete stream//如果不再运行，请完整流
                mCodec.signalEndOfInputStream();
            }

            // pre-lollipop api
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

                // get output buffers
                ByteBuffer[] outputBuffers = mCodec.getOutputBuffers();
                for (; ; ) {
                    //get status
                    int status = mCodec.dequeueOutputBuffer(mBufferInfo, mTimeoutUsec);
                    if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // something wrong with codec - need try again
                        if (!mIsRunning.get()) {
                            break;
                        }
                    } else if (status == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        // need get new output buffers
                        outputBuffers = mCodec.getOutputBuffers();
                    } else if (status >= 0) {

                        // encoded sample//编码样本
                        ByteBuffer data = outputBuffers[status];
                        data.position(mBufferInfo.offset);
                        data.limit(mBufferInfo.offset + mBufferInfo.size);

                        final int endOfStream = mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                        if (endOfStream == 0) {
                            onEncodedSample(mBufferInfo, data);
                        }
                        // releasing buffer is important//释放缓冲区很重要
                        mCodec.releaseOutputBuffer(status, false);

                        // don't have any buffers - need finish
                        if (endOfStream == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                            break;
                        }
                    }
                }
            } else {
                for (; ; ) {
                    //get status
                    int status = mCodec.dequeueOutputBuffer(mBufferInfo, mTimeoutUsec);
                    if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // something wrong with codec - need try again
                        if (!mIsRunning.get()) {
                            break;
                        }
                    } else if (status >= 0) {
                        // encoded sample
                        ByteBuffer data = mCodec.getOutputBuffer(status);
                        if (data != null) {

                            final int endOfStream = mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                            if (endOfStream == 0) {
                                onEncodedSample(mBufferInfo, data);
                            }
                            // release buffer
                            mCodec.releaseOutputBuffer(status, false);

                            // don't have any buffers - need finish
                            if (endOfStream == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        // release all resources
        private void release() {
            onSurfaceDestroyed(mSurface);

            mCodec.stop();
            mCodec.release();
            mSurface.release();
        }

        private MediaFormat getOutputFormat() {
            return mCodec.getOutputFormat();
        }

        private void prepare() {
            // configure video output//配置视频输出
            MediaFormat format = MediaFormat.createVideoFormat(VideoCodecConstants.VIDEO_CODEC, mWidth, mHeight);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, VideoCodecConstants.VIDEO_BITRATE);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, VideoCodecConstants.VIDEO_FPS);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VideoCodecConstants.VIDEO_FI);

            try {
                mCodec = MediaCodec.createEncoderByType(VideoCodecConstants.VIDEO_CODEC);
            } catch (IOException e) {
                // can not create avc codec - throw exception//无法创建avc编解码器 - 抛出异常
                throw new RuntimeException(e);
            }
            mCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            // create surface associated with code//创建与代码关联的表面
            mSurface = mCodec.createInputSurface();
            mSurfaceView.addMediaCodecSurface(mSurface);
            // notify codec to start watch surface and encode samples
            //通知编解码器开始观察表面并对样本进行编码
            mCodec.start();

            onSurfaceCreated(mSurface);
        }
    }
}
