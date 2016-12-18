package net.majorkernelpanic.streaming.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.Surface;

import net.majorkernelpanic.streaming.AppLogger;

/**
 * Created by Olga Melekhina on 18.12.2016.
 */

public abstract class CameraImplBase {

    protected final Context mContext;
    protected Handler mMainHandler;
    protected Handler mHandler;
    protected SurfaceTexture mSurfaceTexture;
    private boolean mIsStarted = false;

    public CameraImplBase(Context context){
        mContext = context;
        mMainHandler = new Handler(Looper.getMainLooper());

        new HandlerThread("CameraHelperImpl2"){
            @Override
            protected void onLooperPrepared() {
                mHandler = new Handler();
            }
        }.start();
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


    protected void log(String message) {
        AppLogger.log(getClass().getSimpleName() + " " + message);
    }
}

