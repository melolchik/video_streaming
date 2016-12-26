package net.majorkernelpanic.streaming.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;
import android.view.Surface;

import net.majorkernelpanic.streaming.hw.EncoderDebugger;
import net.majorkernelpanic.streaming.hw.NV21Convertor;
import net.majorkernelpanic.streaming.video.VideoQuality;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Olga Melekhina on 18.12.2016.
 */

public class CameraHelperImpl2 extends CameraImplBase{

    private static final int IMAGE_WIDTH  = 360;
    private static final int IMAGE_HEIGHT = 240;
    private static final int MAX_IMAGES   = 2;


    protected final CameraManager mCameraManager;
    protected Surface mSurface;
    private CameraDevice mCameraDevice;
    protected CameraCaptureSession mCameraCaptureSession;
    private ImageReader mImageReader;



    public CameraHelperImpl2(Context context, VideoQuality videoQuality) {
        super(context,videoQuality);
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

        int width = IMAGE_WIDTH;
        int height = IMAGE_HEIGHT;
        if(videoQuality != null){
            width = videoQuality.resX;
            height = videoQuality.resY;
        }
        mImageReader = ImageReader.newInstance(width, height,
                ImageFormat.YV12, MAX_IMAGES);
        mImageReader.setOnImageAvailableListener(mImageAvailableListener, mBackgroundHandler);

    }

    public ImageReader.OnImageAvailableListener mImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
                //log("onImageAvailable");
                Image image = imageReader.acquireLatestImage();
                if(image == null) return;
                ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                final byte[] imageBytes = new byte[imageBuf.remaining()];
                imageBuf.get(imageBytes);
                image.close();
            if(mOnPictureTakeListener != null){
                mBackgroundHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mOnPictureTakeListener.onPictureTaken(imageBytes);
                    }
                });

            }


        }
    };

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
                session.setRepeatingRequest(createCaptureRequest(), null, mBackgroundHandler);
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
            builder.addTarget(mImageReader.getSurface());

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
                mCameraDevice.createCaptureSession(Arrays.asList(mSurface,mImageReader.getSurface()), mSessionStateCallback, mBackgroundHandler);
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
        if(surfaceTexture == null) return;
        if (isStarted()) return;
        super.open(surfaceTexture);
        mSurfaceTexture = surfaceTexture;
        mSurface = new Surface(surfaceTexture);
        if (mSurface == null) return;
        try {
            mCameraManager.openCamera(getCamera(), mCameraStateCallback, mBackgroundHandler);
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
        super.close();
        try {
            if (mCameraCaptureSession != null) {
                mCameraCaptureSession.abortCaptures();
                mCameraCaptureSession.close();
                mCameraCaptureSession = null;
            }
            if(mCameraDevice != null){
                mCameraDevice.close();
                mCameraDevice = null;
            }
            setStarted(false);

        } catch (CameraAccessException e) {
            log(e.getMessage());
        } catch (SecurityException se) {
            log(se.getMessage());
        }
        super.close();
    }


}
