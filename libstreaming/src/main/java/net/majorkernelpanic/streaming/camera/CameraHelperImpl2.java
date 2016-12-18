package net.majorkernelpanic.streaming.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import net.majorkernelpanic.streaming.AppLogger;

import java.util.Arrays;

/**
 * Created by Olga Melekhina on 18.12.2016.
 */

public class CameraHelperImpl2 extends CameraImplBase{

    protected final CameraManager mCameraManager;
    protected Surface mSurface;
    private CameraDevice mCameraDevice;
    protected CameraCaptureSession mCameraCaptureSession;



    public CameraHelperImpl2(Context context) {
        super(context);
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

    }

    public String getCamera() {
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation != null && cOrientation == CameraCharacteristics.LENS_FACING_BACK) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            log(e.getMessage());
        }
        return null;
    }

    private CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            mCameraCaptureSession = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), null, mHandler);
            } catch (CameraAccessException e) {
                log(e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
        }
    };

    private CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(mSurface);

            return builder.build();
        } catch (CameraAccessException e) {
            log(e.getMessage());
            return null;
        }
    }

    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            try {
                mCameraDevice.createCaptureSession(Arrays.asList(mSurface), mSessionStateCallback, mHandler);
            } catch (CameraAccessException e) {
                log(e.getMessage());
            }
        }

        @Override
        public void onClosed(CameraDevice camera) {
            super.onClosed(camera);
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
        }

        @Override
        public void onError(CameraDevice camera, int error) {
        }
    };

    public void open(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
        mSurface = new Surface(surfaceTexture);
        if (mSurface == null) return;
        if (isStarted()) return;
        try {
            mCameraManager.openCamera(getCamera(), mCameraStateCallback, mHandler);
            setStarted(true);
        } catch (CameraAccessException e)

        {
            log(e.getMessage());
        } catch (SecurityException se) {
            log(se.getMessage());
        }
    }

    public void close() {
        if (!isStarted()) return;
        try {
            if (mCameraCaptureSession != null) {
                mCameraCaptureSession.abortCaptures();
                mCameraCaptureSession.close();
                mCameraCaptureSession = null;
            }
            setStarted(false);

        } catch (CameraAccessException e) {
            log(e.getMessage());
        } catch (SecurityException se) {
            log(se.getMessage());
        }

    }



}
