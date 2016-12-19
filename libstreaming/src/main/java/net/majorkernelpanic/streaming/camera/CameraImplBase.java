package net.majorkernelpanic.streaming.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import net.majorkernelpanic.streaming.AppLogger;
import net.majorkernelpanic.streaming.video.VideoQuality;

/**
 * Created by Olga Melekhina on 18.12.2016.
 */

public abstract class CameraImplBase {

    protected final Context mContext;
    protected Handler mMainHandler;
    private HandlerThread mBackgroundThread;
    protected Handler mBackgroundHandler;
    protected SurfaceTexture mSurfaceTexture;
    private boolean mIsStarted = false;
    protected VideoQuality mVideoQuality;

    protected OnPictureTakeListener mOnPictureTakeListener;

    public interface OnPictureTakeListener{
        void onPictureTaken(byte[] imageBytes);
    }


    public CameraImplBase(Context context, VideoQuality videoQuality){
        mContext = context;
        mVideoQuality = videoQuality;
        mMainHandler = new Handler(Looper.getMainLooper());

        mBackgroundThread = new HandlerThread("CameraThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

    }

    public Context getContext() {
        return mContext;
    }

    public boolean isStarted() {
        return mIsStarted;
    }

    public void setStarted(boolean started) {
        mIsStarted = started;
    }

    public abstract void open(SurfaceTexture surfaceTexture);
    public abstract void close();

    public void setOnPictureTakeListener(OnPictureTakeListener onPictureTakeListener) {
        mOnPictureTakeListener = onPictureTakeListener;
    }

    protected void log(String message) {
        AppLogger.log(getClass().getSimpleName() + " " + message);
    }
}

