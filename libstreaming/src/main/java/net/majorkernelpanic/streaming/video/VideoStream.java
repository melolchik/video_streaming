/*
 * Copyright (C) 2011-2015 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of libstreaming (https://github.com/fyhertz/libstreaming)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package net.majorkernelpanic.streaming.video;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import net.majorkernelpanic.streaming.MediaStream;
import net.majorkernelpanic.streaming.Stream;
import net.majorkernelpanic.streaming.camera.CameraHelperImpl1;
import net.majorkernelpanic.streaming.camera.CameraHelperImpl2;
import net.majorkernelpanic.streaming.camera.CameraImplBase;
import net.majorkernelpanic.streaming.exceptions.CameraInUseException;
import net.majorkernelpanic.streaming.exceptions.ConfNotSupportedException;
import net.majorkernelpanic.streaming.exceptions.InvalidSurfaceException;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.hw.EncoderDebugger;
import net.majorkernelpanic.streaming.hw.NV21Convertor;
import net.majorkernelpanic.streaming.rtp.MediaCodecInputStream;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;

/**
 * Don't use this class directly.
 */
public abstract class VideoStream extends MediaStream {

    /**
     * The constant TAG.
     */
    protected final static String TAG = "VideoStream";

    /**
     * The M requested quality.
     */
    protected VideoQuality mRequestedQuality = VideoQuality.DEFAULT_VIDEO_QUALITY.clone();
    /**
     * The M quality.
     */
    protected VideoQuality mQuality = mRequestedQuality.clone();
    /**
     * The M surface texture.
     */
    protected SurfaceTexture mSurfaceTexture = null;
    /**
     * The M surface.
     */
    protected Surface mSurface = null;
    /**
     * The M settings.
     */
    protected SharedPreferences mSettings = null;
    /**
     * The M video encoder.
     */
    protected int mVideoEncoder;
    /**
     * The M requested orientation.
     */
    protected int mRequestedOrientation = 0, /**
     * The M orientation.
     */
    mOrientation = 0;


    /**
     * The M flash enabled.
     */
    protected boolean mFlashEnabled = false;
    /**
     * The M surface ready.
     */
    protected boolean mSurfaceReady = false;
    /**
     * The M unlocked.
     */
    protected boolean mUnlocked = false;
    /**
     * The M preview started.
     */
    protected boolean mPreviewStarted = false;
    /**
     * The M updated.
     */
    protected boolean mUpdated = false;

    /**
     * The M mime type.
     */
    protected String mMimeType;
    /**
     * The M encoder name.
     */
    protected String mEncoderName;
    /**
     * The M encoder color format.
     */
    protected int mEncoderColorFormat;
    /**
     * The M camera image format.
     */
    protected int mCameraImageFormat;
    /**
     * The M max fps.
     */
    protected int mMaxFps = 0;

    /**
     * The M camera helper.
     */
    protected CameraImplBase mCameraHelper;

    /**
     * Instantiates a new Video stream.
     *
     * @param context the context
     */
    public VideoStream(Context context) {
        super();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mCameraHelper = new CameraHelperImpl1(context, mQuality);
        }else {
            mCameraHelper = new CameraHelperImpl1(context, mQuality);
        }
    }

    /**
     * Sets surface texture.
     *
     * @param surfaceTexture the surface texture
     */
    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
        if (mSurfaceTexture == null) return;
        mSurface = new Surface(surfaceTexture);
        mSurfaceReady = true;
    }


    /**
     * Sets the orientation of the preview.
     *
     * @param orientation The orientation of the preview
     */
    public void setPreviewOrientation(int orientation) {
        mRequestedOrientation = orientation;
        mUpdated = false;
    }

    /**
     * Sets the configuration of the stream. You can call this method at any time
     * and changes will take effect next time you call {@link #configure()}.
     *
     * @param videoQuality Quality of the stream
     */
    public void setVideoQuality(VideoQuality videoQuality) {
        if (!mRequestedQuality.equals(videoQuality)) {
            mRequestedQuality = videoQuality.clone();
            mUpdated = false;
        }
    }

    /**
     * Returns the quality of the stream.
     *
     * @return the video quality
     */
    public VideoQuality getVideoQuality() {
        return mRequestedQuality;
    }

    /**
     * Some data (SPS and PPS params) needs to be stored when {@link #getSessionDescription()} is called
     *
     * @param prefs The SharedPreferences that will be used to save SPS and PPS parameters
     */
    public void setPreferences(SharedPreferences prefs) {
        mSettings = prefs;
    }

    /**
     * Configures the stream. You need to call this before calling {@link #getSessionDescription()}
     * to apply your configuration of the stream.
     */
    public synchronized void configure() throws IllegalStateException, IOException {
        super.configure();
        mOrientation = mRequestedOrientation;
    }

    public synchronized void start() throws IllegalStateException, IOException {
        super.start();
        log("Stream configuration: FPS: " + mQuality.framerate + " Width: " + mQuality.resX + " Height: " + mQuality.resY);
    }

    /**
     * Stops the stream.
     */
    public synchronized void stop() {

        if (mCameraHelper != null) {
            mCameraHelper.setOnPictureTakeListener(null);
        }
        super.stop();
    }

    /**
     * Start preview.
     *
     * @throws CameraInUseException    the camera in use exception
     * @throws InvalidSurfaceException the invalid surface exception
     * @throws RuntimeException        the runtime exception
     */
    public synchronized void startPreview()
            throws CameraInUseException,
            InvalidSurfaceException,
            RuntimeException {
        log("startPreview");
        mCameraHelper.open(mSurfaceTexture);

    }

    /**
     * Stops the preview.
     */
    public synchronized void stopPreview() {
        log("stopPreview");
        mCameraHelper.close();
    }

    /**
     * Video encoding is done by a MediaRecorder.
     */
    protected void encodeWithMediaRecorder() throws IOException, ConfNotSupportedException {

        log("Video encoded using the MediaRecorder API");
        createSockets();
        try {
            mMediaRecorder = new MediaRecorder();
            //mMediaRecorder.setCamera(mCamera);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setVideoEncoder(mVideoEncoder);
            mMediaRecorder.setPreviewDisplay(mSurface);
            mMediaRecorder.setVideoSize(mRequestedQuality.resX, mRequestedQuality.resY);
            mMediaRecorder.setVideoFrameRate(mRequestedQuality.framerate);

            // The bandwidth actually consumed is often above what was requested
            mMediaRecorder.setVideoEncodingBitRate((int) (mRequestedQuality.bitrate * 0.8));

            // We write the output of the camera in a local socket instead of a file !
            // This one little trick makes streaming feasible quiet simply: data from the camera
            // can then be manipulated at the other end of the socket
            FileDescriptor fd = null;
            if (sPipeApi == PIPE_API_PFD) {
                fd = mParcelWrite.getFileDescriptor();
            } else {
                fd = mSender.getFileDescriptor();
            }
            mMediaRecorder.setOutputFile(fd);

            mMediaRecorder.prepare();
            mMediaRecorder.start();

        } catch (Exception e) {
            throw new ConfNotSupportedException(e.getMessage());
        }

        InputStream is = null;

        if (sPipeApi == PIPE_API_PFD) {
            is = new ParcelFileDescriptor.AutoCloseInputStream(mParcelRead);
        } else {
            is = mReceiver.getInputStream();
        }

        // This will skip the MPEG4 header if this step fails we can't stream anything :(
        try {
            byte buffer[] = new byte[4];
            // Skip all atoms preceding mdat atom
            while (!Thread.interrupted()) {
                while (is.read() != 'm') ;
                is.read(buffer, 0, 3);
                if (buffer[0] == 'd' && buffer[1] == 'a' && buffer[2] == 't') break;
            }
        } catch (IOException e) {
            Log.e(TAG, "Couldn't skip mp4 header :/");
            stop();
            throw e;
        }

        // The packetizer encapsulates the bit stream in an RTP stream and send it over the network
        mPacketizer.setInputStream(is);
        mPacketizer.start();

        mStreaming = true;

    }


    /**
     * Video encoding is done by a MediaCodec.
     */
    protected void encodeWithMediaCodec() throws RuntimeException, IOException {
        log("Video encoded using the MediaCodec API with a buffer");

        if (mSettings != null) {
            Editor editor = mSettings.edit();
            editor.putInt(PREF_PREFIX + "fps" + mRequestedQuality.framerate + "," + mCameraImageFormat + "," + mRequestedQuality.resX + mRequestedQuality.resY, mQuality.framerate);
            editor.commit();
        }

        log("mQuality = " + mQuality);
        EncoderDebugger debugger = EncoderDebugger.debug(mSettings, mQuality.resX, mQuality.resY);
        final NV21Convertor convertor = debugger.getNV21Convertor();
        log("debugger.getEncoderName() = " + debugger.getEncoderName());

        mMediaCodec = MediaCodec.createByCodecName(debugger.getEncoderName());;
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", mQuality.resX, mQuality.resY);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mQuality.bitrate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mQuality.framerate);
        log(" debugger.getEncoderColorFormat() = " +  debugger.getEncoderColorFormat());

        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,debugger.getEncoderColorFormat());
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        mCameraHelper.setOnPictureTakeListener(new CameraImplBase.OnPictureTakeListener() {
            @Override
            public void onPictureTaken(byte[] imageBytes) {
                //log("onPictureTaken");
                long now = System.nanoTime() / 1000;
                final long timeoutUsec = 10000l;
                try {
                    int bufferIndex = mMediaCodec.dequeueInputBuffer(timeoutUsec);
                    if (bufferIndex >= 0) {
                        ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(bufferIndex);
                        inputBuffer.clear();
                       // if (imageBytes == null) log("Symptom of the \"Callback buffer was to small\" problem...");
                       // else convertor.convert(imageBytes, inputBuffer);
                        inputBuffer.put(imageBytes,0,imageBytes.length);
                        mMediaCodec.queueInputBuffer(bufferIndex, 0, imageBytes.length, now, 0);

                    } else {
                        log("No buffer available !");
                    }
                }catch (Exception ex){
                    log("ex = " + ex.getMessage());
                }
            }
        });


        // The packetizer encapsulates the bit stream in an RTP stream and send it over the network
        log("mPacketizer = " + mPacketizer);
        mPacketizer.setInputStream(new MediaCodecInputStream(mMediaCodec));
        mPacketizer.start();

        mStreaming = true;
    }


    /**
     * Returns a description of the stream using SDP.
     * This method can only be called after {@link Stream#configure()}.
     *
     * @throws IllegalStateException Thrown when {@link Stream#configure()} wa not called.
     */
    public abstract String getSessionDescription() throws IllegalStateException;


}
