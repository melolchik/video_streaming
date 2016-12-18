package net.majorkernelpanic.streaming.camera;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

/**
 * Created by Olga Melekhina on 18.12.2016.
 */

public class CameraHelper {
    static CameraHelper sInstance = null;
    final CameraManager mCameraManager;
    private final Context mContext;

    private CameraHelper(Context context) {
        mContext = context;
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
    }

    public static void createInstance(Context context){
        sInstance = new CameraHelper(context);
    }
    public static CameraHelper getInstance() {
        return sInstance;
    }

    public CameraManager getCameraManager() {
        return mCameraManager;
    }

    public Context getContext() {
        return mContext;
    }

}
