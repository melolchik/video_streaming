package com.melolchik.videostreaming;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;

import net.majorkernelpanic.streaming.AppLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Olga Melekhina on 15.07.2016.
 */
public class PermissionManager {

    /**
     * The constant REQUEST_CODE_ALL.
     */
    public static final int REQUEST_CODE_ALL = 500;
    /**
     * The constant REQUEST_CODE_ACCESS_LOCATION.
     */
    public static final int REQUEST_CODE_ACCESS_LOCATION = 501;
    /**
     * The constant REQUEST_CODE_ACCESS_STORAGE.
     */
    public static final int REQUEST_CODE_ACCESS_STORAGE = 502;
    /**
     * The constant REQUEST_CODE_ACCESS_PHONE_STATE.
     */
    public static final int REQUEST_CODE_ACCESS_PHONE_STATE = 503;
    /**
     * The constant REQUEST_CODE_ACCESS_BLUETOOTH.
     */
    public static final int REQUEST_CODE_ACCESS_BLUETOOTH = 504;

    /**
     * The interface Permission result listener.
     */
    public interface PermissionResultListener{
        /**
         * On permissions granted.
         *
         * @param requestCode           the request code
         * @param permissionsNotGranted the permissions not granted
         */
        void onPermissionsGranted(int requestCode, List<String> permissionsNotGranted);

        /**
         * On permissions denied.
         *
         * @param requestCode the request code
         */
        void onPermissionsDenied(int requestCode);
    }

    /**
     * The M activity.
     */
    protected Activity mActivity;
    /**
     * The M result listener.
     */
    protected PermissionResultListener mResultListener;

    /**
     * Instantiates a new Permission manager.
     */
    public PermissionManager() {
    }

    /**
     * Get permission list by code string [ ].
     *
     * @param requestCode the request code
     * @return the string [ ]
     */
    public static String[] getPermissionListByCode(int requestCode){
        String permissionList[] = new String[]{};
        switch (requestCode){
            case REQUEST_CODE_ACCESS_LOCATION:
                permissionList = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION};
                break;
            case REQUEST_CODE_ACCESS_STORAGE:
                permissionList = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE};
                break;
            case REQUEST_CODE_ACCESS_PHONE_STATE:
                permissionList = new String[]{Manifest.permission.READ_PHONE_STATE};
                break;
            case REQUEST_CODE_ACCESS_BLUETOOTH:
                permissionList = new String[]{Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN};
                break;
            case REQUEST_CODE_ALL:
                permissionList = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA
                };
                break;
        }
        return permissionList;
    }

    /**
     * Check permission granted by code boolean.
     *
     * @param activity    the activity
     * @param listener    the listener
     * @param requestCode the request code
     * @return the boolean
     */
    public boolean checkPermissionGrantedByCode(Activity activity,PermissionResultListener listener,int requestCode)
    {
        mActivity = activity;
        mResultListener = listener;
        return checkPermissionGranted(getPermissionListByCode(requestCode),requestCode);
    }


    /**
     * Is location permission granted boolean.
     *
     * @param activity the activity
     * @return the boolean
     */
    public boolean isLocationPermissionGranted(Activity activity) {
        mActivity = activity;
        String permissionList[] = getPermissionListByCode(REQUEST_CODE_ACCESS_LOCATION);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        boolean checkPermissionGranted = true;
        for (String permission : permissionList) {
            if (ActivityCompat.checkSelfPermission(mActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                checkPermissionGranted = false;
                break;
            }
        }

        return checkPermissionGranted;
    }

    /**
     * Is storage permission granted boolean.
     *
     * @param activity the activity
     * @return the boolean
     */
    public boolean isStoragePermissionGranted(Activity activity) {
        mActivity = activity;
        String permissionList[] = getPermissionListByCode(REQUEST_CODE_ACCESS_STORAGE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        boolean checkPermissionGranted = true;
        for (String permission : permissionList) {
            if (ActivityCompat.checkSelfPermission(mActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                checkPermissionGranted = false;
                break;
            }
        }

        return checkPermissionGranted;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean checkPermissionGranted(String permissionList[], final int codeRequest) {

//        log("checkPermissionGranted ");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        if (permissionList == null || permissionList.length == 0) return true;
        boolean checkPermissionGranted = true;
        //check all permissions in list
        for (String permission : permissionList) {
            if (ActivityCompat.checkSelfPermission(mActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                checkPermissionGranted = false;
                break;
            }
        }
        //all permissions granted !!!
        if (checkPermissionGranted) return true;

        /*
        To help find situations where the user might need an explanation,
        Android provides a utility method, shouldShowRequestPermissionRationale().
        This method returns true if the app has requested this permission previously
        and the user denied the request.

        Note: If the user turned down the permission request in the past and chose the
        Don't ask again option in the permission request system dialog, this method returns false.
        The method also returns false if a device policy prohibits the app from having that permission.
         */
        boolean shouldShowRequestPermissionRationale = true;
        for (String permission : permissionList) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permission)) {
                shouldShowRequestPermissionRationale = false;
                break;
            }
        }
        // Should we show an explanation?
        if (shouldShowRequestPermissionRationale) {
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            //onPermissionsGranted(codeRequest, false);

            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onPermissionsDenied(codeRequest);
                }
            });

        } else {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(mActivity,permissionList, codeRequest);
            // PERMISSIONS_REQUEST_CODE_.. is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }
        return false;
    }

    /**
     * On request permissions result.
     * Requested from activity
     *
     * @param requestCode  the request code
     * @param permissions  the permissions
     * @param grantResults the grant results
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        log("onRequestPermissionsResult requestCode = " + requestCode);
        StringBuilder builder = new StringBuilder();
        for (String perm : permissions) {
            builder.append(perm).append(", ");
        }
        log("onRequestPermissionsResult permissions = " + builder.toString());
        builder = new StringBuilder();
        for (int perm : grantResults) {
            builder.append(perm).append(", ");
        }
        log("onRequestPermissionsResult grantResults = " + builder.toString());
        List<String> notGrantedList = getPermissionsNotGranted(permissions, grantResults);
        onPermissionsGranted(requestCode,notGrantedList);

    }

    private List<String> getPermissionsNotGranted(String[] permissions, int[] grantResults) {
        List<String> resultArray = new ArrayList<>();
        if (grantResults.length <= 0) return resultArray;
        int permisssonsCount = permissions.length;
        for (int i = 0; i < permisssonsCount; i++) {
            String perm = permissions[i];
            int result = grantResults[i];
            if (result != PackageManager.PERMISSION_GRANTED) {
                resultArray.add(perm);
            }
        }
        return resultArray;
    }


    /**
     * On permissions granted.
     *
     * @param requestCode               the request code
     * @param permissionsNotGrantedList the permissions not granted list
     */
    protected void onPermissionsGranted(int requestCode,List<String> permissionsNotGrantedList) {

        if(mResultListener == null) return;
       /* String[] array = new String[0];
        if(permissionsNotGrantedList != null && !permissionsNotGrantedList.isEmpty()) {
            int size = permissionsNotGrantedList.size();
            array = new String[size];
            permissionsNotGrantedList.toArray(array);
        }*/
        mResultListener.onPermissionsGranted(requestCode,permissionsNotGrantedList);

    }

    /**
     * On permissions denied.
     *
     * @param requestCode the request code
     */
    protected void onPermissionsDenied(int requestCode){
        if(mResultListener == null) return;
        mResultListener.onPermissionsDenied(requestCode);
    }

    /**
     * Log.
     *
     * @param message the message
     */
    protected void log(String message) {
        AppLogger.log(this.getClass().getSimpleName() + " " + message);
    }
}
