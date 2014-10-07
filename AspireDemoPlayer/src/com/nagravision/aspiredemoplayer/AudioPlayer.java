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
	private String mAudioUri;
	private DrmAgent mDrmAgent = null;

	private TextView tx;

	@Override
	public void onCreate(Bundle xBundle) {
		super.onCreate(xBundle);
		tx = new TextView(this);
		setContentView(tx);
		mExtras = getIntent().getExtras();
		mAudioName = (String)mExtras.getSerializable("MEDIANAME");
		mAudioUri = (String)mExtras.getSerializable("MEDIAURI");
		playAudio((ImageAdapter.Media) mExtras.getSerializable(MEDIA));
		mDrmAgent = DrmAgent.getInstance();
	}

	private void playAudio(ImageAdapter.Media xMedia) {
		try {
			boolean play = false;

			switch (xMedia) {

			case LOCAL_AUDIO:
			case STREAM_AUDIO:

				mMediaPlayer = new MediaPlayer();
				mMediaPlayer.setDataSource(mAudioUri);

				play = true;
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
