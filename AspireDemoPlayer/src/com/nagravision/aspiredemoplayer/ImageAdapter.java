package com.nagravision.aspiredemoplayer;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter {

	private Context mContext;

	private PlayerObject[] mThumbIds = {
			new PlayerObject(R.drawable.avatar, Media.DRM_LOCAL_VIDEO, "video/mp4",false),
			new PlayerObject(R.drawable.oblivion, Media.LOCAL_VIDEO, "video/mp4",false),
			new PlayerObject(R.drawable.sherlockholmes, Media.LOCAL_VIDEO, "video/mp4",false),
			new PlayerObject(R.drawable.thedarkknightrises_cenc, Media.LOCAL_VIDEO, "video/mp4",true),
			new PlayerObject(R.drawable.thegrey, Media.LOCAL_VIDEO, "video/mp4",false),
			new PlayerObject(R.drawable.beethoven, Media.LOCAL_AUDIO, "audio/mp3",false), 
			new PlayerObject(R.drawable.oblivion_cenc, Media.DRM_LOCAL_VIDEO, "video/mp4",true),
			new PlayerObject(R.drawable.avatar, Media.DRM_LOCAL_VIDEO, "video/mp4",false),
			new PlayerObject(R.drawable.oblivion, Media.LOCAL_VIDEO, "video/mp4",false),
			new PlayerObject(R.drawable.sherlockholmes_cenc, Media.LOCAL_VIDEO, "video/mp4",true),
			new PlayerObject(R.drawable.thedarkknightrises, Media.LOCAL_VIDEO, "video/mp4",false),
			new PlayerObject(R.drawable.avatar_cenc, Media.LOCAL_VIDEO, "video/mp4",true),
			new PlayerObject(R.drawable.beethoven, Media.LOCAL_AUDIO, "audio/mp3",false), 
			new PlayerObject(R.drawable.oblivion_cenc, Media.DRM_LOCAL_VIDEO, "video/mp4",true),
			new PlayerObject(R.drawable.thegrey_cenc, Media.DRM_LOCAL_VIDEO, "video/mp4",true)
			};
	
	public int getCount() {
		return mThumbIds.length;
	}

	public Object getItem(int position) {
		return mThumbIds[position].mID;
	}

	public Object getItemMedia(int position) {
		return mThumbIds[position].mMedia;
	}

	public Object getItemMime(int position) {
		return mThumbIds[position].mMime;
	}

	public Object getItemExt(int position) {
		return mThumbIds[position].mExt;
	}

	public long getItemId(int position) {
		return 0;
	}

	public ImageAdapter(Context c) {
		mContext = c;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView;
		if (convertView == null) {
			imageView = new ImageView(mContext);
			imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imageView.setPadding(8, 8, 8, 8);
		} else {
			imageView = (ImageView) convertView;
		}
		imageView.setImageResource(mThumbIds[position].mID);
		if(mThumbIds[position].mEncrypted){
		    imageView.setBackgroundColor(Color.RED);
		}
		return imageView;
	}

	public enum Media {
		LOCAL_AUDIO, 
		STREAM_AUDIO, 
		LOCAL_VIDEO, 
		STREAM_VIDEO,
		DRM_LOCAL_AUDIO, 
		DRM_STREAM_AUDIO, 
		DRM_LOCAL_VIDEO, 
		DRM_STREAM_VIDEO
	}

	class PlayerObject {

		private int mID;
		private Media mMedia;
		private String mMime;
		private String mExt;
		private boolean mEncrypted;

		public PlayerObject(int xId, Media xMedia, String xMime, boolean xEnc) {
			this.mID = xId;
			this.mMedia = xMedia;
			this.mMime = xMime;
			this.mExt = "."
					+ this.mMime.substring(this.mMime.lastIndexOf("/") + 1);
			this.mEncrypted = xEnc;
		}

		public int getID() {
			return mID;
		}

		public Media getMedia() {
			return mMedia;
		}

		public String getMime() {
			return mMime;
		}

		public String getExt() {
			return mExt;
		}

	}

}