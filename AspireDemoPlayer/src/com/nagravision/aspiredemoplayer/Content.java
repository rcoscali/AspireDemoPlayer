package com.nagravision.aspiredemoplayer;

import com.nagravision.aspiredemoplayer.drm.DrmAgent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class Content extends Activity {
	private static final String TAG = "Content";
	private GridView mGridView = null;
	private static final String MEDIA = "media";
	private ImageAdapter mImageAdapter;
	private DrmAgent mDrmAgent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);
		mGridView = (GridView) findViewById(R.id.grid_view);
		mImageAdapter = new ImageAdapter(this);
		mGridView.setAdapter(mImageAdapter);
		mDrmAgent = DrmAgent.getInstance();

		/**
		 * On Click event for Single Gridview Item
		 * */
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				int itemId = (Integer) mImageAdapter.getItem(position);
				ImageAdapter.Media itemMedia = (ImageAdapter.Media) mImageAdapter
						.getItemMedia(position);
				String itemExt = (String) mImageAdapter.getItemExt(position);
				String filePath = getResources().getResourceName(itemId);
				String mediaName = filePath.substring(filePath
						.lastIndexOf("/") + 1)+itemExt;
				Log.v(TAG, "Play request for " + mediaName);
				
				Intent i = null;

				int rightsStatus = mDrmAgent.checkDrmInfoRights(mediaName);
				Log.v(TAG, "Right for " + mediaName + " = " + rightsStatus);
				if(0 != rightsStatus){
					// Sending to content purchaser
					i = new Intent(getApplicationContext(), Purchase.class);
					Toast.makeText(
							Content.this,
							"You do ot have the rights to view this Content "
									+ "Click the image to get the rights.",
							Toast.LENGTH_LONG).show();
				}
				else if (itemMedia.equals(ImageAdapter.Media.LOCAL_VIDEO)
						|| itemMedia.equals(ImageAdapter.Media.STREAM_VIDEO)
						|| itemMedia.equals(ImageAdapter.Media.DRM_LOCAL_VIDEO)
						|| itemMedia.equals(ImageAdapter.Media.DRM_STREAM_VIDEO)
								) {
					// Sending video id to VideoPlayer
					i = new Intent(getApplicationContext(), VideoPlayer.class);
				}

				else if (itemMedia.equals(ImageAdapter.Media.LOCAL_AUDIO)
						|| itemMedia.equals(ImageAdapter.Media.STREAM_AUDIO)
						|| itemMedia.equals(ImageAdapter.Media.DRM_STREAM_AUDIO)
						|| itemMedia.equals(ImageAdapter.Media.DRM_LOCAL_AUDIO)
						) {
					// Sending audio id to AudioPlayer
					i = new Intent(getApplicationContext(), AudioPlayer.class);
				} else {
					Log.v(TAG, "error: Unknown media location");
				}

				if (i != null) {				
					// passing array index
					i.putExtra("MEDIANAME", mediaName);
					i.putExtra(MEDIA, itemMedia);
					i.putExtra("position", position);
					startActivity(i);
				}
			}
		});
	}
	
	@Override
	protected void onResume(){
		  super.onResume();
		  mGridView.setVisibility(View.VISIBLE);
	  }
   
	@Override
	protected void  onPause(){
		  super. onPause();
		  mGridView.setVisibility(View.INVISIBLE);
	  }
}
