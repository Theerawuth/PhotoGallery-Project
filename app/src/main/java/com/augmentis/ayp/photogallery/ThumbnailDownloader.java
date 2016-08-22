package com.augmentis.ayp.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Created by Theerawuth on 8/18/2016.
 */
public class ThumbnailDownloader<T> extends HandlerThread {

    private static final String TAG = "TThumbnailDownloader";
    private static final int DOWNLOAD_FILE = 2018;

    private Handler mRequestHandler;
    private final ConcurrentMap<T, String> mRequestUrlMap = new ConcurrentHashMap<>();

    private Handler mResponseHandler;
    private ThumbnailDownloaderListener<T> mThumbnailDownloaderListener;

    interface ThumbnailDownloaderListener<T> {
        void onThumbnailDownloaderListener(T target, Bitmap thumbnail, String url);
    }

    public void setmThumbnailDownloaderListener(ThumbnailDownloaderListener<T> mThumbnailDownloaderListener) {
        this.mThumbnailDownloaderListener = mThumbnailDownloaderListener;
    }

    public ThumbnailDownloader(Handler mUIHandle) {
        super(TAG);

        mResponseHandler = mUIHandle;
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler(){

            @Override
            public void handleMessage(Message msg) {
                if(msg.what == DOWNLOAD_FILE){
                    T target = (T) msg.obj;
                    String url = mRequestUrlMap.get(target);
//                    Log.i(TAG, "Got Message from queue pls download this URL " + url);
                    handleRequestDownload(target,url);
                }
            }
        };
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(DOWNLOAD_FILE);
    }

    public void handleRequestDownload(final T target, final String url){
        try{
            if(url == null){
                return;
            }

            byte[] bitMapBytes = new FlickrFetcher().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitMapBytes, 0, bitMapBytes.length);

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    String currentUrl = mRequestUrlMap.get(target);
                    if(currentUrl!= null && !currentUrl.equals(url)){
                        return;
                    }
                    // url is ok(the same one)
                    mRequestUrlMap.remove(target);
                    mThumbnailDownloaderListener.onThumbnailDownloaderListener(target, bitmap, url);
                }
            });


//            Log.i(TAG, "Bitmap URL downloaded: ");
        }catch (IOException e){
            Log.e(TAG, "Error downloading : ", e);
        }
    }

    public void queueThumbnailDownloader(T target, String url){
//        Log.d(TAG, "Gor url : " + url);

        if( null == url ){
            mRequestUrlMap.remove(target);
        }else {
            mRequestUrlMap.put(target, url);

            Message msg = mRequestHandler.obtainMessage(DOWNLOAD_FILE, target); //get message from handler
            msg.sendToTarget(); //send to handler
        }

    }

}
