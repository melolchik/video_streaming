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

    protected final static String TAG = "VideoStream";

    protected VideoQuality mRequestedQuality = VideoQuality.DEFAULT_VIDEO_QUALITY.clone();
    protected VideoQuality mQuality = mRequestedQuality.clone();
    protected SurfaceTexture mSurfaceTexture = null;
    protected Surface mSurface = null;
    protected SharedPreferences mSettings = null;
    protected int mVideoEncoder;
    protected int mRequestedOrientation = 0, mOrientation = 0;


    protected boolean mCameraOpenedManually = true;
    protected boolean mFlashEnabled = false;
    protected boolean mSurfaceReady = false;
    protected boolean mUnlocked = false;
    protected boolean mPreviewStarted = false;
    protected boolean mUpdated = false;

    protected String mMimeType;
    protected String mEncoderName;
    protected int mEncoderColorFormat;
    protected int mCameraImageFormat;
    protected int mMaxFps = 0;

    protected CameraImplBase mCameraHelper;

    public VideoStream(Context context) {
        super();
        mCameraHelper = new CameraHelperImpl1(context, mQuality);
    }

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

    /**
     * Starts the stream.
     * This will also open the camera and display the preview
     * if {@link #startPreview()} has not already been called.
     */
    public synchronized void start() throws IllegalStateException, IOException {
        if (!mPreviewStarted) mCameraOpenedManually = false;
        super.start();
        log("Stream configuration: FPS: " + mQuality.framerate + " Width: " + mQuality.resX + " Height: " + mQuality.resY);
    }

    /**
     * Stops the stream.
     */
    public synchronized void stop() {
		/*if (mCamera != null) {
			if (mMode == MODE_MEDIACODEC_API) {
				mCamera.setPreviewCallbackWithBuffer(null);
			}
			super.stop();
			// We need to restart the preview
			if (!mCameraOpenedManually) {
				destroyCamera();
			} else {
				try {
					startPreview();
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
			}
		}*/
    }

    public synchronized void startPreview()
            throws CameraInUseException,
            InvalidSurfaceException,
            RuntimeException {

        mCameraOpenedManually = true;
        //if (!mPreviewStarted) {
        mCameraHelper.open(mSurfaceTexture);
        //}
    }

    /**
     * Stops the preview.
     */
    public synchronized void stopPreview() {
        mCameraOpenedManually = false;
        //stop();
        //if(mPreviewStarted){
        mCameraHelper.close();
        //}
    }

    /**
     * Video encoding is done by a MediaRecorder.
     */
    protected void encodeWithMediaRecorder() throws IOException, ConfNotSupportedException {

        log("Video encoded using the MediaRecorder API");


        createSockets();

        // Reopens the camera if needed
        //destroyCamera();
        //createCamera();

        // The camera must be unlocked before the MediaRecorder can use it
        //unlockCamera();

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

        // Updates the parameters of the camera if needed
        //createCamera();
        //updateCamera();

        // Estimates the frame rate of the camera
        //measureFramerate();

        if (mSettings != null) {
            Editor editor = mSettings.edit();
            editor.putInt(PREF_PREFIX+"fps"+mRequestedQuality.framerate+","+mCameraImageFormat+","+mRequestedQuality.resX+mRequestedQuality.resY, mQuality.framerate);
            editor.commit();
        }

        // Starts the preview if needed
        if (!mPreviewStarted) {
            try {
                //mCamera.startPreview();
                mPreviewStarted = true;
            } catch (RuntimeException e) {
                //destroyCamera();
                //throw e;
            }
        }

        log("mQuality = " + mQuality);
        EncoderDebugger debugger = EncoderDebugger.debug(mSettings, mQuality.resX, mQuality.resY);
        final NV21Convertor convertor = debugger.getNV21Convertor();

        mMediaCodec = MediaCodec.createByCodecName(debugger.getEncoderName());
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", mQuality.resX, mQuality.resY);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mQuality.bitrate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mQuality.framerate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, debugger.getEncoderColorFormat());
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();

        mCameraHelper.setOnPictureTakeListener(new CameraImplBase.OnPictureTakeListener() {
            @Override
            public void onPictureTaken(byte[] imageBytes) {
                log("onPictureTaken");
                long now = System.nanoTime() / 1000;
                ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
                int bufferIndex = mMediaCodec.dequeueInputBuffer(500000);
                if (bufferIndex >= 0) {
                    inputBuffers[bufferIndex].clear();
                    //if (imageBytes == null) log("Symptom of the \"Callback buffer was to small\" problem...");
                    //else convertor.convert(imageBytes, inputBuffers[bufferIndex]);
                    if (mCameraHelper instanceof CameraHelperImpl1) {
                        convertor.convert(imageBytes, inputBuffers[bufferIndex]);
                    } else {
                        inputBuffers[bufferIndex].put(imageBytes);
                    }
                    mMediaCodec.queueInputBuffer(bufferIndex, 0, inputBuffers[bufferIndex].position(), now, 0);
                } else {
                    log("No buffer available !");
                }
            }
        });

		/*Camera.PreviewCallback callback = new Camera.PreviewCallback() {
			long now = System.nanoTime()/1000, oldnow = now, i=0;
			ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				oldnow = now;
				now = System.nanoTime()/1000;
				if (i++>3) {
					i = 0;
					//Log.d(TAG,"Measured: "+1000000L/(now-oldnow)+" fps.");
				}
				try {
					int bufferIndex = mMediaCodec.dequeueInputBuffer(500000);
					if (bufferIndex>=0) {
						inputBuffers[bufferIndex].clear();
						if (data == null) Log.e(TAG,"Symptom of the \"Callback buffer was to small\" problem...");
						else convertor.convert(data, inputBuffers[bufferIndex]);
						mMediaCodec.queueInputBuffer(bufferIndex, 0, inputBuffers[bufferIndex].position(), now, 0);
					} else {
						Log.e(TAG,"No buffer available !");
					}
				} finally {
					//mCamera.addCallbackBuffer(data);
				}
			}
		};*/

		/*for (int i=0;i<10;i++) mCamera.addCallbackBuffer(new byte[convertor.getBufferSize()]);
		mCamera.setPreviewCallbackWithBuffer(callback);
*/
        // The packetizer encapsulates the bit stream in an RTP stream and send it over the network
        mPacketizer.setInputStream(new MediaCodecInputStream(mMediaCodec));
        mPacketizer.start();

        mStreaming = true;
    }


    /**
     * Video encoding is done by a MediaCodec.
     * But here we will use the buffer-to-surface method
     */
/*	@SuppressLint({ "InlinedApi", "NewApi" })
	protected void encodeWithMediaCodecMethod2() throws RuntimeException, IOException {

		Log.d(TAG,"Video encoded using the MediaCodec API with a surface");

		// Updates the parameters of the camera if needed
		createCamera();
		updateCamera();

		// Estimates the frame rate of the camera
		measureFramerate();

		EncoderDebugger debugger = EncoderDebugger.debug(mSettings, mQuality.resX, mQuality.resY);

		mMediaCodec = MediaCodec.createByCodecName(debugger.getEncoderName());
		MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", mQuality.resX, mQuality.resY);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mQuality.bitrate);
		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mQuality.framerate);	
		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
		mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		Surface surface = mMediaCodec.createInputSurface();
		((SurfaceView)mSurfaceView).addMediaCodecSurface(surface);
		mMediaCodec.start();

		// The packetizer encapsulates the bit stream in an RTP stream and send it over the network
		mPacketizer.setInputStream(new MediaCodecInputStream(mMediaCodec));
		mPacketizer.start();

		mStreaming = true;

	}*/

    /**
     * Returns a description of the stream using SDP.
     * This method can only be called after {@link Stream#configure()}.
     *
     * @throws IllegalStateException Thrown when {@link Stream#configure()} wa not called.
     */
    public abstract String getSessionDescription() throws IllegalStateException;


    /**
     * Computes the average frame rate at which the preview callback is called.
     * We will then use this average frame rate with the MediaCodec.
     * Blocks the thread in which this function is called.
     */
	/*private void measureFramerate() {
		final Semaphore lock = new Semaphore(0);

		final Camera.PreviewCallback callback = new Camera.PreviewCallback() {
			int i = 0, t = 0;
			long now, oldnow, count = 0;
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				i++;
				now = System.nanoTime()/1000;
				if (i>3) {
					t += now - oldnow;
					count++;
				}
				if (i>20) {
					mQuality.framerate = (int) (1000000/(t/count)+1);
					lock.release();
				}
				oldnow = now;
			}
		};

		mCamera.setPreviewCallback(callback);

		try {
			lock.tryAcquire(2,TimeUnit.SECONDS);
			Log.d(TAG,"Actual framerate: "+mQuality.framerate);
			if (mSettings != null) {
				Editor editor = mSettings.edit();
				editor.putInt(PREF_PREFIX+"fps"+mRequestedQuality.framerate+","+mCameraImageFormat+","+mRequestedQuality.resX+mRequestedQuality.resY, mQuality.framerate);
				editor.commit();
			}
		} catch (InterruptedException e) {}

		mCamera.setPreviewCallback(null);

	}*/

}
