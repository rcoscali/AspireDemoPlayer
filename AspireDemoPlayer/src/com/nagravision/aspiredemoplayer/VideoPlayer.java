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
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Rect;
import android.graphics.Canvas;

public class VideoPlayer extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = "VideoPlayer";
    private SurfaceView mPreview;
    private SurfaceHolder mHolder;
    private Bundle mExtras;
    private static final String MEDIA = "media";
    private String mVideoName = "";
    private String mVideoUri = "";
    private DrmAgent mDrmAgent = null;
    private MediaMetadataRetriever mMetadataRetriever = null;
    private int mWidth = 0;
    private int mHeight = 0;
    
    MediaExtractor mExtractor = null;
    PlayerThread mPlayerThread = null;

    public enum Media {
        AUDIO, VIDEO
    }

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
        mVideoUri = (String) mExtras.getSerializable("MEDIAURI");
        mExtractor = new MediaExtractor();
        mMetadataRetriever = new MediaMetadataRetriever();
        mMetadataRetriever.setDataSource(VideoPlayer.this.getBaseContext(), URI.create(mVideoUri));
        mDrmAgent = DrmAgent.getInstance();
    }

    private void playVideo(ImageAdapter.Media xMedia) {
        try {
            switch (xMedia) {

            case LOCAL_VIDEO:
            case DRM_LOCAL_VIDEO:
            case STREAM_VIDEO:
            case DRM_STREAM_VIDEO:
                mWidth = mMetadataRetriever.extractMetadata(mMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                mHeight = mMetadataRetriever.extractMetadata(mMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                startVideoPlayback();
                break;

            case LOCAL_AUDIO:
            case DRM_LOCAL_AUDIO:
            case STREAM_AUDIO:
            case DRM_STREAM_AUDIO:
                startAudioPlayback();
                break;

            default:
                Log.e(TAG, "Unknown media type (" + xMedia + ") !!");
                break;
            }

        } catch (Exception xExcep) {
            Log.e(TAG, "error: " + xExcep.getMessage(), xExcep);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
        Log.d(TAG, "surfaceChanged called");

        Rect theRect = this.mHolder.getSurfaceFrame(); 
        this.mHolder.setFixedSize(Rect.width(), (mHeight * Rect.width()) / mWidth);
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
                mPlayerThread.join(1000); // wait a second
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
            mPlayerThread.setDataSource(mVideoUri);
            try {
                mPlayerThread.renderAudioVideo();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void startAudioPlayback() {
        Log.v(TAG, "startVideoPlayback");
        if (null == mPlayerThread) {
            // mHolder.getSurface()
            VideoPlayer.this.getBaseContext().getResources().getDrawable(R.drawable.audio_player_background).draw(mHolder.lockCanvas());
            mHolder.unlockCanvasAndPost();
            mPlayerThread = new PlayerThread(null);
            mPlayerThread.setDataSource(mVideoUri);
            try {
                mPlayerThread.renderAudioVideo();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class PlayerThread extends Thread {
        MediaExtractor mExtractor;
        private Surface mSurface;
        private String mDataSource;
        private boolean mStopStream = false;
        MediaCodec.CryptoInfo mCryptoInfo = new MediaCodec.CryptoInfo();
        long mStartMs;
        String mPsshData;
        AudioTrack mAudioTrack;
        private final long TIME_OUT_MICRO_SECS = 1000000;
        SparseArray<RenderObject> mRenderObjects = new SparseArray<RenderObject>();

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
                    renderAudioVideo();

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

        /**
         * @brief
         * 
         *        Make a render object for each track
         * 
         */
        private void createRenderObjects() {
            try {
                // set up main extractor
                mExtractor = new MediaExtractor();
                mExtractor.setDataSource(mDataSource);
                int numTracks = mExtractor.getTrackCount();
                Log.v(TAG, "Number of tracks: " + numTracks);

                for (int i = 0; i < numTracks; i++) {
                    MediaFormat format = mExtractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    Log.v(TAG, "Track[" + i + "].mime = " + mime);

                    RenderObject renderObject = null;
                    if (mime.equals("video/avc")) {
                        renderObject = new VideoRenderObject();
                    } else if (mime.equals("audio/mp4a-latm")) {
                        renderObject = new AudioRenderObject();
                    }

                    this.mExtractor.selectTrack(i);

                    if (null != renderObject) {
                        renderObject.mMediaExtractor = mExtractor;
                        renderObject.mRenderingBuffers = new RenderingBuffers();
                        renderObject.mFormat = format;
                        renderObject.mTrackId = i;
                        renderObject.initializeObject(mime);
                        mRenderObjects.put(i, renderObject);
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void renderAudioVideo() throws InterruptedException {

            // create on render object per track in the data source
            createRenderObjects();

            int numTracks = 1;//mRenderObjects.size();

            for (int i = 0; i < numTracks; i++) {

                // get a tracks render object
                RenderObject renderObject = (RenderObject) mRenderObjects
                    .get(mRenderObjects.keyAt(i));

                // start the decoder for the track
                renderObject.startDecoder();

                // start the audio and video render thread
                if (renderObject.mMedia == Media.VIDEO) {
                    renderObject.mRenderThread = new VideoPlayerThread(this,
                                                                       renderObject);
                    new Thread(renderObject.mRenderThread, "Video Thread")
                        .start();
                } else if (renderObject.mMedia == Media.AUDIO) {
                    renderObject.mRenderThread = new AudioPlayerThread(this,
                                                                       renderObject);
                    new Thread(renderObject.mRenderThread, "Audio Thread")
                        .start();
                }
            }

            mStartMs = System.currentTimeMillis();

            for (int i = 0; i < numTracks; i++) {
                // get a tracks render object
                RenderObject renderObject = (RenderObject) mRenderObjects
                    .get(mRenderObjects.keyAt(i));
                renderObject.mRenderThread.startThread();
            }

            // select the first track
            int track = 0;
            this.mExtractor.selectTrack(track);

            for (;;) {
                if (isInterrupted()) {
                    break;
                }

                if (mStopStream) {
                    for (int i = 0; i < numTracks; i++) {
                        // get a tracks render object
                        RenderObject renderObject = (RenderObject) mRenderObjects
                            .get(mRenderObjects.keyAt(i));
                        renderObject.mRenderThread.stopThread();
                    }
                    break;
                }

                boolean isStopped = true;
                for (int i = 0; i < numTracks; i++) {
                    // get a tracks render object
                    RenderObject renderObject = (RenderObject) mRenderObjects
                        .get(mRenderObjects.keyAt(i));
                    isStopped = isStopped
                        && renderObject.mRenderThread.isStopped();
                }

                if (isStopped) {
                    break;
                }

                RenderObject renderObject = (RenderObject) mRenderObjects
                    .get(mRenderObjects.keyAt(track));

                track = enqueue(renderObject);


            }

            releaseAll();
        }

        private void releaseAll() {
            for (int i = 0; i < mRenderObjects.size(); ++i) {

                RenderObject renderObject = (RenderObject) mRenderObjects
                    .get(mRenderObjects.keyAt(i));
                renderObject.startDecoder();

                if (renderObject.mMediaCodec != null) {
                    renderObject.mMediaCodec.stop();
                    renderObject.mMediaCodec.release();
                    renderObject.mMediaCodec = null;
                }

                if (renderObject.mRenderingBuffers != null) {
                    renderObject.mRenderingBuffers = null;
                }

                renderObject.mRenderingBuffers = null;
                renderObject.mMediaExtractor.release();
                renderObject.mMediaExtractor = null;
                renderObject = null;
            }

            mRenderObjects.clear();
        }

        /**
         * @brief enqueue data
         * 
         * @param xRenderObject
         *            The render object to enqueue to
         * 
         * @return The next track
         */
        public int enqueue(RenderObject xRenderObject) {

            if (xRenderObject.mTrackId == mExtractor.getSampleTrackIndex()) {
                int inputBufferIndex = xRenderObject.mMediaCodec
                    .dequeueInputBuffer(TIME_OUT_MICRO_SECS);
                if (inputBufferIndex >= 0) {
                    ByteBuffer buffer = xRenderObject.mRenderingBuffers.mInputBuffers[inputBufferIndex];
                    buffer.clear();
                    // xRenderObject.mOffset = buffer.position();
                    int sampleSize = mExtractor.readSampleData(buffer,
                                                               xRenderObject.mOffset);
                    // buffer.position(xRenderObject.mOffset + sampleSize);

                    Log.v(TAG, "Read data from extractor (and crypto) track "
                          + xRenderObject.mTrackId
                          + " and provide to decoder sample size "
                          + sampleSize + "\n");
                    if (sampleSize < 0) {
                        Log.v(TAG,
                              "Enqueue InputBuffer BUFFER_FLAG_END_OF_STREAM\n");
                        xRenderObject.mMediaCodec.queueInputBuffer(
                                                                   inputBufferIndex, 0, 0, 0,
                                                                   MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        mExtractor.getSampleCryptoInfo(mCryptoInfo);
                        if (mCryptoInfo.mode == MediaCodec.CRYPTO_MODE_UNENCRYPTED) {
                            Log.v(TAG, "Enqueue clear data .....");
                            xRenderObject.mMediaCodec.queueInputBuffer(
                                                                       inputBufferIndex, 0, sampleSize,
                                                                       mExtractor.getSampleTime(), 0);
                            Log.v(TAG, "..... clear data enqueued");
                        } else {
                            Log.v(TAG, "Enqueue encrypted data .....");
                            mCryptoInfo.key = mDrmAgent.provideKey(mPsshData);

                            xRenderObject.mMediaCodec.queueSecureInputBuffer(
                                                                             inputBufferIndex, 0, mCryptoInfo,
                                                                             mExtractor.getSampleTime(),
                                                                             mCryptoInfo.mode);
                            Log.v(TAG, "Encrypted data enqueued");
                        }
                        if (!mExtractor.advance()) {
                            Log.v(TAG, "No more samples\n");
                        }
                    }
                }
            }

            return mExtractor.getSampleTrackIndex();
        }

        private class RenderingBuffers {
            public ByteBuffer[] mInputBuffers;
            public ByteBuffer[] mOutputBuffers;
            public BufferInfo mBufferInfo;
        }

        private abstract class RenderObject {
            public MediaExtractor mMediaExtractor;
            public MediaCodec mMediaCodec;
            public MediaCrypto mMediaCrypto;
            public RenderingBuffers mRenderingBuffers;
            public MediaFormat mFormat;
            public int mTrackId;
            protected MediaPlayerThread mRenderThread;
            protected Media mMedia;
            public int mOffset;

            public abstract void initializeObject(String xMime) {
                if (!checkCryptoSupport())
                    Log.v(TAG, "Unsupported crypto scheme on track " + mTrackId);

                mMediaCodec = MediaCodec.createDecoderByType(xMime);
                mMediaCodec.configure(mFormat, mSurface, mMediaCrypto, 0);
            }

            public abstract int dequeue();

            public abstract boolean render(int xIndex);

            public void startDecoder() {
                if (mMediaCodec != null && mRenderingBuffers != null) {
                    mMediaCodec.start();
                    mRenderingBuffers.mInputBuffers = mMediaCodec
                        .getInputBuffers();
                    mRenderingBuffers.mOutputBuffers = mMediaCodec
                        .getOutputBuffers();
                    mRenderingBuffers.mBufferInfo = new BufferInfo();
                }
            }

            protected boolean checkCryptoSupport() {
                boolean isCryptoSupported = false;
                Map<UUID, byte[]> psshInfo = mMediaExtractor.getPsshInfo();
                if (null != psshInfo) {
                    for (Iterator<UUID> it = psshInfo.keySet().iterator(); it
                             .hasNext();) {
                        UUID uuid = it.next();
                        if (MediaCrypto.isCryptoSchemeSupported(uuid)) {
                            Log.v(TAG, "Supported crypto scheme");
                            mPsshData = new String(psshInfo.get(uuid));
                            Log.v(TAG, "PSSH for UUID: " + uuid.toString()
                                  + " has data :" + mPsshData);

                            try {
                                mMediaCrypto = new MediaCrypto(uuid,
                                                               psshInfo.get(uuid));
                            } catch (MediaCryptoException e) {
                                e.printStackTrace();
                            }
                            isCryptoSupported = true;
                        }
                    }
                }
                return isCryptoSupported;
            }
        }

        private class VideoRenderObject extends RenderObject {
            public void initializeObject(String xMime) {
                super(xMime);
                mMedia = Media.VIDEO;
            }

            @Override
            public int dequeue() {
                //Log.v(TAG, "Dequeue data from track " + mTrackId);
                int outIndex = mMediaCodec.dequeueOutputBuffer(mRenderingBuffers.mBufferInfo, TIME_OUT_MICRO_SECS);
                //Log.v(TAG, ".... data dequeued");

                switch (outIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    //Log.v(TAG, "INFO_OUTPUT_BUFFERS_CHANGED track " + mTrackId);
                    mRenderingBuffers.mOutputBuffers = mMediaCodec
                        .getOutputBuffers();
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    //Log.v(TAG,
                    //      "INFO_OUTPUT_FORMAT_CHANGED "
                    //      + mMediaCodec.getOutputFormat() + "track "
                    //      + mTrackId);
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    //Log.v(TAG,
                    //      "INFO_TRY_AGAIN_LATER decoder timmed out on track "
                    //      + mTrackId);
                    break;
                }

                return outIndex;
            }

            @Override
            public boolean render(int xIndex) {
                boolean done = false;
                if (xIndex >= 0) {
                    ByteBuffer buffer = mRenderingBuffers.mOutputBuffers[xIndex];
                    Log.v(TAG, "Rendering video track " + mTrackId);
                    while (mRenderingBuffers.mBufferInfo.presentationTimeUs / 1000 > System
                           .currentTimeMillis() - mStartMs) {
                        try {
                            sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    mMediaCodec.releaseOutputBuffer(xIndex, true);
                }

                // All decoded frames have been rendered
                if ((mRenderingBuffers.mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.v(TAG, "outputBuffers BUFFER_FLAG_END_OF_STREAM track "
                          + mTrackId);
                    done = true;
                }

                return done;
            }
        }

        private class AudioRenderObject extends RenderObject {
            public void initializeObject(String xMime) {
                super(xMime);
                mMedia = Media.AUDIO;
            }

            @Override
            public int dequeue() {
                Log.v(TAG, "Dequeue data .......");
                int outIndex = mMediaCodec.dequeueOutputBuffer(
                                                               mRenderingBuffers.mBufferInfo, 0);
                Log.v(TAG, "Encrypted data dequeued");

                switch (outIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.v(TAG, "INFO_OUTPUT_BUFFERS_CHANGED track " + mTrackId);
                    mRenderingBuffers.mOutputBuffers = mMediaCodec
                        .getOutputBuffers();
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    final MediaFormat format = mMediaCodec.getOutputFormat();
                    Log.d(TAG, "Output format has changed to " + format + "("
                          + MediaFormat.KEY_SAMPLE_RATE + ")" + "track "
                          + mTrackId);
                    mAudioTrack.setPlaybackRate(format
                                                .getInteger(MediaFormat.KEY_SAMPLE_RATE));
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    Log.v(TAG, "INFO_TRY_AGAIN_LATER decoder timmed out track "
                          + mTrackId);
                    break;
                }

                return outIndex;
            }

            @Override
            public boolean render(int xIndex) {
                boolean done = false;
                if (xIndex >= 0) {
                    ByteBuffer buffer = mRenderingBuffers.mOutputBuffers[xIndex];
                    Log.v(TAG, "Rendering audio track" + mTrackId);
                    final byte[] chunk = new byte[mRenderingBuffers.mBufferInfo.size];
                    buffer.get(chunk);
                    buffer.clear();
                    if (chunk.length > 0) {
                        mAudioTrack.write(chunk, 0, chunk.length);
                    }
                    mMediaCodec.releaseOutputBuffer(xIndex, false);
                }

                // All decoded frames have been rendered
                if ((mRenderingBuffers.mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.v(TAG, "outputBuffers BUFFER_FLAG_END_OF_STREAM track "
                          + mTrackId);
                    done = true;
                }
                return done;
            }
        }

        private abstract class MediaPlayerThread implements Runnable {
            protected RenderObject mRenderObject;
            protected Thread mRunner;
            protected boolean mStop = false;
            protected boolean mStart = false;

            public MediaPlayerThread(PlayerThread xPlayer) {
                mRunner = new Thread(this, "Video player Thread");
                mRunner.start();
            }

            public void stopThread() {
                this.mStop = true;
                try {
                    mRunner.join(1000);
                    if (mRunner.isAlive()) {
                        Log.e(TAG, "Serious problem with mediaplayer thread! "
                              + mRunner.getName());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            public boolean isStopped() {
                return this.mStop;
            }

            public void startThread() {
                this.mStart = true;
            }

            @Override
            public void run() {
                while (!mStop) {
                    if (mStart) {
                        int index = mRenderObject.dequeue();
                        mStop = mRenderObject.render(index);
                    } else {
                        try {
                            sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        private class VideoPlayerThread extends MediaPlayerThread {
            public VideoPlayerThread(PlayerThread xPlayer,
                                     RenderObject xRenderObject) {
                super(xPlayer);
                this.mRenderObject = xRenderObject;
            }
        }

        private class AudioPlayerThread extends MediaPlayerThread {

            public AudioPlayerThread(PlayerThread xPlayer,
                                     RenderObject xRenderObject) {
                super(xPlayer);
                this.mRenderObject = xRenderObject;

                int buffsize = AudioTrack.getMinBufferSize(44100,
                                                           AudioFormat.CHANNEL_OUT_STEREO,
                                                           AudioFormat.ENCODING_PCM_16BIT);
                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                                             AudioFormat.CHANNEL_OUT_STEREO,
                                             AudioFormat.ENCODING_PCM_16BIT, buffsize,
                                             AudioTrack.MODE_STATIC);
            }
        }
    }
}
