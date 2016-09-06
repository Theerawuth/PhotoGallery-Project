package com.augmentis.ayp.photogallery;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

/**
 * Created by theerawuth on 05-Sep-16.
 */
public class SettingActivity extends SingleFragmentActivity {

    public static Intent newIntent(Context c) {
        return new Intent(c, SettingActivity.class);
    }

    @Override
    protected Fragment onCreateFragment() {
        return PhotoSettingFragment.newInstance();
    }
}
