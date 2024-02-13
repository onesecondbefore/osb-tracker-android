//  Copyright (c) 2023 Onesecondbefore B.V. All rights reserved.
//  This Source Code Form is subject to the terms of the Mozilla Public
//  License, v. 2.0. If a copy of the MPL was not distributed with this
//  file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.onesecondbefore.tracker;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;
import org.json.JSONObject;
import java.io.IOException;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class ApiTask {
    private final JSONObject mData;
    private final String mUrl;

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
    private final LinkedList<ApiTask> mQueue = new LinkedList<>();
    private boolean mIsEnabled = true;
    private final Context mContext;
    private final String mUserAgent;


    ApiQueue(Context context) {
        mContext = context;
        mUserAgent = createUserAgentString();
    }

    private String createUserAgentString() {
        String product = Build.PRODUCT;
        String appName = "unknown";
        String appVersion = "unknown";

        boolean isEmulator = product.equals("sdk");
        String deviceBrand = isEmulator ? null : Build.BRAND;
        String deviceModel = isEmulator ? "Emulator" : Build.MODEL;

        String release = Build.VERSION.RELEASE;

        try {
            String packageName = this.mContext.getPackageName();
            PackageInfo pInfo = this.mContext.getPackageManager().getPackageInfo(packageName, 0);
            appVersion = pInfo.versionName;
            ApplicationInfo applicationInfo = this.mContext.getPackageManager().getApplicationInfo(packageName, 0);
            appName = (String) ((applicationInfo != null) ? this.mContext.getPackageManager().getApplicationLabel(applicationInfo) : "unknown");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getAppInfo - " + e.getMessage());
        }

        return appName + "/" + appVersion + " (Linux; Android " + release + "; " + deviceBrand + " " + deviceModel + ")";
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
            synchronized (mQueue) {
                try {
                    ApiTask task = mQueue.removeFirst();
                    if (task != null) {
                        sendPostRequest(task.getUrl(), task.getData().toString());
                    }
                } catch (NoSuchElementException ex) {
                    /* Nothing */
                }
            }
        }
    }

    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private void sendPostRequest(String url, String data) {
        long start = System.currentTimeMillis();
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(data, JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", mUserAgent)
                .post(body)
                .build();
        try {
            Log.i(TAG, "Request: " + request);
            Log.i(TAG, "Sending request to " + url);
            Response response = client.newCall(request).execute();
            Log.i(TAG, "Got Response: " + response.code() + " " + (response.body() != null ? response.body().string() : "") + " in " + (System.currentTimeMillis() - start) + " ms");
        } catch (IOException ex) {
            Log.e(TAG, "Could not POST request: " + ex.getMessage());
        }
    }

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
