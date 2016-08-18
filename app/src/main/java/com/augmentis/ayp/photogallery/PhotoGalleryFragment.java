package com.augmentis.ayp.photogallery;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.LruCache;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Theerawuth on 8/16/2016.
 */
public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";

    public static PhotoGalleryFragment newInstance() {

        Bundle args = new Bundle();

        PhotoGalleryFragment fragment = new PhotoGalleryFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private RecyclerView mRecyclerView;
    private FlickrFetcher mFlickrFetcher;
    private PhotoGalleryAdapter mAdapter;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloaderThread;
    private FetcherTask mFetcherTask;
    private LruCache<String, Bitmap> mMemoryCache;
    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024) ;
    final int cacheSize = maxMemory / 8;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        setHasOptionsMenu(true);

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize){
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };


        // Move from onCreateView
        mFlickrFetcher = new FlickrFetcher();
        mFetcherTask = new FetcherTask(); //run another thread

        Handler responseUIHandler = new Handler();

        ThumbnailDownloader.ThumbnailDownloaderListener listener =
                new ThumbnailDownloader.ThumbnailDownloaderListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaderListener(PhotoHolder target, Bitmap thumbnail, String url) {
                        if (mMemoryCache.get(url) == null)
                        {
                            mMemoryCache.put(url, thumbnail);
                        }

                        Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                        target.bindDrawable(drawable);
                    }
                };

        mThumbnailDownloaderThread = new ThumbnailDownloader<>(responseUIHandler);
        mThumbnailDownloaderThread.setmThumbnailDownloaderListener(listener);
        mThumbnailDownloaderThread.start();
        mThumbnailDownloaderThread.getLooper();

        Log.i(TAG, "Start background thread");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mThumbnailDownloaderThread.quit();
        Log.i(TAG, "Stop background thread");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mThumbnailDownloaderThread.clearQueue();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mRecyclerView = (RecyclerView) v.findViewById(R.id.photo_gallery_recycler_view);
        Resources r = getResources();
        int gridSize = r.getInteger(R.integer.grid_size);

        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), gridSize));

        if(!mFetcherTask.isRunning()){
            mFetcherTask = new FetcherTask();
            mFetcherTask.execute();

        }



        return v;

    }

    class PhotoHolder extends RecyclerView.ViewHolder {
        ImageView mPhoto;

        public PhotoHolder(View itemView){
            super(itemView);

            mPhoto = (ImageView) itemView.findViewById(R.id.image_photo);
        }

        public void bindDrawable(@NonNull Drawable drawable) {
            mPhoto.setImageDrawable(drawable);

        }
    }

    class PhotoGalleryAdapter extends RecyclerView.Adapter<PhotoHolder> {

        List<GalleryItem> mGalleryItemList;

        PhotoGalleryAdapter(List<GalleryItem> galleryItems) {
            mGalleryItemList = galleryItems;
        }
        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getActivity()).inflate(R.layout.item_photo, parent, false);

            return new PhotoHolder(v);
        }

        //use map or set picture
        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            Drawable jpgDrawable =
                    ResourcesCompat.getDrawable(getResources(), R.drawable.photo, null);
            GalleryItem galleryItem = mGalleryItemList.get(position);

            Log.d(TAG, "bind position #" + position + ", url: " + galleryItem.getUrl());

            holder.bindDrawable(jpgDrawable);

            //
            mThumbnailDownloaderThread.queueThumbnailDownloader(holder, galleryItem.getUrl());
        }

        @Override
        public int getItemCount() {
            return mGalleryItemList.size();
        }
    }
    boolean running = false;

    class FetcherTask extends AsyncTask<Void, Void, List<GalleryItem>> {

        boolean isRunning() {
            return running;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {

            synchronized (this) {
                running = true;
            }
            try{
                List<GalleryItem> itemList = new ArrayList<>();
                mFlickrFetcher.fetchItems(itemList);
                return itemList;
            }finally {
                synchronized (this){
                    running = false;
                }
            }

        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
                mAdapter = new PhotoGalleryAdapter(galleryItems);
                mRecyclerView.setAdapter(mAdapter);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

            String formatString = getResources().getString(R.string.photo_progress_loaded);
            Snackbar.make(mRecyclerView, formatString, Snackbar.LENGTH_SHORT).show();
        }
    }
}
