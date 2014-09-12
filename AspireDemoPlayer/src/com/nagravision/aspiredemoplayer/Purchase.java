package com.nagravision.aspiredemoplayer;

import com.nagravision.aspiredemoplayer.drm.DrmAgent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class Purchase extends Activity {
	private ImageAdapter mImageAdapter;
	private int mPosition;
	private DrmAgent mDrmAgent;
	private static final String MEDIA = "media";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.purchase);

		// get intent data
		Intent i = getIntent();

		// Selected image id
		mPosition = i.getExtras().getInt("position");

		mImageAdapter = new ImageAdapter(this);
		ImageView imageView = (ImageView) findViewById(R.id.purchaseImage);
		int itemId = (Integer) mImageAdapter.getItem(mPosition);
		imageView.setImageResource(itemId);
		imageView.setOnClickListener((android.view.View.OnClickListener) mPurchaseListener);

		mDrmAgent = DrmAgent.getInstance();
	}

	private OnClickListener mPurchaseListener = new OnClickListener() {
		public void onClick(View v) {

			String itemMime = (String) mImageAdapter.getItemMime(mPosition);
			String itemExt = (String) mImageAdapter.getItemExt(mPosition);
			int itemId = (Integer) mImageAdapter.getItem(mPosition);

			String filePath = getResources().getResourceName(itemId);
			String mediaName = filePath
					.substring(filePath.lastIndexOf("/") + 1) + itemExt;

			/*String contentRights = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/contentrights.rights";
			String mediaLocation = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + "/" + mediaName;*/
			
			String contentRights = "/data/drm/nagravision/contentrights.rights";
			String mediaLocation = "/data/drm/contents/"+mediaName;

			if (!mDrmAgent.acquireDrmInfoRights(mediaLocation, contentRights,
					itemMime)) {
				Toast.makeText(
						Purchase.this,
						"You do not have the required rights to play this video",
						Toast.LENGTH_LONG).show();
			} else {
				ImageAdapter.Media itemMedia = (ImageAdapter.Media) mImageAdapter
						.getItemMedia(mPosition);
				Intent intent = null;

				if (itemMedia.equals(ImageAdapter.Media.LOCAL_VIDEO)
						|| itemMedia.equals(ImageAdapter.Media.STREAM_VIDEO)
						|| itemMedia.equals(ImageAdapter.Media.DRM_LOCAL_VIDEO)
						|| itemMedia.equals(ImageAdapter.Media.DRM_STREAM_VIDEO)) {
					// Sending video id to VideoPlayer
					intent = new Intent(getApplicationContext(),
							VideoPlayer.class);
				}

				else if (itemMedia.equals(ImageAdapter.Media.LOCAL_AUDIO)
						|| itemMedia.equals(ImageAdapter.Media.STREAM_AUDIO)
						|| itemMedia.equals(ImageAdapter.Media.DRM_LOCAL_AUDIO)
						|| itemMedia.equals(ImageAdapter.Media.DRM_STREAM_AUDIO)) {
					// Sending audio id to AudioPlayer
					intent = new Intent(getApplicationContext(),
							AudioPlayer.class);
				}

				if (null != intent) {
					intent.putExtra("MEDIANAME", mediaName);
					intent.putExtra(MEDIA, itemMedia);
					startActivity(intent);
				}

			}

		}
	};
	
	@Override
	protected void  onPause(){
		  super. onPause();
		  finish();
	  }

}