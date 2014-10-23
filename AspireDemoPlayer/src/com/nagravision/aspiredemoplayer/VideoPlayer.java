package com.nagravision.aspiredemoplayer;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.nagravision.aspiredemoplayer.drm.DrmAgent;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCrypto;
import android.media.MediaCryptoException;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.MediaController;

public class VideoPlayer extends Activity 
	implements SurfaceHolder.Callback, MediaController.MediaPlayerControl {

	/* Values for supported/handled messages 'what' */
	public static final int DO_RESIZE = 1;
	public static final int DO_SECURE = 2;
	public static final int DO_ADVANCE = 3;
	public static final int DO_EOS = 4;
	public static final int DO_METADATA = 5;
	public static final int DO_STOP_PLAY = 7;
	public static final int DO_SEEK_TO = 8;

	/* Values for SECURE message */
	public static final int DO_SECURE_FALSE = 0;
	public static final int DO_SECURE_TRUE = 1;

	/* Keys values for Activity bundle, Intent bundle, Messages bundles and Metadata */
	public static final String KEY_SAMPLE_TIME = "SAMPLE_TIME";
	public static final String KEY_MEDIA_NAME = "MEDIANAME";
	public static final String KEY_MEDIA_URI = "MEDIAURI";
	public static final String KEY_DURATION = "DURATION";
	public static final String KEY_CACHED_DURATION = "CACHED_DURATION";
	public static final String KEY_IS_PLAYING = "IS_PLAYING";
	public static final String KEY_MEDIA_MEDIA = "MEDIA";
	
	/* Supported mime types for tracks */
	public static final String MIMETYPE_VIDEO_AVC = "video/avc";
	public static final String MIMETYPE_AUDIO_MP4A = "audio/mp4a-latm";
	
	/* Debug tag */
	private static final String TAG = "VideoPlayer";
	
	/* The surface view supporting video ion activity */
	private SurfaceView mPreview;
	
	/* The surface holder (access to native surface) */
	private SurfaceHolder mHolder;
	
	/* Intent bundle */
	private Bundle mExtras;

	/* URI of the video obtained from Intent bundle */
	private String mVideoUri = "";
	
	/* DrmAgent instance */
	private DrmAgent mDrmAgent = null;
	
	/* Dimensions for screen & video */
	private int mMaxWidth = 1280; // Some default guess
	private int mMaxHeight = 800; // Some default guess
	private int mWidth = 0;
	private int mHeight = 0;
	
	/* Type (local/remote/audio/video/drm/nodrm) of media rendered */
	private ImageAdapter.Media mMedia = ImageAdapter.Media.INVALID;
	
	/* Handler used for UI to receive msgs from rendering thread  */
	final Handler mHandler = new PlayerThreadHandler();
	
	/* The thread that is in charge for rendering video */
	PlayerThread mPlayerThread = null;
	
	/* Media controller providing access to trick modes */
	private MediaController mMediaCtrlr = null;
	
	/* Interface with controller play/pause button */
	private boolean mIsPlaying = false;
	/* Total media duration in ms */
	private long mDuration = 0;
	/* Time of media cached */
	private long mCacheDuration = 0;
	/* Current sample time */
	private long mSampleTime = 0;

	/**
	 * 
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle xBundle) 
	{
		super.onCreate(xBundle);
		Thread.currentThread().setName("VideoPlayer UI thread");
		// Get back state and times
		if (xBundle != null)
		{
			mIsPlaying = xBundle.getBoolean(KEY_IS_PLAYING);
			mDuration = xBundle.getLong(KEY_DURATION);
			mCacheDuration = xBundle.getLong(KEY_CACHED_DURATION);
			mSampleTime = xBundle.getLong(KEY_SAMPLE_TIME);
		}
		// Setup window manager
	    //requestWindowFeature(Window.FEATURE_NO_TITLE|Window.FEATURE_ACTION_BAR|Window.FEATURE_ACTION_BAR_OVERLAY);
	    // Add view to activity
	    setContentView(R.layout.row);
	    // Setup WindowManager flags for our window
	    getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, 
	    		WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	    // Get the view supporting the video
		mPreview = (SurfaceView) findViewById(R.id.surface);
		// Setup some of its visibility options
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            /* View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |*/
            /* View.SYSTEM_UI_FLAG_LAYOUT_STABLE |*/
            View.SYSTEM_UI_FLAG_FULLSCREEN;
        //if (Build.VERSION.SDK_INT >= 19)
        //    uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE;
        mPreview.setSystemUiVisibility(uiOptions);
        
        // Get the surface holder (provided to the rendering thread)
		mHolder = mPreview.getHolder();
		// Manage its state in activity (created/destroyed/changed)
		mHolder.addCallback(this);
		// Get the intent bundle
		mExtras = getIntent().getExtras();
		// get the media URI
		mVideoUri = (String) mExtras.getSerializable(KEY_MEDIA_URI);
		// And the kind of media it is
		mMedia = (ImageAdapter.Media) mExtras.getSerializable(KEY_MEDIA_MEDIA);
		// Get DRM agent instance
		mDrmAgent = DrmAgent.getInstance();

		// Get screen size & set default video size
		Point size = new Point();
		getWindowManager().getDefaultDisplay().getSize(size);
		mMaxWidth = mWidth = size.x;
		mMaxHeight = mHeight = size.y;
		
		// Instanciate the media controller
	    mMediaCtrlr = new MediaController(this, true);
	    // Set activity as the media player for this controller
	    mMediaCtrlr.setMediaPlayer(this);
	    // Set the SurfaceView as the view on which media controller is anchored
	    mMediaCtrlr.setAnchorView(mPreview);
	    // Setup listeners for next/prev media buttons
	    mMediaCtrlr.setPrevNextListeners(
	    		new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Log.v(TAG, "Next listener of media controller");
						// TODO: implem
					}
	    		}, 
	    		new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Log.v(TAG, "Prev listener of media controller");
						// TODO: implem						
					}
	    		});
	    // Enable the media controller
	    mMediaCtrlr.setEnabled(true);
	    /* Setup listeners that displays the media controllers
	     * (basically click/move/hover on surface view and 
	     * move/hover on media controller itself)
	     */
	    // OnClick listener for Surface view
	    mPreview.setOnClickListener(
	    		new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mMediaCtrlr.show(5000);
					}
	    		});
	    // Motion listener for Surface view
	    mPreview.setOnGenericMotionListener(
	    		new View.OnGenericMotionListener() {					
					@Override
					public boolean onGenericMotion(View v, MotionEvent event) {
						mMediaCtrlr.show(2000);
						return false;
					}
				});
	    // Touch event listener for Surface view
	    mPreview.setOnTouchListener(
	    		new View.OnTouchListener() {					
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						mMediaCtrlr.show(2000);
						return false;
					}
				});
	    // HOver listener for Surface view
//	    mPreview.setOnHoverListener(
//	    		new View.OnHoverListener() {
//					@Override
//					public boolean onHover(View v, MotionEvent event) {
//						mMediaCtrlr.show(2000);
//						return false;
//					}
//				});
	    // Motion listener for media controller
	    mMediaCtrlr.setOnGenericMotionListener(
	    		new View.OnGenericMotionListener() {					
					@Override
					public boolean onGenericMotion(View v, MotionEvent event) {
						mMediaCtrlr.show(2000);
						return false;
					}
				});
	    // HOver listener for media controller
//	    mMediaCtrlr.setOnHoverListener(
//	    		new View.OnHoverListener() {
//					@Override
//					public boolean onHover(View v, MotionEvent event) {
//						mMediaCtrlr.show(2000);
//						return false;
//					}
//				});
	}

	/**
	 * onBackPressed
	 * 
	 * Stop the thread player, release it and go back to main activity
	 */
	@Override
	public void onBackPressed() {
		Log.v(TAG, "Back pressed");
		releaseMediaPlayer();
		mPlayerThread = null;
		onDestroy();
		startActivity(new Intent(getApplicationContext(), MainActivity.class));
	}
	
	/**
	 * onKeyDown
	 * 
	 *  Stop the thread player, release it and go back to main activity
	 *  when the back button is pressed down
	 */
//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//	    if (keyCode == KeyEvent.KEYCODE_BACK)
//            onBackPressed();
//
//	    return super.onKeyDown(keyCode, event);
//	}
	
	/**
	 * onKeyUp
	 * 
	 *  Stop the thread player, release it and go back to main activity
	 *  when the back button is pressed up (sometimes the down event is 
	 *  missed, because of the heavy thread activity ?)
	 * 
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK)
            onBackPressed();

	    return super.onKeyUp(keyCode, event);
	}
	
	/*
	 * surfaceChanged listener
	 * 
	 * Called when surface properties changed (Video view is modified on the screen)
	 */
	@Override
	public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
		// Set video size 
		mHolder.setFixedSize(mWidth, mHeight);
		// And start media playback
		startMediaPlayback();
	}

	/*
	 * surfaceDestroyed listener
	 * 
	 * Called when surface is destroyed (Video view is removed from screen)
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder surfaceholder) {
	}

	/*
	 * surfaceCreated listener
	 * 
	 * Called when surface is crated (Video view is about to be displayed on screen)
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// Setup Surface View
		mPreview.setZOrderMediaOverlay(true);
		// And the Surface Holder
		mHolder.setKeepScreenOn(true);
		mHolder.setFormat(PixelFormat.TRANSLUCENT);
		mHolder.setFixedSize(mWidth, mHeight);
	}

	/*
	 * onPause listener
	 * 
	 * Called when the activity is paused
	 */
	@Override
	protected void onPause() {
		// Do pause
		super.onPause();
		// Release resources held by player
		releaseMediaPlayer();
	}

	/*
	 * onDestroy listener
	 * 
	 * Called when the activity is destroyed
	 */
	@Override
	protected void onDestroy() {
		// Release player resources
		releaseMediaPlayer();
		// And destroy activity
		super.onDestroy();
	}

	/*
	 * onSaveInstanceState listener
	 * 
	 * Called when activity state changes and it is required to 
	 * be saved for further restoration
	 */
	@Override
	public void onSaveInstanceState(Bundle xBundle) {
		// Let super class to also save its state
		super.onSaveInstanceState(xBundle);
		// Save important flags & values
		xBundle.putBoolean(KEY_IS_PLAYING, mIsPlaying);
		xBundle.putLong(KEY_DURATION, mDuration);
		xBundle.putLong(KEY_CACHED_DURATION, mCacheDuration);
		xBundle.putLong(KEY_SAMPLE_TIME, mSampleTime);
	}
	
	/*
	 * onRestoreInstanceState listener
	 * 
	 * Called when activity state changes and it is required to 
	 * restore for recalling a specific state
	 */
	@Override
	public void onRestoreInstanceState(Bundle xBundle) {
		// Let super class restore its own state
		super.onRestoreInstanceState(xBundle);
		// Then restore our important flags & values
		mIsPlaying = xBundle.getBoolean(KEY_IS_PLAYING);
		mDuration = xBundle.getLong(KEY_DURATION);
		mCacheDuration = xBundle.getLong(KEY_CACHED_DURATION);
		mSampleTime = xBundle.getLong(KEY_SAMPLE_TIME);
	}
	
	/*
	 * releaseMediaPlayer
	 * 
	 * Clean exit for player thread and remove ref on it (GC can then collect it)
	 */
	private void releaseMediaPlayer() 
	{
		try 
		{
			if (null != this.mPlayerThread) 
			{
				stopStream();
				mPlayerThread.interrupt();
				mPlayerThread.join(500);
				mPlayerThread = null;
			}
		} 
		catch (InterruptedException e) 
		{
			Log.v(TAG, "Player Thread interrupted !!");
		}
	}

	/*
	 * startMediaPlayback
	 * 
	 * Start playing a media which ref was provided in Intent
	 */
	private void startMediaPlayback() 
	{
		if (mPlayerThread == null) {
			mPlayerThread = new PlayerThread(mHolder.getSurface());
			mPlayerThread.setDataSource(mVideoUri);
			mPlayerThread.start();
		}
	}

	/*
	 * PlayerThreadHandler class
	 * 
	 * This class provides implementation for handleMessage in order 
	 * to receive states and values from player rendering thread
	 */
	private class PlayerThreadHandler extends Handler 
	{
		public PlayerThreadHandler() 
		{
			super(Looper.getMainLooper());
			Log.v(TAG, "PlayerThreadHandler is thread: " + Thread.currentThread().getName());
		}
		
		@Override
		public void handleMessage(Message inMsg) 
		{
			//PlayerThread aPlayerThread = (PlayerThread)inMsg.obj;
			switch(inMsg.what)
			{
			case DO_RESIZE:
				// We just got the video size from its metadata and then need to resize view
				Log.i(TAG, "width=" + inMsg.arg1 + " height=" + inMsg.arg2);
				int width = inMsg.arg1;
				int height = inMsg.arg2;
				int rectWidth = mPreview.getWidth();
				int rectHeight = mPreview.getHeight();
				Log.i(TAG, "cur width=" + rectWidth + " cur height=" + rectHeight);
				if (height != rectHeight)
				{
					mWidth = mMaxWidth;
					mHeight = (height * mMaxWidth) / width;
					Log.i(TAG, "Setting new size: (" + mWidth + "," + mHeight + ")");
					VideoPlayer.this.mHolder.setFixedSize(mWidth, mHeight);
				}
				break;
				
			case DO_SECURE:
				// We are actually deciphering a ciphered media: go secure to
				// avoid SurfaceView screenshot
				Log.i(TAG, "Going to secure ...");
				VideoPlayer.this.mPreview.setSecure(inMsg.arg1 == DO_SECURE_TRUE ? true : false);
				break;

			case DO_ADVANCE:
				// We just processed one more sample, set new current time
				mSampleTime = inMsg.getData().getLong(KEY_SAMPLE_TIME) / 1000;
				mIsPlaying = true;
				//Log.v(TAG, "Sample time = " + String.format("%02.2f s", mSampleTime / 1000000.0));
				break;

			case DO_EOS:
				// We reached end of stream: update state flags
				Log.i(TAG, "End of stream ...");
				mIsPlaying = false;
				break;

			case DO_METADATA:
				// We found some important metadata: store values
				Log.i(TAG, "Got metadata ...");
				mDuration = inMsg.getData().getLong(KEY_DURATION);
				break;

			case DO_STOP_PLAY:
				// User just stopped playing... or what else
				Log.i(TAG, "Stop play ...");
				mIsPlaying = false;
				break;

			case DO_SEEK_TO:
				// a Trick mode was used
				Log.i(TAG, "Seek to " + inMsg.arg1 + "...");
				if (mIsPlaying)
				{
					pause();
					mPlayerThread.seekTo(inMsg.arg1);
					start();
				}
				else {
					mPlayerThread.seekTo(inMsg.arg1);
				}
			}
		}		
	}
	
	/*
	 * PlayerThread class
	 * 
	 * This is the class providing the media rendering
	 */
	private class PlayerThread extends Thread 
	{
		// The media extractor (a.k.a. demux)
		private MediaExtractor mExtractor;
		// The metadata retriever used for getting metadata values (duration)
		private MediaMetadataRetriever mMetadataRetr = null;
		// All media codecs used for tracks of this media
		private MediaCodec[] mMediaCodec;
		// The low level surface used for rendering video frames
		private Surface mSurface;
		// The media datasource (file path or url)
		private String mDataSource;
		// The media crypto object used for deciphering packets/fragments
		MediaCrypto mMediaCrypto = null;
		
		/*
		 * Some flags used in player state management
		 */
		// Used for synchronization
		private final Object mStopLock = new Object();
		// Stream does not advance anymore
		private volatile boolean mStopStream = false;
		// Stream pause was requested
		private volatile boolean mPauseStreamRequested = false;
		// Stream un-pause was requested
		private volatile boolean mUnPauseStreamRequested = false;
		// Stream is paused (resources are not released for allowing quick restart)
		private volatile boolean mPauseStream = false;
		// Pause was notified through message (to avoid sending message continuously)
		private volatile boolean mPauseNotified = false;
		// We reached en of stream
		volatile boolean mIsEndOfStream;
		// The stream needs to be secured or not
		volatile boolean mSecured = false;
		
		// Time we want to seek to (in micro seconds)
		private long mSeekToTimeUs = -1;
		// Crypto infos (about how content is protected)
		MediaCodec.CryptoInfo mCryptoInfo = new MediaCodec.CryptoInfo();
		// Android Time (in ms) when playing was started
		long mStartMs = 0;
		// The PSSH data found in media when ciphered (TODO: which one if several are available???)
		String mPsshData;
		// Rendering buffers used for deciphering/decoding content
		RenderingBuffers[] mBuffers;
		// Audio track ID
		private int mAudioTrackId = -1;
		// Buffers timeout value in µs
		private final long TIME_OUT_MICRO_SECS = 10000000;
		// Number of tracks available in media
		private int mNumTracks = 0;
		// The main video track rendered (one used for timing others)
		private int mMainTrackId = 0;
		// Media formats (one per track)
		private MediaFormat[] mFormat;
		
		private long mPauseStarted = 0;
		private AudioRenderer mRenderAudioThread;
		AudioRenderer mAudioRenderer = null;
		private Thread mAudioRendererThread = null;
		private final Object mAudioLock = new Object();
		private volatile boolean mAudioSync = false;

		// Public constructor
		public PlayerThread(Surface xSurface) 
		{
			// Store the low level surface provided by activity
			this.mSurface = xSurface;
		}

		// Activity requests to seek at a specific position
		public void seekTo(int pos) {
			// Set time value in Âµs
			mSeekToTimeUs = pos * 1000;
			for (int i = 0; i < mNumTracks; ++i) 
			{
				if (mMediaCodec[i] != null)
					mMediaCodec[i].flush();
			}
		}

		// The thread implementation
		@Override
		public void run() 
		{
//			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

			// Infinite loop
			while (true)
				// Check if we got a source for data
				if (!this.mDataSource.equals("")) 
				{
					// Play the content
					try 
					{
						playContent();
					}
					catch (InterruptedException e) 
					{
						Log.v(TAG, "Player thread stopped");
					}

					// Stream was stopped. Thread will RIP
					if (mStopStream || mIsEndOfStream) 
					{
						for (int i = 0; i < mNumTracks; ++i) 
						{
							if (mMediaCodec[i] != null)
							{
								mMediaCodec[i].stop();
								mMediaCodec[i].release();
								mMediaCodec[i] = null;
							}						
							if (mMediaCodec[i] != null)
								mBuffers[i] = null;
						}

						mExtractor.release();
						mMediaCodec = null;
						mExtractor = null;
						mBuffers = null;

						break;
					}
					// Stream was paused, Thread goes on
					else if (mPauseStreamRequested)
					{
						synchronized(mStopLock)
						{
							// Keep resources, we will start again soon
							mPauseStreamRequested = false;
							mPauseStream = true;
							mStopLock.notifyAll();
						}
						
						mPauseStarted = System.currentTimeMillis();
						// Wait and check at some regular interval if
						// start was requested
						while (!mUnPauseStreamRequested)
							try 
							{
								// Wait for play
								Thread.sleep(100);
							} 
							catch (InterruptedException e) 
							{
								Log.v(TAG, "Rendering paused interrupted...");
							}
						
						synchronized(mStopLock)
						{
							mPauseStream = false;
							mUnPauseStreamRequested = false;
							mStopLock.notifyAll();
						}
					}

				} 
				else 
					Log.v(TAG, "No Media Uri Call setDataSource() before thread start()");				

		}

		// Set data source for media
		public void setDataSource(String xDataSource) 
		{
			this.mDataSource = xDataSource;
			Log.v(TAG, "Media Uri: " + this.mDataSource);
		}

		// Do play the media content
		private void playContent() throws InterruptedException 
		{
			try 
			{
				if (mExtractor == null)
				{
					mExtractor = new MediaExtractor();
					mExtractor.setDataSource(mDataSource);
					if (mDuration == 0)
					{
						try
						{
							mMetadataRetr = new MediaMetadataRetriever();
							URI uri = URI.create(mDataSource);
							if (VideoPlayer.this.mMedia.equals(ImageAdapter.Media.DRM_STREAM_AUDIO) ||
									VideoPlayer.this.mMedia.equals(ImageAdapter.Media.DRM_STREAM_VIDEO) ||
									VideoPlayer.this.mMedia.equals(ImageAdapter.Media.STREAM_AUDIO) ||
									VideoPlayer.this.mMedia.equals(ImageAdapter.Media.STREAM_VIDEO))
							{
								Map<String, String> headers = new HashMap<String, String>();
								headers.put("Pragma", "none"); // Have to provide one header else JNI raise exception
								mMetadataRetr.setDataSource(uri.toASCIIString(), headers);
							}
							else
								mMetadataRetr.setDataSource(uri.getPath());
							mDuration = Long.parseLong(mMetadataRetr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
							mMetadataRetr = null;							
						}
						catch (Exception e)
						{
							Log.e(TAG, "Metadata retrieving failed", e);
						}

						Message aMsg1 = VideoPlayer.this.mHandler.obtainMessage(VideoPlayer.DO_METADATA);
						aMsg1.getData().putLong(KEY_DURATION, mDuration);
						aMsg1.sendToTarget();
					}

					mNumTracks = mExtractor.getTrackCount();
					Log.v(TAG, "Number of tracks: " + mNumTracks);

					mBuffers = new RenderingBuffers[mNumTracks];
					mMediaCodec = new MediaCodec[mNumTracks];
					mFormat = new MediaFormat[mNumTracks];

					for (int i = 0; i < mNumTracks; ++i) 
					{
						mFormat[i] = mExtractor.getTrackFormat(i);
						String mime = mFormat[i].getString(MediaFormat.KEY_MIME);
						Log.v(TAG, "Track[" + i + "].mime = " + mime);
						
						//
						// A Video track
						//
						if (mime.equals(MIMETYPE_VIDEO_AVC)) {
							mMainTrackId = i;
							mExtractor.selectTrack(i);
							// Got metadata from media ?
							if (mFormat[i].containsKey(MediaFormat.KEY_HEIGHT) && 
									mFormat[i].containsKey(MediaFormat.KEY_WIDTH))
							{
								// Yes: send back to UI thread
								int aWidth = mFormat[i].getInteger(MediaFormat.KEY_WIDTH);
								int aHeight = mFormat[i].getInteger(MediaFormat.KEY_HEIGHT);
								VideoPlayer.this.mHandler.obtainMessage(VideoPlayer.DO_RESIZE, aWidth, aHeight, this).sendToTarget();
							}

							mMediaCodec[i] = MediaCodec.createDecoderByType(mime);
							Map<UUID, byte[]> psshInfo = mExtractor.getPsshInfo();
							if (null != psshInfo) {
								for (Iterator<UUID> it = psshInfo.keySet()
										.iterator(); it.hasNext();) {
									UUID uuid = it.next();
									if (MediaCrypto.isCryptoSchemeSupported(uuid)) {
										Log.v(TAG, "Supported crypto scheme");
										mPsshData = new String(psshInfo.get(uuid));
										Log.v(TAG,
												"PSSH for UUID: " + uuid.toString()
												+ " has data :" + mPsshData);

										mMediaCrypto = new MediaCrypto(uuid, psshInfo.get(uuid));

										mSecured = true;
										break;
									}
								}
							}

							// Tell UI about secure or not
							VideoPlayer.this.mHandler.obtainMessage(VideoPlayer.DO_SECURE, 
									mSecured ? VideoPlayer.DO_SECURE_TRUE : VideoPlayer.DO_SECURE_FALSE, 
											0, this).sendToTarget();

							mMediaCodec[i].configure(mFormat[i], mSurface, mMediaCrypto, 0);

							if (!mSecured)
								Log.v(TAG, "Unsupported crypto scheme");
						}

						//
						// An audio track
						//
						if (mime.equals(MIMETYPE_AUDIO_MP4A)) 
						{
							if (mAudioTrackId == -1)
							{
								mAudioTrackId = i;
								mMediaCodec[i] = MediaCodec.createDecoderByType(mime);
								mMediaCodec[i].configure(mFormat[i], null, mMediaCrypto, 0);
								mAudioRenderer = new AudioRenderer(this, mAudioTrackId);
								mAudioRendererThread = new Thread(mAudioRenderer);
							}
						}
					}
					for (int i = 0; i < mNumTracks; ++i) 
					{
						if (mMediaCodec[i] != null) 
						{
							mMediaCodec[i].start();
							mBuffers[i] = new RenderingBuffers();
							mBuffers[i].mInputBuffers = mMediaCodec[i].getInputBuffers();
							mBuffers[i].mOutputBuffers = mMediaCodec[i].getOutputBuffers();
							mBuffers[i].mBufferInfo = new BufferInfo();
						}
					}
					if (mAudioRendererThread != null)
						mAudioRendererThread.start();
				}
				// We paused, then we restart
				else
				{
					if (mSeekToTimeUs != -1) {
						mExtractor.seekTo(mSeekToTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
						mStartMs += mSampleTime - (mSeekToTimeUs / 1000);
						mSeekToTimeUs = -1;
					}
					mAudioRendererThread.interrupt();
				}
				
				mIsEndOfStream = false;
				mStartMs += System.currentTimeMillis() - mPauseStarted;
				while (true) 
				{
					
					mCacheDuration = mExtractor.getCachedDuration() / 1000;
					if (isInterrupted())
					{
						mStopStream = true;
						break;
					}

					if (mStopStream || mPauseStreamRequested)
					{
						if (mPauseStreamRequested) {
							mSeekToTimeUs = mExtractor.getSampleTime();
							Message aMsg7 = VideoPlayer.this.mHandler.obtainMessage(VideoPlayer.DO_ADVANCE, this);
							aMsg7.getData().putLong(VideoPlayer.KEY_SAMPLE_TIME, mSeekToTimeUs);
							aMsg7.sendToTarget();
						}
						break;
					}
 
					if (renderVideo(mMainTrackId))
					{
						mStopStream = true;
						break;
					}					
				}

			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			catch (MediaCryptoException e) 
			{
				Log.e(TAG, "Could not instanciate MediaCrypto ! " + e);
			}
			VideoPlayer.this.mHandler.obtainMessage(VideoPlayer.DO_STOP_PLAY).sendToTarget();
		}

		private void inputBuffer(int xtrack) 
		{
//			mExtractor.selectTrack(xtrack);
			int inputBufferIndex = mMediaCodec[xtrack]
					.dequeueInputBuffer(TIME_OUT_MICRO_SECS);
			if (inputBufferIndex >= 0) 
			{
				ByteBuffer buffer = mBuffers[xtrack].mInputBuffers[inputBufferIndex];
				int sampleSize = mExtractor.readSampleData(buffer, 0);
				if (sampleSize < 0) {
					Log.v(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM\n");
					mMediaCodec[xtrack].queueInputBuffer(inputBufferIndex, 0,
							0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
					// Tell UI about EOS
					VideoPlayer.this.mHandler.obtainMessage(VideoPlayer.DO_EOS, this).sendToTarget();
				} 
				else 
				{
					mExtractor.getSampleCryptoInfo(mCryptoInfo);

					if (mCryptoInfo.mode == MediaCodec.CRYPTO_MODE_UNENCRYPTED) 
					{
						mMediaCodec[xtrack].queueInputBuffer(inputBufferIndex,
								0, sampleSize, mExtractor.getSampleTime(), 0);
					} 
					else 
					{
						mCryptoInfo.key = mDrmAgent.provideKey(mPsshData);
						
						mMediaCodec[xtrack].queueSecureInputBuffer(
								inputBufferIndex, 0, mCryptoInfo,
								mExtractor.getSampleTime(), mCryptoInfo.mode);
					}
//					synchronized (mAudioLock) {
//						mAudioSync = true;
//						mAudioLock.notifyAll();
//					}
					mExtractor.advance();
				}
			}
		}

		private int outputBuffer(int xtrack) 
		{
			int outIndex = mMediaCodec[xtrack].dequeueOutputBuffer(mBuffers[xtrack].mBufferInfo, TIME_OUT_MICRO_SECS);
			
			switch (outIndex)
			{
			case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
				// not important for us, since we're using Surface
				Log.v(TAG, "INFO_OUTPUT_BUFFERS_CHANGED\n");
				mBuffers[xtrack].mOutputBuffers = mMediaCodec[xtrack].getOutputBuffers();
				break;
				
			case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
				mFormat[xtrack] = mMediaCodec[xtrack].getOutputFormat();
				Log.v(TAG,
						"INFO_OUTPUT_FORMAT_CHANGED "
								+ mFormat[xtrack]);
				if (mFormat[xtrack].containsKey(MediaFormat.KEY_HEIGHT) && 
						mFormat[xtrack].containsKey(MediaFormat.KEY_WIDTH))
				{
					// Yes: send back to UI thread
					int aWidth = mFormat[xtrack].getInteger(MediaFormat.KEY_WIDTH);
					int aHeight = mFormat[xtrack].getInteger(MediaFormat.KEY_HEIGHT);
					VideoPlayer.this.mHandler.obtainMessage(VideoPlayer.DO_RESIZE, aWidth, aHeight, this).sendToTarget();
				}
				break;
				
			case MediaCodec.INFO_TRY_AGAIN_LATER:
				Log.v(TAG, "INFO_TRY_AGAIN_LATER decoder timmed out");
				break;

				default:
				if (outIndex < 0)
					throw new RuntimeException(
	                        "unexpected result from mMediaCodec[xtrack].dequeueOutputBuffer: " +
	                                outIndex);
				break;
				
			}

			return outIndex;
		}

		private boolean renderVideo(int xTrack) 
		{
			if (!mPauseStreamRequested && !mPauseStream)
			{
				int outIndex = -1;
				if (mExtractor.getSampleTrackIndex() == xTrack)
				{
					inputBuffer(xTrack);
					outIndex = outputBuffer(xTrack);
					if (outIndex >= 0)
					{
						while (mBuffers[xTrack].mBufferInfo.presentationTimeUs / 1000 > 
						System.currentTimeMillis() - mStartMs && 
						!(mPauseStreamRequested || mPauseStream)) 
						{
							try 
							{
								sleep(10);
							} 
							catch (InterruptedException e) 
							{
								e.printStackTrace();
								break;
							}
						}
						mMediaCodec[xTrack].releaseOutputBuffer(outIndex, true);
					}

					// All decoded frames have been rendered
					if ((mBuffers[xTrack].mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) 
					{
						Log.v(TAG, "outputBuffers BUFFER_FLAG_END_OF_STREAM");
						mIsEndOfStream = true;
						// Tell UI about EOS
						VideoPlayer.this.mHandler.obtainMessage(VideoPlayer.DO_EOS, this).sendToTarget();
					}
					else 
					{
						// Tell UI about advance
						Message aMsg7 = VideoPlayer.this.mHandler.obtainMessage(VideoPlayer.DO_ADVANCE, this);
						aMsg7.getData().putLong(VideoPlayer.KEY_SAMPLE_TIME, mBuffers[xTrack].mBufferInfo.presentationTimeUs);
						aMsg7.sendToTarget();				
					}
				} else
					try {
						Thread.sleep(2);
					} catch (InterruptedException e) {
					}
			} 

			return mIsEndOfStream;
		}

		private class AudioRenderer implements Runnable
		{
			private int mAudioTrackId = -1;
			private PlayerThread mPlayerThread = null;
			// The rendered audio track
			private AudioTrack mAudioTrack;
			
			public AudioRenderer(PlayerThread xPlayerThread, int xTrack)
			{
				mAudioTrackId = xTrack;
				mPlayerThread = xPlayerThread;
				int buffsize = AudioTrack.getMinBufferSize(48000,
						AudioFormat.CHANNEL_OUT_STEREO,
						AudioFormat.ENCODING_PCM_16BIT);
				mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 48000,
						AudioFormat.CHANNEL_OUT_STEREO,
						AudioFormat.ENCODING_PCM_16BIT, buffsize,
						AudioTrack.MODE_STREAM);
				mAudioTrack.play();
			}
			
			public int getAudioSessionId()
			{
				return mAudioTrack.getAudioSessionId();
			}
			
			public void run() 
			{
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
				while (!mPlayerThread.mIsEndOfStream && !mPlayerThread.mStopStream)
				{
					if (mPlayerThread.mPauseStream)
						waitForUnPaused();

					else
					{
						if (mExtractor.getSampleTrackIndex() == mAudioTrackId)
						{
							mPlayerThread.inputBuffer(mAudioTrackId);
							int outIndex = mPlayerThread.outputBuffer(mAudioTrackId);
							if(outIndex >= 0)
							{
								//							Log.v(TAG, "Audio thread will wait " + (mBuffers[mAudioTrackId].mBufferInfo.presentationTimeUs / 1000 - System.currentTimeMillis() - mStartMs) + " ms");
								//							while (mBuffers[mAudioTrackId].mBufferInfo.presentationTimeUs / 1000 > 
								//									System.currentTimeMillis() - mStartMs && 
								//									!(mPauseStreamRequested || mPauseStream)) 
								//							{
								//								try 
								//								{
								//									sleep(10);
								//								} 
								//								catch (InterruptedException e) 
								//								{
								//									e.printStackTrace();
								//									break;
								//								}
								//							}
								ByteBuffer buffer = mBuffers[mAudioTrackId].mOutputBuffers[outIndex];
								final byte[] chunk = new byte[mBuffers[mAudioTrackId].mBufferInfo.size];
								buffer.get(chunk);
								buffer.clear();
								if(chunk.length > 0)
									mAudioTrack.write(chunk, 0, chunk.length);

								mPlayerThread.mMediaCodec[mAudioTrackId].releaseOutputBuffer(outIndex, false);
								//							waitForAudioSync();
							}
						} else
							try {
								Thread.sleep(2);
							} catch (InterruptedException e) {
							}
					}
				}
			}
		}

        /**
         * Wait for the player to be paused.
         * <p>
         * Called from any thread other than the PlayTask thread.
         */
        public void waitForAudioSync() {
        	if (Thread.currentThread().equals(mPlayerThread))
        	{
        		Log.v(TAG, "waitForPaused(): will not wait for me !!");
        		return;
        	}
            synchronized (mAudioLock) {
                while (!mAudioSync) {
                    try {
                        mAudioLock.wait();
                    } catch (InterruptedException ie) {
                        // discard
                    }
                }
                // Rearm barrier
                mAudioSync = false;
            }
        }

        /**
         * Wait for the player to be paused.
         * <p>
         * Called from any thread other than the PlayTask thread.
         */
        public void waitForPaused() {
        	if (Thread.currentThread().equals(mPlayerThread))
        	{
        		Log.v(TAG, "waitForPaused(): will not wait for me !!");
        		return;
        	}
            synchronized (mStopLock) {
                while (!mPauseStream) {
                    try {
                        mStopLock.wait();
                    } catch (InterruptedException ie) {
                        // discard
                    }
                }
            }
        }

        /**
         * Wait for the player to play.
         * <p>
         * Called from any thread other than the PlayTask thread.
         */
        public void waitForUnPaused() {
        	if (Thread.currentThread().equals(mPlayerThread))
        	{
        		Log.v(TAG, "waitForUnPaused(): will not wait for me !!");
        		return;
        	}
            synchronized (mStopLock) {
                while (mPauseStream) {
                    try {
                        mStopLock.wait();
                    } catch (InterruptedException ie) {
                        // discard
                    }
                }
            }
        }

        private class RenderingBuffers 
		{
			public ByteBuffer[] mInputBuffers;
			public ByteBuffer[] mOutputBuffers;
			public BufferInfo mBufferInfo;
		}
	}
	
	// Activity requests to stop playing
	public void stopStream() 
	{
		// Raise the corresponding flag
		mPlayerThread.mStopStream = true;
	}

	// Activity requests to pause playing
	public void pauseStream() 
	{
		// Raise corresponding flag
		mPlayerThread.mPauseStreamRequested = true;
		// Setup other flags according to current state
		mPlayerThread.mUnPauseStreamRequested = false;
		mPlayerThread.mPauseNotified = false;
	}

	// Activity requests to re-play stream
	public void unPauseStream() 
	{
		// Raise corresponding flag
		mPlayerThread.mPauseStreamRequested = false;
		// Setup other flags according to current state
		mPlayerThread.mUnPauseStreamRequested = true;
		mPlayerThread.mPauseNotified = false;
	}


	/*
	 * MediaControl.MediaPlayerControl implem
	 */

	// Called when the start button is clicked to start playing (unpause)
	@Override
	public void start() 
	{
		unPauseStream();
		mPlayerThread.waitForUnPaused();
		mIsPlaying = true;
	}

	// Called when the pause button is clicked (pause)
	@Override
	public void pause() 
	{
		mIsPlaying = false;
		pauseStream();
		mPlayerThread.waitForPaused();
	}

	// Return the media duration value
	// This value is displayed at the end of the slider
	@Override
	public int getDuration() 
	{
		return (int)mDuration;
	}

	// Return the time on the current position in the media
	// This value is used for moving the cursor symbolizing the 
	// current position in the stream on the slider
	@Override
	public int getCurrentPosition() 
	{
		return (int)mSampleTime;
	}

	// User request to seek to another position in the stream
	// (cursor was moved)
	@Override
	public void seekTo(final int pos) 
	{
		// We wait the user released the cursor to avoid doing too many seek
		mHandler.removeMessages(DO_SEEK_TO);
		mHandler.sendMessageDelayed(mHandler.obtainMessage(DO_SEEK_TO, pos, 0), 200);
	}

	// Return playing state of the player
	// Used for displaying either play button or pause button
	@Override
	public boolean isPlaying() 
	{
		return mIsPlaying;
	}

	// Return the percentage of media cached
	// Used for filling the slider for indicating cache state
	@Override
	public int getBufferPercentage() 
	{
		float percent = 0;
		if (mDuration != 0)
		{
			percent = ((float)(mCacheDuration + mSampleTime) * 100) / (float)mDuration;
			if (mCacheDuration + mSampleTime >= mDuration)
				percent = 100;
		}
		return (int)percent;
	}

	// Return a boolean indicating if player can pause
	// Used for activating pause button
	@Override
	public boolean canPause() 
	{
		return true;
	}

	// Return a boolean indicating if player can seek backward
	// Used for activating rewind button
	@Override
	public boolean canSeekBackward() 
	{
		return true;
	}

	// Return a boolean indicating if player can seek forward
	// Used for activating fast forward button
	@Override
	public boolean canSeekForward() 
	{
		return true;
	}

	// Return the audio Session ID
	// Might be used for audio priority ?
	@Override
	public int getAudioSessionId() 
	{
		return mPlayerThread.mAudioRenderer.getAudioSessionId();
	}
}