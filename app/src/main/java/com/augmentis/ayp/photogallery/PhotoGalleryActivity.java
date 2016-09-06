package com.augmentis.ayp.photogallery;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    @Override
    protected Fragment onCreateFragment() {
        return PhotoGalleryFragment.newInstance();
    }

    public static Intent newIntent(Context ctx) {
        return new Intent(ctx, PhotoGalleryActivity.class);
    }
}