package com.augmentis.ayp.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;

/**
 * Created by Theerawuth on 8/29/2016.
 */
public class PhotoPageActivity extends SingleFragmentActivity {

    public static Intent newIntent(Context context, Uri uri){
        Intent intent = new Intent(context, PhotoPageActivity.class);
        intent.setData(uri);
        return intent;
    }

    @Override
    protected Fragment onCreateFragment() {
        Uri uri = getIntent().getData();

        return PhotoPageFragment.newInstance(uri);
    }
}
