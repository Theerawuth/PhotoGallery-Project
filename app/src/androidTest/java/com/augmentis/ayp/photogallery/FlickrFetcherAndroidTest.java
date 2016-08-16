package com.augmentis.ayp.photogallery;

import android.net.Uri;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created by Theerawuth on 8/16/2016.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class FlickrFetcherAndroidTest {

    private FlickrFetcher mFlickrFetcher;

    @Before
    public void setUp() throws Exception {
        mFlickrFetcher = new FlickrFetcher();

    }

    @Test
    public void testGetUrlString() throws Exception {
        String htmlResult = mFlickrFetcher.getUrlString("https://www.augmentis.biz");


        System.out.println(htmlResult);
        assertThat(htmlResult, containsString("IT Professional Services"));
    }

    @Test
    public void testFetch() throws Exception {
        List<GalleryItem> galleryItemList = new ArrayList<>();
        mFlickrFetcher.fetchItems(galleryItemList);

        assertThat(galleryItemList.size(), is(100));
    }
}