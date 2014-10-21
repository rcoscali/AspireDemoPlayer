package com.nagravision.aspiredemoplayer;

import java.io.File;
import java.net.URI;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter {

	private Context mContext;

	private PlayerObject[] mThumbIds = 
	{
	    /* Avatar Stream */
	    new PlayerObject(R.drawable.avatar, Media.STREAM_VIDEO, "video/mp4",false, URI.create("http://home.citycable.ch/rcoscali/Aspire/assets/avatar.mp4")),
	    new PlayerObject(R.drawable.avatar_cenc, Media.DRM_STREAM_VIDEO, "video/mp4",true, URI.create("http://home.citycable.ch/rcoscali/Aspire/assets/avatar_cenc.mp4")),
	    new PlayerObject(R.drawable.avatar_cenc, Media.DRM_STREAM_VIDEO, "video/mp4",true, URI.create("http://home.citycable.ch/rcoscali/Aspire/assets/avatar_cenc2.mp4")),
	    new PlayerObject(R.drawable.avatar_cenc, Media.DRM_STREAM_VIDEO, "video/mp4",true, URI.create("http://home.citycable.ch/rcoscali/Aspire/assets/avatar_cenc3.mp4")),
		    
	    /* Avatar Local */
	    new PlayerObject(R.drawable.avatar, Media.LOCAL_VIDEO, "video/mp4",false),
	    new PlayerObject(R.drawable.avatar_cenc, Media.DRM_LOCAL_VIDEO, "video/mp4",true),
		    
	    /* Oblivion Stream */
	    new PlayerObject(R.drawable.oblivion, Media.STREAM_VIDEO, "video/mp4",false, URI.create("http://home.citycable.ch/rcoscali/Aspire/assets/oblivion.mp4")),
	    new PlayerObject(R.drawable.oblivion_cenc, Media.DRM_STREAM_VIDEO, "video/mp4",true, URI.create("http://home.citycable.ch/rcoscali/Aspire/assets/oblivion_cenc.mp4")),
	    new PlayerObject(R.drawable.oblivion_cenc, Media.DRM_STREAM_VIDEO, "video/mp4",true, URI.create("https://onedrive.live.com/download.aspx?cid=4076E5A51C407C83&resid=4076E5A51C407C83%21191&canary=DAtQGKRlJVJ05toCXET5CL%2FdoywBt2%2F95wF2xXRXF0Y%3D0")),
	    /* Oblivion Local */
	    new PlayerObject(R.drawable.oblivion, Media.LOCAL_VIDEO, "video/mp4",false),
	    new PlayerObject(R.drawable.oblivion_cenc, Media.DRM_LOCAL_VIDEO, "video/mp4",true),

	    /* SherlockHolmes Stream */
	    new PlayerObject(R.drawable.sherlockholmes, Media.STREAM_VIDEO, "video/mp4",false, URI.create("http://home.citycable.ch/rcoscali/Aspire/assets/sherlockholmes.mp4")),
	    new PlayerObject(R.drawable.sherlockholmes_cenc, Media.DRM_STREAM_VIDEO,	"video/mp4",true, URI.create("http://home.citycable.ch/rcoscali/Aspire/assets/sherlockholmes_cenc.mp4")),
	    
	    /* SherlockHolmes Local */
	    new PlayerObject(R.drawable.sherlockholmes, Media.LOCAL_VIDEO, "video/mp4",false),
	    new PlayerObject(R.drawable.sherlockholmes_cenc, Media.DRM_LOCAL_VIDEO,	"video/mp4",true),
	    
	    /* Batman Local */
	    new PlayerObject(R.drawable.thedarkknightrises, Media.LOCAL_VIDEO, "video/mp4",false),
	    new PlayerObject(R.drawable.thedarkknightrises_cenc, Media.DRM_LOCAL_VIDEO, "video/mp4",true),
	    
	    /* TheGrey Local */
	    new PlayerObject(R.drawable.thegrey, Media.LOCAL_VIDEO, "video/mp4",false),
	    new PlayerObject(R.drawable.thegrey_cenc, Media.DRM_LOCAL_VIDEO, "video/mp4",true),
	    
	    /* Thor Local */
	    new PlayerObject(R.drawable.thor, Media.LOCAL_VIDEO, "video/mp4",false),
	    new PlayerObject(R.drawable.thor_cenc, Media.DRM_LOCAL_VIDEO, "video/mp4",true),
	    
	    /* Venoms Lab 2 Stream */
	    new PlayerObject(R.drawable.venomslab2_teaser_1080, Media.STREAM_VIDEO, "video/mp4",false, URI.create("http://home.citycable.ch/rcoscali2/Videos/venomslab2_teaser_1080.mp4")),
	    new PlayerObject(R.drawable.venomslab2_teaser_1080_cenc, Media.DRM_STREAM_VIDEO, "video/mp4",true, URI.create("http://home.citycable.ch/rcoscali2/Videos/venomslab2_teaser_1080_cenc.mp4")),	    
	    
	    /* Sintel Stream */
	    new PlayerObject(R.drawable.sintel_poster, Media.STREAM_VIDEO, "video/mp4",false, URI.create("http://mirrorblender.top-ix.org/movies/sintel-1024-stereo.mp4")),
	    new PlayerObject(R.drawable.sintel_poster, Media.DRM_STREAM_VIDEO, "video/mp4",true, URI.create("https://www.dropbox.com/s/1ogirag33q5f7ff/sintel-1024-stereo_cenc.mp4?dl=1")),	    

	    /* Tears Of Steel Stream */
	    new PlayerObject(R.drawable.tears_of_steel, Media.STREAM_VIDEO, "video/mp4",false, URI.create("https://onedrive.live.com/download.aspx?cid=4076E5A51C407C83&resid=4076E5A51C407C83%21151&authkey=%21ALCGbdGV_Je9wT0&canary=DAtQGKRlJVJ05toCXET5CL%2FdoywBt2%2F95wF2xXRXF0Y%3D1")),
	    new PlayerObject(R.drawable.tears_of_steel, Media.DRM_STREAM_VIDEO, "video/mp4",true, URI.create("https://onedrive.live.com/download.aspx?cid=4076E5A51C407C83&resid=4076E5A51C407C83%21155&authkey=%21AFalpkr36hmdsGs&canary=DAtQGKRlJVJ05toCXET5CL%2FdoywBt2%2F95wF2xXRXF0Y%3D9")),	    

	    /* Gran Dillama Stream */
	    new PlayerObject(R.drawable.gran_dillama, Media.STREAM_VIDEO, "video/mp4",false, URI.create("https://www.dropbox.com/s/xiecigq414bnv9c/02_gran_dillama_1080p.mp4?dl=1")),
	    new PlayerObject(R.drawable.gran_dillama, Media.DRM_STREAM_VIDEO, "video/mp4",true, URI.create("https://www.dropbox.com/s/5jeqdzfkdekchhl/02_gran_dillama_1080p_cenc.mp4?dl=1")),	    

	    /* Beethoven Local */
	    new PlayerObject(R.drawable.beethoven, Media.LOCAL_AUDIO, "audio/mp3",false), 
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

	public Object getItemUri(int position) {
		if (isItemLocal(position) && mThumbIds[position].mUri == null)
		{
			int itemId = mThumbIds[position].mID;
			String filePath = mContext.getResources().getResourceName(itemId);
			String fileUri = "file://" +
					  mContext.getExternalFilesDir(null).getAbsolutePath() + 
					  "/contents/" + 
					  filePath.substring(filePath.lastIndexOf("/") + 1) + 
					  mThumbIds[position].mExt;
			mThumbIds[position].mUri = URI.create(fileUri);
		}
		return mThumbIds[position].mUri;
	}
	
	public boolean isLocalItemAccessible(int position) {
		if (isItemLocal(position))
			return (new File(((URI)getItemUri(position)).getPath())).canRead();
		return true;
	}

	public long getItemId(int position) {
		return mThumbIds[position].mID;
	}

	public boolean isItemLocal(int position) {
		return (mThumbIds[position].mMedia.equals(Media.LOCAL_AUDIO) ||
				mThumbIds[position].mMedia.equals(Media.LOCAL_VIDEO) ||
				mThumbIds[position].mMedia.equals(Media.DRM_LOCAL_AUDIO) ||
				mThumbIds[position].mMedia.equals(Media.DRM_LOCAL_VIDEO));
	}

	public boolean isItemStream(int position) {
		return ! isItemLocal(position);
	}
	
	public boolean isItemAudio(int position) {
		return (mThumbIds[position].mMedia.equals(Media.LOCAL_AUDIO) ||
				mThumbIds[position].mMedia.equals(Media.STREAM_AUDIO) ||
				mThumbIds[position].mMedia.equals(Media.DRM_LOCAL_AUDIO) ||
				mThumbIds[position].mMedia.equals(Media.DRM_STREAM_AUDIO));
	}

	public boolean isItemVideo(int position) {
		return ! isItemAudio(position);
	}

	public ImageAdapter(Context c) {
		mContext = c;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView;
		if (convertView == null) 
		{
			imageView = new ImageView(mContext);
			imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imageView.setPadding(8, 8, 8, 8);
		}
		else 
			imageView = (ImageView) convertView;

		imageView.setImageResource(mThumbIds[position].mID);
		if(mThumbIds[position].mEncrypted)
		{
			if (ImageAdapter.this.isItemLocal(position))
				imageView.setBackgroundColor(isLocalItemAccessible(position) ? Color.RED : Color.parseColor("#AAAAAA"));
			else
				imageView.setBackgroundColor(Color.parseColor("#FFAAAA"));
		}
		else
		{
			if (ImageAdapter.this.isItemLocal(position))
				imageView.setBackgroundColor(isLocalItemAccessible(position) ? Color.GREEN : Color.parseColor("#AAAAAA"));
			else
				imageView.setBackgroundColor(Color.parseColor("#AAFFAA"));
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
		DRM_STREAM_VIDEO,
		INVALID
	}

	class PlayerObject {

		private int mID;
		private Media mMedia;
		private String mMime;
		private String mExt;
		private boolean mEncrypted;
		private URI mUri;

	    public PlayerObject(int xId, Media xMedia, String xMime, boolean xEnc) {
			this.mID = xId;
			this.mMedia = xMedia;
			this.mMime = xMime;
			this.mExt = "." + this.mMime.substring(this.mMime.lastIndexOf("/") + 1);
			this.mEncrypted = xEnc;
			this.mUri = null;
		}

	    public PlayerObject(int xId, Media xMedia, String xMime, boolean xEnc, URI xUri) {
			this.mID = xId;
			this.mMedia = xMedia;
			this.mMime = xMime;
			this.mExt = "." + this.mMime.substring(this.mMime.lastIndexOf("/") + 1);
			this.mEncrypted = xEnc;
			this.mUri = xUri;
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

		public URI getUri() {
			return mUri;
		}

	}

}
