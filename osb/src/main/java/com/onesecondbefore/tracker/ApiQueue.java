package com.onesecondbefore.tracker;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class ApiTask {
    private JSONObject mData = null;
    private String mUrl = "";

    ApiTask(String url, JSONObject data) {
        this.mData = data;
        this.mUrl = url;
    }

    public JSONObject getData() {
        return mData;
    }

    public String getUrl() {
        return mUrl;
    }
}

class ApiQueue {
    private static final String TAG = "OSB:Queue";
    private LinkedList<ApiTask> mQueue = new LinkedList<>();
    private boolean mIsEnabled = true;
    private Context mContext;

    ApiQueue(Context context) {
        mContext = context;
    }

    public void destroy() {
        mQueue.clear();
    }

    public void addToQueue(String url, JSONObject data) {
        if (url != null && data != null) {
            ApiTask task = new ApiTask(url, data);
            mQueue.push(task);

            processQueue();
        }
    }

    public void pause() {
        mIsEnabled = false;
    }

    public void resume() {
        mIsEnabled = true;
        processQueue();
    }

    /* Private Functions */
    private void processQueue() {
        if (mIsEnabled && isNetworkConnected() && !mQueue.isEmpty()) {
            ApiTask task = mQueue.pop();
            sendPostRequest(task.getUrl(), task.getData().toString());
        }
    }

    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private void sendPostRequest(String url, String data) {
        long start = System.currentTimeMillis();
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON, data);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", OSB.userAgent)
                .post(body)
                .build();
        try {
            Log.i(TAG, "Sending request to " + url);
            Response response = client.newCall(request).execute();
            Log.i(TAG, "Got Response: " + response.code() + " " + response.body().string() + " in " + (System.currentTimeMillis() - start) + " ms");
        } catch (IOException ex) {
            Log.e(TAG, "Could not POST request: " + ex.getMessage());
        }
    }


//    private void sendPostRequest(String url, String data) {
//        HttpURLConnection httpURLConnection = null;
//
//        Log.i(TAG, "Sending request to " + url);
//        try {
//            httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
//            httpURLConnection.setRequestMethod("POST");
//            httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
//            httpURLConnection.setRequestProperty("Accept", "application/json");
//            httpURLConnection.setRequestProperty("User-Agent", OSB.userAgent);
//            httpURLConnection.setDoOutput(true);
//
//            DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
//            wr.writeBytes(data);
//            wr.flush();
//            wr.close();
//
//            InputStream in = httpURLConnection.getInputStream();
//            InputStreamReader inputStreamReader = new InputStreamReader(in);
//
//            StringBuilder response = new StringBuilder();
//            int inputStreamData = inputStreamReader.read();
//            while (inputStreamData != -1) {
//                char current = (char) inputStreamData;
//                response.append(current);
//                inputStreamData = inputStreamReader.read();
//            }
//
//            Log.i(TAG, "Got Response: " + response.toString());
//        } catch (Exception e) {
//            Log.e(TAG, "sendPostRequest - " + e.getMessage());
//        }  finally {
//            if (httpURLConnection != null) {
//                httpURLConnection.disconnect();
//            }
//
//            processQueue();
//        }
//    }

    private boolean isNetworkConnected() {
        boolean isOnline = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                if (info.getType() == ConnectivityManager.TYPE_WIFI ||
                        info.getType() == ConnectivityManager.TYPE_MOBILE) {
                    isOnline = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "isNetworkConnected - " + e.getMessage());
        }

        return isOnline;
    }
}
