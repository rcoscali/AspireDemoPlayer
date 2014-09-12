package com.nagravision.aspiredemoplayer;

import com.nagravision.aspiredemoplayer.drm.DrmAgent;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class AudioPlayer extends Activity {

	private static final String TAG = "MediaPlayerDemo";
	private MediaPlayer mMediaPlayer;
	private static final String MEDIA = "media";
	private Bundle mExtras;
	private String mAudioName;
	private DrmAgent mDrmAgent = null;

	private TextView tx;

	@Override
	public void onCreate(Bundle xBundle) {
		super.onCreate(xBundle);
		tx = new TextView(this);
		setContentView(tx);
		mExtras = getIntent().getExtras();
		mAudioName = (String)mExtras.getSerializable("MEDIANAME");
		playAudio((ImageAdapter.Media) mExtras.getSerializable(MEDIA));
		mDrmAgent = DrmAgent.getInstance();
	}

	private void playAudio(ImageAdapter.Media xMedia) {
		try {
			boolean play = false;
			AssetFileDescriptor afd = null;
			switch (xMedia) {
			case LOCAL_AUDIO:

				afd = getAssets().openFd(mAudioName);
				if (afd == null) {
					// Tell the user to provide an audio file URL.
					Toast.makeText(
							AudioPlayer.this,
							"Please edit ImageAdapter Class, "
									+ "and set the path variable to your audio file path."
									+ " Your audio file must be stored on sdcard.",
							Toast.LENGTH_LONG).show();
				}

				mMediaPlayer = new MediaPlayer();
				mMediaPlayer.setDataSource(afd.getFileDescriptor(),
						afd.getStartOffset(), afd.getLength());

				play = true;
				break;

			case STREAM_AUDIO:

				// mMediaPlayer = MediaPlayer.create(this,
				// R.raw.allegro_from_duet_in_c_major);//allegro_from_duet_in_c_major.mp3
				// mMediaPlayer.start();
				// play = true;
				break;
			default:
				break;

			}

			if (play) {
				mMediaPlayer.prepare();
				mMediaPlayer.start();
			}

			tx.setText("Playing audio...");

		} catch (Exception xExcep) {
			Log.e(TAG, "error: " + xExcep.getMessage(), xExcep);
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// TODO Auto-generated method stub
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
			mMediaPlayer = null;
		}

	}
}
