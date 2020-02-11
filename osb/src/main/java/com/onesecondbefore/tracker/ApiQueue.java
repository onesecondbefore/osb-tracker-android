package com.onesecondbefore.tracker;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;

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
    private LinkedList<ApiTask> mQueue = new LinkedList<ApiTask>();
    private boolean mIsEnabled = true;
    private Context mContext;

    ApiQueue(Context context) {
        mContext = context;
    }

    public void destory() {
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

    private void sendPostRequest(String url, String data) {
        HttpURLConnection httpURLConnection = null;

        Log.i(TAG, "Sending request to " + url);
        try {
            httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
            wr.writeBytes(data);
            wr.flush();
            wr.close();

            InputStream in = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(in);

            String response = "";
            int inputStreamData = inputStreamReader.read();
            while (inputStreamData != -1) {
                char current = (char) inputStreamData;
                inputStreamData = inputStreamReader.read();
                response += current;
            }

            Log.i(TAG, "Got Response: " + response);
        } catch (Exception e) {
            Log.e(TAG, "sendPostRequest - " + e.getMessage());
        }  finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }

            processQueue();
        }
    }

    private boolean isNetworkConnected() {
        boolean isOffline = true;
        try {
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info == null && !info.isConnected()) {
                if (info.getType() == ConnectivityManager.TYPE_WIFI ||
                        info.getType() == ConnectivityManager.TYPE_MOBILE) {
                    isOffline = false;;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "isNetworkConnected - " + e.getMessage());
        }

        return isOffline;
    }
}
