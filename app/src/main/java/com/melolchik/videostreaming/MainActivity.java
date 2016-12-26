package com.melolchik.videostreaming;


import android.graphics.SurfaceTexture;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.widget.Button;
import android.widget.Toast;

import net.majorkernelpanic.streaming.AppLogger;
import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspClient;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;



public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener,Session.Callback,
        RtspClient.Callback, PermissionManager.PermissionResultListener {

    public final static String TAG = "VIDEO_STREAMING";
    protected @BindView(R.id.texture_view)
    TextureView mTextureView;

    protected @BindView(R.id.button)
    Button mButton;


    protected SurfaceTexture mSurfaceTexture;

    protected PermissionManager mPermissionManager = new PermissionManager();

    // Rtsp session
    private Session mSession;
    private static RtspClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mTextureView.setSurfaceTextureListener(this);
        mButton.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mButton.setEnabled(mPermissionManager.checkPermissionGrantedByCode(this,this,PermissionManager.REQUEST_CODE_ALL));
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        mSurfaceTexture = surfaceTexture;
        mSession = SessionBuilder.getInstance()
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_NONE)
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setSurfaceTexture(surfaceTexture)
                .setPreviewOrientation(0)
                .setCallback(this)
                .build();

        // Configures the RTSP client
        mClient = new RtspClient();
        mClient.setSession(mSession);
         mClient.setCallback(this);
        String ip, port, path;

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mPermissionManager != null) {
            mPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    /**
     * From  PermissionResultListener
     * Here can be only part of permissions granted, if permissionsNotGranted list is not empty
     * For get all requested permission list use  PermissionManager.getPermissionListByCode()
     * If two lists are equal ==> permission all denied
     *
     * @param requestCode               the request code
     * @param permissionsNotGranted the permissions not granted
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> permissionsNotGranted) {

        if(permissionsNotGranted.isEmpty()){
            mButton.setEnabled(true);
        }else {
            mButton.setEnabled(false);
            showErrorToast("Please, check all permissions in Settings");
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode) {

        mButton.setEnabled(false);
        showErrorToast("Please, check all permissions in Settings");
    }

    protected void showErrorToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.show();
    }

}
