package net.majorkernelpanic.streaming.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import net.majorkernelpanic.streaming.video.VideoQuality;

import java.io.IOException;

/**
 * Created by Olga Melekhina on 18.12.2016.
 */

//@TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
public class CameraHelperImpl1 extends CameraImplBase {

    protected int mCameraId = 0;
    protected Camera mCamera;

    public CameraHelperImpl1(Context context, VideoQuality videoQuality) {
        super(context, videoQuality);
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

    protected Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            log("onPreviewFrame " + bytes);
            if (mOnPictureTakeListener != null && bytes != null) {
                mOnPictureTakeListener.onPictureTaken(bytes);
            }
        }
    };

    @Override
    public void open(final SurfaceTexture surfaceTexture) {
        if (mSurfaceTexture == null) return;
        if (isStarted()) return;
        super.open(surfaceTexture);
        mSurfaceTexture = surfaceTexture;

        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCamera = Camera.open(mCameraId);
                    setStarted(true);

                   /* Camera.Parameters parameters = mCamera.getParameters();
                    mVideoQuality = VideoQuality.determineClosestSupportedResolution(parameters, mVideoQuality);
                    int[] max = VideoQuality.determineMaximumSupportedFramerate(parameters);

                    double ratio = (double) mVideoQuality.resX / (double) mVideoQuality.resY;

                    parameters.setPreviewSize(mVideoQuality.resX, mVideoQuality.resY);
                    parameters.setPreviewFpsRange(max[0], max[1]);*/
                    mCamera.setDisplayOrientation(90);

                    /*try {
                        mCamera.setParameters(parameters);
                        mCamera.setDisplayOrientation(90);

                    } catch (RuntimeException e) {
                        throw e;
                    }*/

                    mCamera.setPreviewTexture(surfaceTexture);


                    /*for (int i = 0; i < 10; i++)
                        mCamera.addCallbackBuffer(new byte[3 * mVideoQuality.resX * mVideoQuality.resY / 2]);
                    mCamera.setPreviewCallbackWithBuffer(mPreviewCallback);*/
                    mCamera.setPreviewCallback(mPreviewCallback);
                    mCamera.startPreview();
                } catch (IOException e) {
                    log(e.getMessage());
                }

            }
        });
    }

    @Override
    public void close() {
        if (!isStarted()) return;

        setStarted(false);
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }
        super.close();
    }

}
