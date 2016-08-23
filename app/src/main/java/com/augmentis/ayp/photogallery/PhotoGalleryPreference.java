package com.augmentis.ayp.photogallery;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Theerawuth on 8/19/2016.
 */
public class PhotoGalleryPreference {
    private static final String TAG = "PhotoGalleryPref";
    private static final String PREF_SEARCH_KEY = "PhotoGalleryPref";
    private static final String PREF_LAST_RESULT_ID = "lastResultId";

    public static SharedPreferences getSP(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String getStoredSearchKey(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getString(PREF_SEARCH_KEY, null);
    }

    public static void setStoredSearchKey(Context context, String key){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit()
                .putString(PREF_SEARCH_KEY, key)
                .apply();
    }

    public static String getLastResultId(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getString(PREF_LAST_RESULT_ID, null);
    }
    public static void setStoreLastId(Context context, String lastId) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit()
                .putString(PREF_LAST_RESULT_ID, lastId)
                .apply();
    }
}
