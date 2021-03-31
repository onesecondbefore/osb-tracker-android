package com.onesecondbefore.tracker;

import android.content.Context;
import android.util.Log;
import android.webkit.WebSettings;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import org.json.JSONObject;

import java.util.Map;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OSB implements LifecycleObserver {
    private static final String TAG = "OSB:Api";
    private static OSB mInstance = null;

    private Config mConfig = new Config();
    private GpsTracker mGpsTracker = null;
    private ApiQueue mQueue = null;
    private Context mContext;

    public static String userAgent = null;
    public static String viewId = calculateViewId();

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        clear();
    }

    public static OSB getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new OSB();
            mInstance.mContext = context.getApplicationContext();
            userAgent = WebSettings.getDefaultUserAgent(context);
        }
        return mInstance;
    }

    public void clear() {
        if (mGpsTracker != null) {
            mGpsTracker.stopTracker();
            mGpsTracker = null;
        }

        if (mQueue != null) {
            mQueue.destroy();
            mQueue = null;
        }
    }

    public void initialize(String accountId, String url) {
        initialize(accountId, url, null, null);
    }

    public void initialize(String accountId, String url, String siteId, String domain) {
        mConfig.setAccountId(accountId);
        mConfig.setServerUrl(url);
        mConfig.setSiteId(siteId);
        mConfig.setDomain(domain);

        clear();
        mQueue = new ApiQueue(mContext);
        startGpsTracker();

        Log.i(TAG, "OSB - Initialized");
    }

    public void debug(boolean isEnabled) {
        mConfig.setDebug(isEnabled);
    }

    public void sendPageView(String url, String title, String referrer) {
        sendPageView(url, title, referrer, null);
    }

    public void sendPageView(String url, String title, String referrer, Map<String, Object> data) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put("url", url);
        data.put("ttl", title);
        data.put("ref", referrer);
        data.put("vid", viewId);
        send(EventType.PAGEVIEW, null, data);
    }

    public void sendScreenView(String screenName) {
        sendScreenView(screenName, null);
    }

    public void sendScreenView(String screenName, Map<String, Object> data) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put("sn", screenName);
        data.put("vid", viewId);
        send(EventType.SCREENVIEW, null, data);
    }

    public void send(EventType type) {
        sendEventToQueue(type, null, null);
    }

    public void send(EventType type, Map<String, Object> data) {
        sendEventToQueue(type, null, data);
    }

    public void send(EventType type, String actionType,
                          Map<String, Object> data) {
        sendEventToQueue(type, actionType, data);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void onAppBackgrounded() {
        Log.d(TAG, "App in background");
        if (mGpsTracker != null) {
            mGpsTracker.stopTracker();
        }

        if (mQueue != null) {
            mQueue.pause();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private void onAppForegrounded() {
        Log.d(TAG, "App in foreground");
        startGpsTracker();

        if (mQueue != null) {
            mQueue.resume();
        }
    }

    /* Private Functions */
    private void startGpsTracker() {
        if (mGpsTracker == null) {
            mGpsTracker = new GpsTracker(mContext);
        }

        mGpsTracker.startTracker();
    }

    private void sendEventToQueue(EventType type, String actionType,
                                  Map<String, Object> data) {
        if (mQueue != null) {
            this.startGpsTracker();

            final Event event = new Event(type, actionType, data,
                mGpsTracker.canGetLocation(), mGpsTracker.getLatitude(), mGpsTracker.getLongitude());
            Thread t = new Thread(new Runnable() {
                public void run() {
                    JsonGenerator generator = new JsonGenerator(mContext);
                    JSONObject jsonData = generator.generate(mConfig, event);
                    mQueue.addToQueue(mConfig.getServerUrl(), jsonData);
                }
            });

            t.start();
        }
    }

    private static String calculateViewId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
