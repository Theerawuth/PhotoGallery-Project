package com.augmentis.ayp.photogallery;

import android.support.v4.app.Fragment;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    @Override
    protected Fragment onCreateFragment() {

        return PhotoGalleryFragment.newInstance();
    }
}
