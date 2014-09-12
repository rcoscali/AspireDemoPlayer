package com.nagravision.aspiredemoplayer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.nagravision.aspiredemoplayer.drm.DrmAgent;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCrypto;
import android.media.MediaCryptoException;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class VideoPlayer extends Activity implements SurfaceHolder.Callback {

	private static final String TAG = "VideoPlayer";
	private SurfaceView mPreview;
	private SurfaceHolder mHolder;
	private Bundle mExtras;
	private static final String MEDIA = "media";
	private String mVideoName = "";
	private DrmAgent mDrmAgent = null;
	MediaExtractor mExtractor = null;
	PlayerThread mPlayerThread = null;

	/**
	 * 
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle xBundle) {
		super.onCreate(xBundle);
		setContentView(R.layout.row);
		mPreview = (SurfaceView) findViewById(R.id.surface);
		mHolder = mPreview.getHolder();
		mHolder.addCallback(this);
		mExtras = getIntent().getExtras();
		mVideoName = (String) mExtras.getSerializable("MEDIANAME");
		mExtractor = new MediaExtractor();
		mDrmAgent = DrmAgent.getInstance();
	}

	private void playVideo(ImageAdapter.Media xMedia) {
		try {
			switch (xMedia) {

			case LOCAL_VIDEO:
			case DRM_LOCAL_VIDEO:
				startVideoPlayback();
				break;
			case STREAM_VIDEO:
				break;
			default:
				break;
			}

		} catch (Exception xExcep) {
			Log.e(TAG, "error: " + xExcep.getMessage(), xExcep);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
		Log.d(TAG, "surfaceChanged called");

		this.mHolder.setFixedSize(1024, 768);
		playVideo((ImageAdapter.Media) mExtras.getSerializable(MEDIA));
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder surfaceholder) {
		Log.d(TAG, "surfaceDestroyed called");
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated called");

	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseMediaPlayer();
	}

	@Override
	protected void onDestroy() {
		releaseMediaPlayer();
		super.onDestroy();
	}

	private void releaseMediaPlayer() {

		try {
			if (null != this.mPlayerThread) {
				mPlayerThread.interrupt(); // doesn't seem to work
				mPlayerThread.stopStream();// hack to make it work
				mPlayerThread.join(500);
				if (mPlayerThread.isAlive()) {
					Log.e(TAG, "Serious problem with player thread!");
				}
				mPlayerThread = null;
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private void startVideoPlayback() {
		Log.v(TAG, "startVideoPlayback");
		if (null == mPlayerThread) {
			mPlayerThread = new PlayerThread(mHolder.getSurface());
			String uri = "file:///data/drm/contents/" + mVideoName;
			mPlayerThread.setDataSource(uri);
			mPlayerThread.start();
		}
	}

	private class PlayerThread extends Thread {
		private MediaExtractor mExtractor;
		private MediaCodec[] mMediaCodec;
		private Surface mSurface;
		private String mDataSource;
		MediaCrypto mMediaCrypto;
		private boolean mStopStream = false;
		MediaCodec.CryptoInfo mCryptoInfo = new MediaCodec.CryptoInfo();
		long mStartMs;
		boolean mIsEndOfStream;
		String mPsshData;
		RenderingBuffers[] mBuffers;
		AudioTrack mAudioTrack;
		private final long TIME_OUT_MICRO_SECS = 10000000;

		public PlayerThread(Surface xSurface) {
			this.mSurface = xSurface;
		}

		public void stopStream() {
			this.mStopStream = true;
		}

		@Override
		public void run() {
			if (!this.mDataSource.equals("")) {
				try {
					playDrmContent();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				Log.v(TAG,
						"No Media Uri Call setDataSource() before thread start()");
			}
		}

		public void setDataSource(String xDataSource) {
			this.mDataSource = xDataSource;
			Log.v(TAG, "Media Uri: " + this.mDataSource);
		}

		private void playDrmContent() throws InterruptedException {

			try {
//				mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
//						AudioFormat.CHANNEL_OUT_STEREO,
//						AudioFormat.ENCODING_PCM_16BIT, 8192 * 2,
//						AudioTrack.MODE_STATIC);
				mExtractor = new MediaExtractor();
				mExtractor.setDataSource(mDataSource);
				int numTracks = mExtractor.getTrackCount();
				Log.v(TAG, "Number of tracks: " + numTracks);

				mBuffers = new RenderingBuffers[numTracks];
				mMediaCodec = new MediaCodec[numTracks];
				MediaFormat[] format = new MediaFormat[numTracks];

				for (int i = 0; i < numTracks; ++i) {
					format[i] = mExtractor.getTrackFormat(i);
					String mime = format[i].getString(MediaFormat.KEY_MIME);
					Log.v(TAG, "Track[" + i + "].mime = " + mime);
					if (mime.equals("video/avc")) {
						mExtractor.selectTrack(i);

						mMediaCodec[i] = MediaCodec.createDecoderByType(mime);
						boolean isCryptoSupported = false;
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

									mMediaCrypto = new MediaCrypto(uuid,
											psshInfo.get(uuid));

									isCryptoSupported = true;
									break;

								}
							}
						}

						mMediaCodec[i].configure(format[i], mSurface,
								mMediaCrypto, 0);

						if (!isCryptoSupported) {
							Log.v(TAG, "Unsupported crypto scheme");
							//break;
						}
					}

					if (mime.equals("audio/mp4a-latm")) {
						mMediaCodec[i] = MediaCodec.createDecoderByType(mime);
						mMediaCodec[i].configure(format[i], null, mMediaCrypto,
								0);

					}

				}

				for (int i = 0; i < numTracks; ++i) {
					if (mMediaCodec[i] != null) {
						mMediaCodec[i].start();
						mBuffers[i] = new RenderingBuffers();
						mBuffers[i].mInputBuffers = mMediaCodec[i]
								.getInputBuffers();
						mBuffers[i].mOutputBuffers = mMediaCodec[i]
								.getOutputBuffers();
						mBuffers[i].mBufferInfo = new BufferInfo();
					}
				}

				mIsEndOfStream = false;
				mStartMs = System.currentTimeMillis();

				for (;;) {
					if (isInterrupted()) {
						break;
					}

					if (mStopStream)
						break;

					if (renderVideo(0)) {
						break;
					}
					
					/*if (renderAudio(1)) {
						continue;
					}*/

					
				}

				for (int i = 0; i < numTracks; ++i) {
					
					if(mMediaCodec[i] != null){
						mMediaCodec[i].stop();
						mMediaCodec[i].release();
						mMediaCodec[i] = null;
					}
					
					if(mMediaCodec[i] != null){
						mBuffers[i] = null;
					}
				}
				
				mExtractor.release();
				mMediaCodec = null;
				mExtractor = null;
				mBuffers = null;

			} catch (IOException e) {
				e.printStackTrace();
			} catch (MediaCryptoException e) {
				Log.e(TAG, "Could not instanciate MediaCrypto ! " + e);
			}

		}

		private void inputBuffer(int xtrack) {

			mExtractor.selectTrack(xtrack);
			int inputBufferIndex = mMediaCodec[xtrack]
					.dequeueInputBuffer(TIME_OUT_MICRO_SECS);
			if (inputBufferIndex >= 0) {
				ByteBuffer buffer = mBuffers[xtrack].mInputBuffers[inputBufferIndex];
				int sampleSize = mExtractor.readSampleData(buffer, 0);
				Log.v(TAG,
						"Read data from extractor (and crypto) and provide to decoder\n");
				if (sampleSize < 0) {
					Log.v(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM\n");
					mMediaCodec[xtrack].queueInputBuffer(inputBufferIndex, 0,
							0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
				} else {
					mExtractor.getSampleCryptoInfo(mCryptoInfo);

					if (mCryptoInfo.mode == MediaCodec.CRYPTO_MODE_UNENCRYPTED) {
						mMediaCodec[xtrack].queueInputBuffer(inputBufferIndex,
								0, sampleSize, mExtractor.getSampleTime(), 0);
					} else {
						mCryptoInfo.key = mDrmAgent.provideKey(mPsshData);
						
						mMediaCodec[xtrack].queueSecureInputBuffer(
								inputBufferIndex, 0, mCryptoInfo,
								mExtractor.getSampleTime(), mCryptoInfo.mode);
					}
					mExtractor.advance();
				}
			}
		}

		private int outputBuffer(int xtrack) {
			int outIndex = mMediaCodec[xtrack].dequeueOutputBuffer(
					mBuffers[xtrack].mBufferInfo, TIME_OUT_MICRO_SECS);

			switch (outIndex) {
			case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
				Log.v(TAG, "INFO_OUTPUT_BUFFERS_CHANGED\n");
				mBuffers[xtrack].mOutputBuffers = mMediaCodec[xtrack]
						.getOutputBuffers();
				break;
			case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
				Log.v(TAG,
						"INFO_OUTPUT_FORMAT_CHANGED "
								+ mMediaCodec[xtrack].getOutputFormat());
				break;
			case MediaCodec.INFO_TRY_AGAIN_LATER:
				Log.v(TAG, "INFO_TRY_AGAIN_LATER decoder timmed out");
				break;

			}

			return outIndex;
		}

		private boolean renderVideo(int xTrack) {

			inputBuffer(xTrack);
			int outIndex = outputBuffer(xTrack);
			if (outIndex >= 0) {
				ByteBuffer buffer = mBuffers[xTrack].mOutputBuffers[outIndex];
				Log.v(TAG, "Rendering video ....." + buffer);
				while (mBuffers[xTrack].mBufferInfo.presentationTimeUs / 1000 > System
						.currentTimeMillis() - mStartMs) {
					try {
						sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
				}
				mMediaCodec[xTrack].releaseOutputBuffer(outIndex, true);
			}

			// All decoded frames have been rendered
			if ((mBuffers[xTrack].mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
				Log.v(TAG, "outputBuffers BUFFER_FLAG_END_OF_STREAM");
				mIsEndOfStream = true;
			}

			return mIsEndOfStream;
		}

		private boolean renderAudio(int xTrack) {
			inputBuffer(xTrack);
			int outIndex = outputBuffer(xTrack);
			if(outIndex >= 0){
				ByteBuffer buffer = mBuffers[xTrack].mOutputBuffers[outIndex];
				Log.v(TAG, "Rendering audio....." + buffer);
				final byte[] chunk = new byte[mBuffers[xTrack].mBufferInfo.size];
				buffer.get(chunk);
				buffer.clear();
				mAudioTrack.play();
				if(chunk.length > 0){
					mAudioTrack.write(chunk, 0, chunk.length);
				}
				mMediaCodec[xTrack].releaseOutputBuffer(outIndex, false);
			}

			return false;
		}

		private class RenderingBuffers {
			public ByteBuffer[] mInputBuffers;
			public ByteBuffer[] mOutputBuffers;
			public BufferInfo mBufferInfo;
		}

	}
}