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

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.rtsp.RtspClient;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;



public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener{

    public final static String TAG = "VIDEO_STREAMING";
    protected @BindView(R.id.texture_view)
    TextureView mTextureView;

    protected @BindView(R.id.button)
    Button mButton;
    File mOutputFile = null;

    protected Surface mSurface;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    protected MediaRecorder mMediaRecorder;
    protected CameraCaptureSession mCameraCaptureSession;

    private Session mSession;
    private RtspClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mTextureView.setSurfaceTextureListener(this);

        mSession = SessionBuilder.getInstance()
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_AAC)
                .setAudioQuality(new AudioQuality(8000,16000))
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
               // .setSurfaceView(mSurfaceView)
                .setPreviewOrientation(0)
               // .setCallback(this)
                .build();

        // Configures the RTSP client
        mClient = new RtspClient();
        mClient.setSession(mSession);
      //  mClient.setCallback(this);

    }

    public String getCamera(){
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation!= null && cOrientation == CameraCharacteristics.LENS_FACING_BACK) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
        return null;
    }

    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            try {
                mCameraDevice.createCaptureSession(Arrays.asList(mSurface), sessionStateCallback, null);
                mButton.setText(R.string.button_stop);
            } catch (CameraAccessException e){
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onClosed(CameraDevice camera) {
            super.onClosed(camera);
            mButton.setText(R.string.button_start);
        }

        @Override
        public void onDisconnected(CameraDevice camera) {}

        @Override
        public void onError(CameraDevice camera, int error) {}
    };

    class MyCaptureCallback extends CameraCaptureSession.CaptureCallback {

        @Override
        public void onCaptureCompleted(CameraCaptureSession camera, CaptureRequest request,
                                       TotalCaptureResult result) {
            // TODO Auto-generated method stub
            Log.v(TAG, "in onCaptureComplete");

        }

        @Override
        public void onCaptureFailed(CameraCaptureSession camera, CaptureRequest request,
                                    CaptureFailure failure) {
            // TODO Auto-generated method stub
            Log.v(TAG, "onCaptureFailed is being called");
        }

    }

    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            mCameraCaptureSession = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), new MyCaptureCallback(), null);
            } catch (CameraAccessException e){
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {}
    };

    private CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(mSurface);

            return builder.build();
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }


    protected SurfaceTexture.OnFrameAvailableListener mOnFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {


        }
    };


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {

        mSurface = new Surface(surfaceTexture);
        surfaceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        //mMediaRecorder.release();
        try {
            if(mCameraCaptureSession != null) {
                mCameraCaptureSession.abortCaptures();
                mCameraCaptureSession.close();
            }
        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    boolean isStarted = false;

    protected @OnClick(R.id.button) void clickOnButton(){
        try{
            if(!isStarted) {
                mCameraManager.openCamera(getCamera(), mCameraStateCallback, null);
                isStarted = true;
            }else {
                if(mCameraCaptureSession != null) {
                    mCameraCaptureSession.abortCaptures();
                    mCameraCaptureSession.close();
                    mCameraCaptureSession = null;
                }
                isStarted = false;
                mButton.setText(R.string.button_start);
            }
        }catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }
        catch (SecurityException se){
            Log.e(TAG, se.getMessage());
        }
       /* try {
            if (isStarted) {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                mMediaRecorder.release();
                isStarted = false;
            } else {
                mMediaRecorder.prepare();
                mMediaRecorder.start();
                isStarted = true;
            }
        }catch (IOException ex){
            Log.d(TAG, ex.getMessage());
        }*/

    }

    public  static File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return  null;
        }

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraSample");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()) {
                Log.d("CameraSample", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");

        return mediaFile;
    }


}
