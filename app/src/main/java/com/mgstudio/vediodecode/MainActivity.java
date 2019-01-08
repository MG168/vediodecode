package com.mgstudio.vediodecode;

import android.media.MediaCodec;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

//    video output dimension
    static final int OUTPUT_WIDTH = 640;
    static final int OUTPUT_HEIGHT = 480;

//    添加编码解码方法
    VideoEncoder mEncoder;
    VideoDecoder mDecoder;

    SurfaceView mEncoderSurfaceView;
    android.view.SurfaceView mDecoderSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEncoderSurfaceView = (SurfaceView) findViewById(R.id.encoder_surface);
        mEncoderSurfaceView.getHolder().addCallback(mEncoderCallback);

        mDecoderSurfaceView = (android.view.SurfaceView) findViewById(R.id.decoder_surface);
        mDecoderSurfaceView.getHolder().addCallback(mDecoderCallback);

        mEncoder = new MyEncoder();
        mDecoder = new VideoDecoder();
    }

    private SurfaceHolder.Callback mEncoderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
//            sruface is fully initialized on the activity
            mEncoderSurfaceView.startGLThread();
            mEncoder.start();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };

    private SurfaceHolder.Callback mDecoderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
//            Surface is fully initialized on the activity
//            mDecoderSurfaceView.startGLThread();
            mDecoder.start();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };


    class MyEncoder extends VideoDecoder {

        byte[] mBuffer = new byte[];

        public MyEncoder() {
            super(mEncoderSurfaceView, OUTPUT_WIDTH, OUTPUT_HEIGHT);
        }

        @Override
        protected void onEncodedSample(MediaCodec.BufferInfo info, ByteBuffer data) {
            // Here we could have just used ByteBuffer, but in real life case we might need to
            //这里我们可以使用ByteBuffer，但在现实生活中我们可能需要
            // send sample over network, etc. This requires byte[]//通过网络发送样本等。这需要byte []
            if (mBuffer.length < info.size) {
                mBuffer = new byte[info.size];
            }
            data.position(info.offset);
            data.limit(info.offset + info.size);
            data.get(mBuffer, 0, info.size);

            Log.d("ENCODER_FLAG", String.valueOf(info.flags));

            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                // this is the first and only config sample, which contains information about codec
                //这是第一个也是唯一的配置示例，其中包含有关编解码器的信息
                // like H.264, that let's configure the decoder
                //像H.264，让我们配置解码器

                VideoPacket.StreamSettings streamSettings = VideoPacket.getStreamSettings(mBuffer);

                mDecoder.configure(mDecoderSurfaceView.getHolder().getSurface(),
                        OUTPUT_WIDTH,
                        OUTPUT_HEIGHT,
                        streamSettings.sps, streamSettings.pps);
            } else {
                // pass byte[] to decoder's queue to render asap 将byte []传递给解码器的队列以尽快渲染
                mDecoder.decodeSample(mBuffer,
                        0,
                        info.size,
                        info.presentationTimeUs,
                        info.flags);
            }
        }
    }


}
