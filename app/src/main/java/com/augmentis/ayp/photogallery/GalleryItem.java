package com.augmentis.ayp.photogallery;

import java.util.Objects;

/**
 * Created by Theerawuth on 8/16/2016.
 */
public class GalleryItem {

    private String mId;
    private String mTitle;
    private String mUrl;
    private String mBigSizeUrl;

    public static void printHello(){
        System.out.println("Hello");
    }

    public void setId(String id) {
        mId = id;
    }

    public String getId() {
        return mId;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getUrl() {

        return mUrl;
    }



    public String getName() {

        return getTitle();
    }

    public void setTile(String tile) {
        mTitle = tile;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof GalleryItem){
            // is GalleryItem too !!
            GalleryItem that = (GalleryItem) obj;

            return  that.mId != null && mId != null && that.mId == this.mId;
            }

        return false;
    }

    public void setBigSizeUrl(String bigSizeUrl) {
        mBigSizeUrl = bigSizeUrl;
    }

    public String getBigSizeUrl() {
        return mBigSizeUrl;
    }
}
