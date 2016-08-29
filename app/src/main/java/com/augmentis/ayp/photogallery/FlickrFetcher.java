package com.augmentis.ayp.photogallery;

import android.net.Uri;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by Theerawuth on 8/16/2016.
 */
public class FlickrFetcher {

    private static final String TAG = "FlickrFetcher";
    private static final String API_KEY = "f0c6703100a0b1e270ee958a32a0e09c";
    private static final String FLICKR_URL = "https://api.flickr.com/services/rest/";

    private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
    private static final String METHOD_GET_SEARCH = "flickr.photos.search";

    // method : use download
    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            //if connection is not OK throw new IOException
            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            int byteRead = 0;

            byte[] buffer = new byte[2048];

            while ((byteRead = in.read(buffer)) > 0 ) {
                out.write(buffer, 0, byteRead);
            }

            out.close();

            return out.toByteArray();

        }finally {
            connection.disconnect();

        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    private String buildUri(String method, String ... param) throws IOException {

        Uri baseUrl = Uri.parse(FLICKR_URL); //Uri ไว้แปลงพวก PathFile
        Uri.Builder builder = baseUrl.buildUpon();
            builder.appendQueryParameter("method", method);
            builder.appendQueryParameter("api_key", API_KEY);
            builder.appendQueryParameter("format", "json");
            builder.appendQueryParameter("nojsoncallback", "1");
            builder.appendQueryParameter("extras", "url_s,url_z");

        if (METHOD_GET_SEARCH.equalsIgnoreCase(method)) {

            builder.appendQueryParameter("text", param[0]);
        }

        Uri completeUrl = builder.build();
            String url = completeUrl.toString();

            Log.d(TAG, "Run URL: " + url);


            Log.i(TAG, "Search: Received JSON: " + url);
            return url;
    }

    private String queryItem(String url) throws IOException {
        String jsonString =  getUrlString(url);
        return jsonString;
    }

    /**
     * Search photo than put into <b>items</>
     * @param items array target
     * @param key to search
     *
     */
    //method: use getPhotos
    public void getRecentPhotos (List<GalleryItem> items) {
        try {
            String url = buildUri(METHOD_GET_RECENT);
            String jsonStr = queryItem(url);
            if(jsonStr != null){
                parseJSON(items, jsonStr);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to fetch item", e);
        }
    }

    public void searchPhotos (List<GalleryItem> items, String key) {
        try {
            String url = buildUri(METHOD_GET_SEARCH, key);
            String jsonStr = queryItem(url);
            if(jsonStr != null){
                parseJSON(items, jsonStr);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to fetch item", e);
        }
    }


    private void parseJSON(List<GalleryItem> newGalleryItemList, String jsonBodyStr)
            throws IOException, JSONException {

        JSONObject jsonBody = new JSONObject(jsonBodyStr);
        JSONObject photosJson = jsonBody.getJSONObject("photos");
        JSONArray photoListJson = photosJson.getJSONArray("photo");

        for (int i = 0; i < photoListJson.length(); i++) {
            JSONObject jsonPhotoItem = photoListJson.getJSONObject(i);

            GalleryItem item = new GalleryItem();

            item.setId(jsonPhotoItem.getString("id"));
            item.setTitle(jsonPhotoItem.getString("title"));
            item.setOwner(jsonPhotoItem.getString("owner"));

            if (!jsonPhotoItem.has("url_s")) {
                continue;
            }
            item.setUrl(jsonPhotoItem.getString("url_s"));

            if (!jsonPhotoItem.has("url_z")) {
                continue;
            }
            item.setBigSizeUrl(jsonPhotoItem.getString("url_z"));

            newGalleryItemList.add(item);
        }
    }
}





