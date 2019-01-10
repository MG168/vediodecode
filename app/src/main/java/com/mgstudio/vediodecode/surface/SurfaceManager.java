package com.mgstudio.vediodecode.surface;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.view.Surface;

public class SurfaceManager {

    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    private EGLContext mEGLContext = null;
    private EGLContext mEGLSharedContext = null;
    private EGLSurface mEGLSurface = null;
    private EGLDisplay mEGLDisplay = null;

    private Surface mSurface;

    /**
     * Creates an EGL context and an EGL surface.创建EGL上下文和EGL表面。
     */
    public SurfaceManager(Surface surface, SurfaceManager manager) {
        mSurface = surface;
        mEGLSharedContext = manager.mEGLContext;
        eglSetup();
    }

    /**
     * Creates an EGL context and an EGL surface.创建EGL上下文和EGL表面。
     */
    public SurfaceManager(Surface surface) {
        mSurface = surface;
        eglSetup();
    }

    public void makeCurrent() {
        if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext))
            throw new RuntimeException("eglMakeCurrent failed");
    }

    public void swapBuffer() {
        EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface);
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     * 将演示时间戳发送到EGL。 时间以纳秒表示。
     */
    public void setPresentationTime(long nsecs) {
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs);
        checkEglError("eglPresentationTimeANDROID");
    }

    /**
     * Prepares EGL.  We want a GLES 2.0 context and a surface that supports recording.
     * 准备EGL。 我们需要GLES 2.0上下文和支持录制的表面。
     */
    private void eglSetup() {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            throw new RuntimeException("unable to initialize EGL14");
        }

        // Configure EGL for recording and OpenGL ES 2.0.//配置EGL进行录制和OpenGL ES 2.0。
        int[] attribList;
        if (mEGLSharedContext == null) {
            attribList = new int[] {
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_NONE
            };
        } else {
            attribList = new int[] {
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL_RECORDABLE_ANDROID, 1,
                    EGL14.EGL_NONE
            };
        }
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
                numConfigs, 0);
        checkEglError("eglCreateContext RGB888+recordable ES2");

        // Configure context for OpenGL ES 2.0.//配置OpenGL ES 2.0的上下文。
        int[] attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };

        if (mEGLSharedContext == null) {
            mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT, attrib_list, 0);
        } else {
            mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], mEGLSharedContext, attrib_list, 0);
        }
        checkEglError("eglCreateContext");

        // Create a window surface, and attach it to the Surface we received.
        //创建一个窗口曲面，并将其附加到我们收到的曲面上。
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };
        mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mSurface,
                surfaceAttribs, 0);
        checkEglError("eglCreateWindowSurface");

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);

    }

    /**
     * Discards all resources held by this class, notably the EGL context.  Also releases the
     * *丢弃此类持有的所有资源，尤其是EGL上下文。 同时发布
     * Surface that was passed to our constructor.
     *传递给我们的构造函数的表面。
     */
    public void release() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEGLDisplay);
        }
        mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        mEGLContext = EGL14.EGL_NO_CONTEXT;
        mEGLSurface = EGL14.EGL_NO_SURFACE;
        mSurface.release();
    }

    /**
     * Checks for EGL errors. Throws an exception if one is found.检查EGL错误。 如果找到，则引发异常。
     *
     */
    private void checkEglError(String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }
}
