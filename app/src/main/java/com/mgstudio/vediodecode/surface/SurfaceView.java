package com.mgstudio.vediodecode.surface;


import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.concurrent.Semaphore;

/**
 * An enhanced SurfaceView in which the camera preview will be rendered. *增强的SurfaceView，其中将呈现相机预览。
 * This class was needed for two reasons. <br /> 这个类有两个原因需要。
 * <p>
 * First, it allows to use to feed MediaCodec with the camera preview 首先，它允许使用摄像头预览来提供MediaCodec
 * using the surface-to-buffer method while rendering it in a surface在表面渲染时使用表面到缓冲方法
 * visible to the user.对用户可见。
 * <p>
 * Second, it allows to force the aspect ratio of the SurfaceView 其次，它允许强制SurfaceView的宽高比
 * to match the aspect ratio of the camera preview, so that the 匹配相机预览的宽高比，以便
 * preview do not appear distorted to the user of your app. To do 预览不会对应用程序的用户造成扭曲。 去做
 * that, call {@link SurfaceView#setAspectRatioMode(int)} with  用{@link SurfaceView＃setAspectRatioMode（int）}调用
 * {@link SurfaceView#ASPECT_RATIO_PREVIEW} after creating your  {@link SurfaceView＃ASPECT_RATIO_PREVIEW}创建后
 * {@link SurfaceView}. <br />   {@link SurfaceView}。
 */
public class SurfaceView extends android.view.SurfaceView implements Runnable,
        SurfaceTexture.OnFrameAvailableListener, SurfaceHolder.Callback {

    public final static String TAG = "SurfaceView";

    /**
     * The aspect ratio of the surface view will be equal
     * to the aspect ration of the camera preview.表面视图的纵横比将等于相机预览的纵横比。
     **/
    public static final int ASPECT_RATIO_PREVIEW = 0x01;

    /**
     * The surface view will fill completely fill its parent.
     * 表面视图将填充完全填充其父级。
     */
    public static final int ASPECT_RATIO_STRETCH = 0x00;

    private Thread mThread = null;
    private Handler mHandler = null;
    private boolean mFrameAvailable = false;
    private boolean mRunning = true;
    private int mAspectRatioMode = ASPECT_RATIO_STRETCH;

    // The surface in which the preview is rendered 渲染预览的表面
    private SurfaceManager mViewSurfaceManager = null;

    // The input surface of the MediaCodec  MediaCodec的输入表面
    private SurfaceManager mCodecSurfaceManager = null;

    // Handles the rendering of the SurfaceTexture we got/处理我们得到的SurfaceTexture的渲染
    // from the camera, onto a Surface   //从相机到Surface
    private TextureManager mTextureManager = null;

    private final Semaphore mLock = new Semaphore(0);
    private final Object mSyncObject = new Object();

    // Allows to force the aspect ratio of the preview //允许强制预览的宽高比
    private ViewAspectRatioMeasurer mVARM = new ViewAspectRatioMeasurer();

    private Integer measuredWidth = null;

    public SurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHandler = new Handler();
        getHolder().addCallback(this);
    }

    public void setAspectRatioMode(int mode) {
        mAspectRatioMode = mode;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mTextureManager.getSurfaceTexture();
    }

    public void addMediaCodecSurface(Surface surface) {
        synchronized (mSyncObject) {
            mCodecSurfaceManager = new SurfaceManager(surface, mViewSurfaceManager);
        }
    }

    public void removeMediaCodecSurface() {
        synchronized (mSyncObject) {
            if (mCodecSurfaceManager != null) {
                mCodecSurfaceManager.release();
                mCodecSurfaceManager = null;
            }
        }
    }

    public void startGLThread() {
        Log.d(TAG, "Thread started.");
        if (mTextureManager == null) {
            mTextureManager = new TextureManager();
        }
        if (mTextureManager.getSurfaceTexture() == null) {
            mThread = new Thread(SurfaceView.this);
            mRunning = true;
            mThread.start();
            mLock.acquireUninterruptibly();
        }
    }

    @Override
    public void run() {
        mViewSurfaceManager = new SurfaceManager(getHolder().getSurface());
        mViewSurfaceManager.makeCurrent();
        mTextureManager.createTexture().setOnFrameAvailableListener(this);

        mLock.release();

        try {
            long ts = 0, oldts = 0;
            while (mRunning) {
                synchronized (mSyncObject) {
                    mSyncObject.wait(2500);
                    if (mFrameAvailable) {
                        mFrameAvailable = false;

                        mViewSurfaceManager.makeCurrent();
                        mTextureManager.updateFrame();
                        mTextureManager.drawFrame();
                        mViewSurfaceManager.swapBuffer();

                        if (mCodecSurfaceManager != null) {
                            mCodecSurfaceManager.makeCurrent();
                            mTextureManager.drawFrame();
                            oldts = ts;
                            ts = mTextureManager.getSurfaceTexture().getTimestamp();
                            //Log.d(TAG,"FPS: "+(1000000000/(ts-oldts)));
                            mCodecSurfaceManager.setPresentationTime(ts);
                            mCodecSurfaceManager.swapBuffer();
                        }

                    } else {
                        Log.e(TAG, "No frame received !");
                    }
                }
            }
        } catch (InterruptedException ignore) {
        } finally {
            mViewSurfaceManager.release();
            mTextureManager.release();
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (mSyncObject) {
            mFrameAvailable = true;
            mSyncObject.notifyAll();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mThread != null) {
            mThread.interrupt();
        }
        mRunning = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mVARM.getAspectRatio() > 0 && mAspectRatioMode == ASPECT_RATIO_PREVIEW) {
            mVARM.measure(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(mVARM.getMeasuredWidth(), mVARM.getMeasuredHeight());
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public void requestAspectRatio(double aspectRatio) {
        if (mVARM.getAspectRatio() != aspectRatio) {
            mVARM.setAspectRatio(aspectRatio);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mAspectRatioMode == ASPECT_RATIO_PREVIEW) {
                        requestLayout();
                    }
                }
            });
        }
    }

    /**
     * This class is a helper to measure views that require a specific aspect ratio.
     *此类是测量需要特定宽高比的视图的辅助工具。
     * @author Jesper Borgstrup
     */
    public class ViewAspectRatioMeasurer {

        private double aspectRatio;

        public void setAspectRatio(double aspectRatio) {
            this.aspectRatio = aspectRatio;
        }

        public double getAspectRatio() {
            return this.aspectRatio;
        }

        /**
         * Measure with the aspect ratio given at construction.<br />使用创建时给出的纵横比进行测量
         * <br />
         * After measuring, get the width and height with the {@link #getMeasuredWidth()}
         * 测量后，使用{@link #getMeasuredWidth（）}获取宽度和高度
         * and {@link #getMeasuredHeight()} methods, respectively.
         *和{@link #getMeasuredHeight（）}方法
         * @param widthMeasureSpec  The width <tt>MeasureSpec</tt> passed in your <tt>View.onMeasure()</tt> method
         * @param widthMeasureSpec View.onMeasure（）</ tt>方法中传递的宽度<tt> MeasureSpec </ tt>
         * @param heightMeasureSpec The height <tt>MeasureSpec</tt> passed in your <tt>View.onMeasure()</tt> method
         *@param heightMeasureSpec View.onMeasure（）</ tt>方法中传递的高度<tt> MeasureSpec </ tt>
         */
        public void measure(int widthMeasureSpec, int heightMeasureSpec) {
            measure(widthMeasureSpec, heightMeasureSpec, this.aspectRatio);
        }

        /**
         * Measure with a specific aspect ratio<br /> 使用特定宽高比进行测量
         * <br />
         * After measuring, get the width and height with the {@link #getMeasuredWidth()}
         * 测量后，使用{@link #getMeasuredWidth（）}获取宽度和高度
         * and {@link #getMeasuredHeight()} methods, respectively.
         *和{@link #getMeasuredHeight（）}方法。
         * @param widthMeasureSpec  The width <tt>MeasureSpec</tt> passed in your <tt>View.onMeasure()</tt> method
         * @param widthMeasureSpec <tt> View.onMeasure（）</ tt>方法中传递的宽度<tt> MeasureSpec </ tt>
         * @param heightMeasureSpec The height <tt>MeasureSpec</tt> passed in your <tt>View.onMeasure()</tt> method
         *@param heightMeasureSpec <tt> View.onMeasure（）</ tt>方法中传递的高度<tt> MeasureSpec </ tt>
         * @param aspectRatio       The aspect ratio to calculate measurements in respect to
         *@param aspectRatio 用于计算测量值的纵横比
         */
        public void measure(int widthMeasureSpec, int heightMeasureSpec, double aspectRatio) {
            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSize = widthMode == MeasureSpec.UNSPECIFIED ? Integer.MAX_VALUE : MeasureSpec.getSize(widthMeasureSpec);
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSize = heightMode == MeasureSpec.UNSPECIFIED ? Integer.MAX_VALUE : MeasureSpec.getSize(heightMeasureSpec);

            if (heightMode == MeasureSpec.EXACTLY && widthMode == MeasureSpec.EXACTLY) {
                /*
                 * Possibility 1: Both width and height fixed 可能性1：宽度和高度都固定
                 */
                measuredWidth = widthSize;
                measuredHeight = heightSize;

            } else if (heightMode == MeasureSpec.EXACTLY) {
                /*
                 * Possibility 2: Width dynamic, height fixed 可能性2：宽度动态，高度固定
                 */
                measuredWidth = (int) Math.min(widthSize, heightSize * aspectRatio);
                measuredHeight = (int) (measuredWidth / aspectRatio);

            } else if (widthMode == MeasureSpec.EXACTLY) {
                /*
                 * Possibility 3: Width fixed, height dynamic 可能性3：宽度固定，高度动态
                 */
                measuredHeight = (int) Math.min(heightSize, widthSize / aspectRatio);
                measuredWidth = (int) (measuredHeight * aspectRatio);

            } else {
                /*
                 * Possibility 4: Both width and height dynamic 可能性4：宽度和高度都是动态的
                 */
                if (widthSize > heightSize * aspectRatio) {
                    measuredHeight = heightSize;
                    measuredWidth = (int) (measuredHeight * aspectRatio);
                } else {
                    measuredWidth = widthSize;
                    measuredHeight = (int) (measuredWidth / aspectRatio);
                }

            }
        }

        /**
         * Get the width measured in the latest call to <tt>measure()</tt>.
         * 获取最新调用<tt> measure（）</ tt>时测量的宽度。
         */
        public int getMeasuredWidth() {
            if (measuredWidth == null) {
                throw new IllegalStateException("You need to run measure() before trying to get measured dimensions");
            }
            return measuredWidth;
        }

        private Integer measuredHeight = null;

        /**
         * Get the height measured in the latest call to <tt>measure()</tt>.
         * 获取在最新调用<tt> measure（）</ tt>时测量的高度。
         */
        public int getMeasuredHeight() {
            if (measuredHeight == null) {
                throw new IllegalStateException("You need to run measure() before trying to get measured dimensions");
            }
            return measuredHeight;
        }
    }
}
