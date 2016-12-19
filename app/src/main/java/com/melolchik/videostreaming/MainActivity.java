package com.melolchik.videostreaming;


import android.content.Context;
import android.graphics.Camera;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.Button;

import net.majorkernelpanic.streaming.AppLogger;
import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.camera.CameraHelper;
import net.majorkernelpanic.streaming.camera.CameraHelperImpl1;
import net.majorkernelpanic.streaming.camera.CameraHelperImpl2;
import net.majorkernelpanic.streaming.camera.CameraImplBase;
import net.majorkernelpanic.streaming.rtsp.RtspClient;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;



public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener,Session.Callback,
        RtspClient.Callback{

    public final static String TAG = "VIDEO_STREAMING";
    protected @BindView(R.id.texture_view)
    TextureView mTextureView;

    protected @BindView(R.id.button)
    Button mButton;


    protected SurfaceTexture mSurfaceTexture;
    //protected CameraImplBase mHelperImpl;

    // Rtsp session
    private Session mSession;
    private static RtspClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mTextureView.setSurfaceTextureListener(this);
        //mHelperImpl = new CameraHelperImpl1(getApplicationContext());



    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        mSurfaceTexture = surfaceTexture;
        mSession = SessionBuilder.getInstance()
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_NONE)
                .setAudioQuality(new AudioQuality(8000, 16000))
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setSurfaceTexture(surfaceTexture)
                .setPreviewOrientation(0)
                .setCallback(this)
                .build();

        // Configures the RTSP client
        mClient = new RtspClient();
        mClient.setSession(mSession);
         mClient.setCallback(this);
        //mSurfaceView.setAspectRatioMode(SurfaceView.ASPECT_RATIO_PREVIEW);
        String ip, port, path;

        // We parse the URI written in the Editext
        Pattern uri = Pattern.compile("rtsp://(.+):(\\d+)/(.+)");
        Matcher m = uri.matcher("rtsp://46.28.207.136:554/live/test_android_stream22041983");
        m.find();
        ip = m.group(1);
        port = m.group(2);
        path = m.group(3);

        mClient.setCredentials("","");
        mClient.setServerAddress(ip, Integer.parseInt(port));
        mClient.setStreamPath("/" + path);

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {

        if(mClient.isStreaming()){
            mSession.stopPreview();
            mClient.stopStream();
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {


    }

    protected @OnClick(R.id.button) void clickOnButton(){
        if(mSurfaceTexture == null) return;

        if(mClient.isStreaming()){
            mSession.stopPreview();
            mClient.stopStream();
            mButton.setText(R.string.button_start);
        }else {
            mSession.startPreview();
            mButton.setText(R.string.button_stop);
            mClient.startStream();
        }

    }


    @Override
    public void onRtspUpdate(int message, Exception exception) {
        log("onRtspUpdate " + message + " " + exception);
    }

    @Override
    public void onBitrateUpdate(long bitrate) {
      //  log("onBitrateUpdate");
    }

    @Override
    public void onSessionError(int reason, int streamType, Exception e) {
        log("onSessionStopped type = "+ streamType + " " + e.getMessage());
    }

    @Override
    public void onPreviewStarted() {
        log("onPreviewStarted");
    }

    @Override
    public void onSessionConfigured() {
        log("onSessionConfigured");
    }

    @Override
    public void onSessionStarted() {
        log("onSessionStarted");
    }

    @Override
    public void onSessionStopped() {
        log("onSessionStopped");
    }

    protected void log(String message) {
        AppLogger.log(getClass().getSimpleName() + " " + message);
    }
}
