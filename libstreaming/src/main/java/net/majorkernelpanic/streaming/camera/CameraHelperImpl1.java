package net.majorkernelpanic.streaming.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import net.majorkernelpanic.streaming.exceptions.CameraInUseException;
import net.majorkernelpanic.streaming.exceptions.InvalidSurfaceException;

import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * Created by Olga Melekhina on 18.12.2016.
 */

public class CameraHelperImpl1 extends CameraImplBase {

    protected int mCameraId = 0;
    protected Camera mCamera;

    public CameraHelperImpl1(Context context) {
        super(context);
        mMainHandler = new Handler(Looper.getMainLooper());

        new HandlerThread("CameraHelperImpl2") {
            @Override
            protected void onLooperPrepared() {
                mHandler = new Handler();
            }
        }.start();

        setCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
    }


    /**
     * Sets the camera that will be used to capture video.
     * You can call this method at any time and changes will take effect next time you start the stream.
     *
     * @param camera Can be either CameraInfo.CAMERA_FACING_BACK or CameraInfo.CAMERA_FACING_FRONT
     */
    public void setCamera(int camera) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == camera) {
                mCameraId = i;
                break;
            }
        }
    }

    protected int getCamera() {
        return mCameraId;
    }

    @Override
    public void open(final SurfaceTexture surfaceTexture) {

        mSurfaceTexture = surfaceTexture;
        if (mSurfaceTexture == null) return;
        if(isStarted()) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCamera = Camera.open();
                    setStarted(true);
                    /*
                     Camera.Parameters parameters = mCamera.getParameters();
                if (parameters.getFlashMode() != null) {
                    parameters.setFlashMode(mFlashEnabled ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
                }
                parameters.setRecordingHint(true);
                mCamera.setParameters(parameters);*/
                mCamera.setDisplayOrientation(90);

                    mCamera.setPreviewTexture(surfaceTexture);
                    mCamera.startPreview();
                } catch (IOException e) {
                    Log.d("CAMERA", e.getMessage());
                }

            }
        });
    }

    @Override
    public void close() {
        if(!isStarted()) return;
        setStarted(false);
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }
    }

}
