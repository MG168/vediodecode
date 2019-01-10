package com.mgstudio.vediodecode.activities;
// 录音机
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.mgstudio.vediodecode.R;
import com.mgstudio.vediodecode.sender.Sender;
import com.mgstudio.vediodecode.surface.SurfaceView;

import java.util.concurrent.atomic.AtomicBoolean;

public class RecorderActivity extends AppCompatActivity {

    // video output dimension
    //视频输出维度
    static final int OUTPUT_WIDTH = 640;
    static final int OUTPUT_HEIGHT = 480;

    Sender mSender;
    AtomicBoolean isSurfaceCreated = new AtomicBoolean(false);

    SurfaceView mEncoderSurfaceView;
    Button mStartButton;
    EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);

        mEncoderSurfaceView = (SurfaceView) findViewById(R.id.encoder_surface);
        mEncoderSurfaceView.getHolder().addCallback(mEncoderCallback);

        mEditText = (EditText) findViewById(R.id.edit_text);

        mStartButton = (Button) findViewById(R.id.start_button);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isSurfaceCreated.get()) {
                    mSender = new Sender(mEditText.getText().toString(), 5006, mEncoderSurfaceView, OUTPUT_WIDTH, OUTPUT_HEIGHT);
                    mSender.start();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSender != null) {
            mSender.stop();
        }
    }

    private SurfaceHolder.Callback mEncoderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            // surface is fully initialized on the activity
            //surface已在活动上完全初始化
            mEncoderSurfaceView.startGLThread();
            isSurfaceCreated.set(true);
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            isSurfaceCreated.set(false);
        }
    };
}
