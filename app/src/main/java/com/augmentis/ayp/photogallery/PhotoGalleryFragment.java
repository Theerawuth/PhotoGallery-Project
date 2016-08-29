package com.augmentis.ayp.photogallery;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.LruCache;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Theerawuth on 8/16/2016.
 */


public class PhotoGalleryFragment extends VisibleFragment {

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

        Intent i = PollService.newIntent(getActivity());
        getActivity().startService(i);
        PollService.setServiceAlarm(getContext(), true);

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


    class PhotoHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener,
            View.OnCreateContextMenuListener,
            MenuItem.OnMenuItemClickListener
    {
        ImageView mPhoto;
        String mBigUrl;

        public PhotoHolder(View itemView){
            super(itemView);

            mPhoto = (ImageView) itemView.findViewById(R.id.image_photo);
            mPhoto.setOnClickListener(this);

            itemView.setOnCreateContextMenuListener(this);

        }

        public void bindDrawable(@NonNull Drawable drawable) {
            mPhoto.setImageDrawable(drawable);

        }

        public void setBigUrl(String bigUrl){
            mBigUrl = bigUrl;
        }

        @Override
        public void onClick(View view) {
            Snackbar.make(mRecyclerView, "Clicked on Photo", Snackbar.LENGTH_SHORT).show();

            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            final ImageView imgView = new ImageView(getActivity());

            imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            builder.setView(imgView);
            builder.setPositiveButton("Close", null);

            // Excute Async Task
            new AsyncTask<String, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(String... urls) {
                    FlickrFetcher flickrFetcher = new FlickrFetcher();
                    Bitmap bm = null;
                    try {
                        byte[] bytes = flickrFetcher.getUrlBytes(urls[0]);
                        bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    }catch (IOException ioe){
                        Log.d(TAG, "error in reading Bitmap");
                        return null;
                    }
                    return bm;
                }

                @Override
                protected void onPostExecute(Bitmap img) {
                    ImageViewDialog imageViewDialog = ImageViewDialog.getInstance(img);
                    FragmentManager fragmentManager = getFragmentManager();

                    imageViewDialog.show(fragmentManager,"NACK_KUY");

                }
            }.execute(mBigUrl);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            MenuItem menuItem = menu.add(R.string.open_by_url);
            menu.setHeaderTitle(mBigUrl);
            menu.setHeaderIcon(android.R.drawable.ic_dialog_alert);
            menuItem.setOnMenuItemClickListener(this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Toast.makeText(getActivity(), "Url : = " + mBigUrl, Toast.LENGTH_LONG).show();
            return true;
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

//            Log.d(TAG, "bind position #" + position + ", url: " + galleryItem.getUrl());

            holder.setBigUrl(galleryItem.getBigSizeUrl());
            holder.bindDrawable(jpgDrawable);


            //
            mThumbnailDownloaderThread.queueThumbnailDownloader(holder, galleryItem.getUrl());
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

        // render polling
        MenuItem mnuPolling = menu.findItem(R.id.menu_toggle_polling);
        if(PollService.isServiceAlarmOn(getActivity())){
            mnuPolling.setTitle(R.string.stop_polling);
        } else {
            mnuPolling.setTitle(R.string.start_polling);
        }
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
            case R.id.menu_toggle_polling:
                boolean shouldStart = !PollService.isServiceAlarmOn(getActivity());
                Log.d(TAG, ((shouldStart) ? "Start" : "Stop") + "Intent service");
                PollService.setServiceAlarm(getActivity(), shouldStart);
                getActivity().invalidateOptionsMenu(); //refresh menu
                return true;

            case R.id.menu_manual_check:
                Intent pollIntent = PollService.newIntent(getActivity());
                getActivity().startService(pollIntent);
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    public static class ImageViewDialog extends DialogFragment{

    private static final String ARG_BITMAP = "ARG BITMAP";

    public static ImageViewDialog getInstance(Bitmap bitmap) {

        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_BITMAP, bitmap);

        ImageViewDialog dialogFragment = new ImageViewDialog();
        dialogFragment.setArguments(bundle);
        return dialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bitmap bmp = getArguments().getParcelable(ARG_BITMAP);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        ImageView imgView = new ImageView(getActivity());
        imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgView.setImageDrawable(new BitmapDrawable(getResources(), bmp));

        builder.setView(imgView);
        builder.setPositiveButton("Close", null);
        return builder.create();

        }
    }
}
