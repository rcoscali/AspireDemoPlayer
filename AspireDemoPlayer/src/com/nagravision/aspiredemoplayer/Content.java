package com.nagravision.aspiredemoplayer;

import java.io.File;
import java.net.URI;

import com.nagravision.aspiredemoplayer.ImageAdapter.Media;
import com.nagravision.aspiredemoplayer.drm.DrmAgent;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class Content extends Activity {
	private static final String TAG = "Content";
	private GridView mGridView = null;
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
		 * On Click event for Single GridView Item
		 * */
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, 
									View v,
									int position, 
									long id) 
			{
				int itemId = (Integer) mImageAdapter.getItem(position);
				
				if (!mImageAdapter.isLocalItemAccessible(position))
				{
					Toast.makeText(getBaseContext(), "This media is not accessible !", Toast.LENGTH_LONG).show();
				    return;
				}
				
				ImageAdapter.Media itemMedia;
				String itemExt = "", filePath = "", mediaUri = "", contentId = "";
				
				itemMedia = (ImageAdapter.Media) mImageAdapter.getItemMedia(position);
				itemExt = (String) mImageAdapter.getItemExt(position);
				filePath = getResources().getResourceName(itemId);
				contentId = filePath.substring(filePath.lastIndexOf("/") + 1) + itemExt;
				mediaUri = ((URI)mImageAdapter.getItemUri(position)).toASCIIString();
				
				Intent i = null;
				mDrmAgent = DrmAgent.getInstance();
				if (mDrmAgent == null) 
				{
					DrmAgent.gCreateInstance(getApplicationContext());
					mDrmAgent = DrmAgent.getInstance();
				}
				int rightsStatus = mDrmAgent.checkDrmInfoRights(contentId);
				if(0 != rightsStatus){
					// Sending to content purchaser
					i = new Intent(getApplicationContext(), Purchase.class);
					Toast.makeText(
							Content.this,
							"You do not have the rights to view this Content "
									+ "Click the image to get the rights.",
							Toast.LENGTH_LONG).show();
				}
				else if (mImageAdapter.isItemVideo(position)) {
					// Sending video id to VideoPlayer
					i = new Intent(getApplicationContext(), VideoPlayer.class);
				}

				else if (mImageAdapter.isItemAudio(position)) {
					// Sending audio id to AudioPlayer
					i = new Intent(getApplicationContext(), AudioPlayer.class);
				}
				
				else
			      Log.v(TAG, "error: Unknown media location");

				if (i != null) {
					// passing array index
					i.putExtra(VideoPlayer.KEY_MEDIA_NAME, contentId);
					i.putExtra(VideoPlayer.KEY_MEDIA_URI, mediaUri);
					i.putExtra(VideoPlayer.KEY_MEDIA_MEDIA, itemMedia);
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
