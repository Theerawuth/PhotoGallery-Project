package com.augmentis.ayp.photogallery;

import android.app.SearchManager;
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
import android.support.v4.content.FileProvider;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.LruCache;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

    private RecyclerView mRecyclerView;
    private PhotoGalleryAdapter mAdapter;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloaderThread;
    private FetcherTask mFetcherTask;
    private LruCache<String, Bitmap> mMemoryCache;
    private String mSearchKey;
    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024) ;
    final int cacheSize = maxMemory / 8;

    //Step 1: Create << newInstance for Calling Fragment, onCreate, onCreateView for set XML.///////

    public static PhotoGalleryFragment newInstance() {

        Bundle args = new Bundle();

        PhotoGalleryFragment fragment = new PhotoGalleryFragment();
        fragment.setArguments(args);
        return fragment;
    }

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mRecyclerView = (RecyclerView) v.findViewById(R.id.photo_gallery_recycler_view);
        Resources r = getResources();
        int gridSize = r.getInteger(R.integer.grid_size);

        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), gridSize));

        mSearchKey = PhotoGalleryPreference.getStoredSearchKey(getActivity());
        loadPhotos();

        Log.d(TAG, "On Create Complete - Loaded search key = " + mSearchKey);
        return v;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

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

    @Override
    public void onPause() {
        super.onPause();

        PhotoGalleryPreference.setStoredSearchKey(getActivity(), mSearchKey);
    }

    @Override
    public void onResume() {
        super.onResume();

        String searchKey = PhotoGalleryPreference.getStoredSearchKey(getActivity());

        if(searchKey != null){
            mSearchKey = searchKey;
        }
        Log.d(TAG, "On Resume Complete");
    }



    public void loadPhotos() {

        if(!mFetcherTask.isRunning() || mFetcherTask == null){
            mFetcherTask = new FetcherTask();

            if(mSearchKey != null){
                mFetcherTask.execute(mSearchKey);
            }
            else
            {
                mFetcherTask.execute();
            }
        }
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


    class FetcherTask extends AsyncTask<String, Void, List<GalleryItem>> {
        private FlickrFetcher mFlickrFetcher;

        boolean isRunning() {
            return running;
        }

        @Override
        protected List<GalleryItem> doInBackground(String... param) {

            synchronized (this) {
                running = true;
            }
            try{
                List<GalleryItem> itemList = new ArrayList<>();
                mFlickrFetcher = new FlickrFetcher();
                if(param.length > 0) {
                    mFlickrFetcher.searchPhotos(itemList, param[0]);
                }
                else{
                    mFlickrFetcher.getRecentPhotos(itemList);
                }
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);

        MenuItem menuItem = menu.findItem(R.id.menu_search);
        final SearchView searchView = (SearchView) menuItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "Query text submitted: " + query);
                mSearchKey = query;
                loadPhotos();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "Query text submitted: " + newText);
                return false;
            }

        });

        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchView.setQuery(mSearchKey, false);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_reload:
                loadPhotos();
                return true;
            case R.id.menu_clear_search:
                mSearchKey = null;
                loadPhotos();
                return true;

        }

        return super.onOptionsItemSelected(item);
    }
}
