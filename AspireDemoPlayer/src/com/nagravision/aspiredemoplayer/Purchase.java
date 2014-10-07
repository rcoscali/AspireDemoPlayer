package com.nagravision.aspiredemoplayer;

import java.net.URI;

import com.nagravision.aspiredemoplayer.drm.DrmAgent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
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
		mDrmAgent.setDatabasePath(this.getExternalFilesDir(null).getAbsolutePath());
	}

	private OnClickListener mPurchaseListener = new OnClickListener() {
		public void onClick(View v) {

			String itemMime = (String) mImageAdapter.getItemMime(mPosition);
			String itemExt = (String) mImageAdapter.getItemExt(mPosition);
			int itemId = (Integer) mImageAdapter.getItem(mPosition);
			String filePath = getResources().getResourceName(itemId);
			String contentId = filePath.substring(filePath.lastIndexOf("/") + 1) + itemExt;
			String mediaUri = (((URI)mImageAdapter.getItemUri(mPosition))).toASCIIString();

			if (!mDrmAgent.acquireDrmInfoRights(contentId, itemMime)) {
				Toast.makeText(
						Purchase.this,
						"You do not have the required rights to play this video",
						Toast.LENGTH_LONG).show();
			}
			else {
				Intent intent = null;
				Toast.makeText(
						Purchase.this,
						"You just purchased " + contentId + ". Congrat !!",
						Toast.LENGTH_LONG).show();
				if (mImageAdapter.isItemVideo(mPosition)) {
					// Sending video id to VideoPlayer
					intent = new Intent(getApplicationContext(), VideoPlayer.class);
				}
				else if (mImageAdapter.isItemAudio(mPosition)) {
					// Sending audio id to AudioPlayer
					intent = new Intent(getApplicationContext(), AudioPlayer.class);
				}

				if (null != intent) {
					intent.putExtra("MEDIANAME", contentId);
					intent.putExtra("MEDIAURI", mediaUri);
					intent.putExtra(MEDIA, (ImageAdapter.Media) mImageAdapter.getItemMedia(mPosition));
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