package com.onesecondbefore.tracker;

import android.content.Context;
import android.util.Log;
import android.webkit.WebSettings;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import org.json.JSONObject;

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

    protected static String userAgent = null;
    protected static String viewId = calculateViewId();

    public enum EventType {
        IDS, SOCIAL, EVENT, ACTION, EXCEPTION, PAGEVIEW, SCREENVIEW, TIMING
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        clear();
    }

    public static OSB initialize(Context context) {
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

    public void create(String accountId, String url) {
        create(accountId, url, null);
    }

    public void create(String accountId, String url, String siteId) {
        mConfig.setAccountId(accountId);
        mConfig.setServerUrl(url);
        mConfig.setSiteId(siteId);

        clear();
        mQueue = new ApiQueue(mContext);
        startGpsTracker();

        Log.i(TAG, "OSB - Initialized");
    }

    public void debug(boolean isEnabled) {
        mConfig.setDebug(isEnabled);
    }

    public void sendPageView(String url, String title) {
        sendPageView(url, title, null, null);
    }

    public void sendPageView(String url, String title, String referrer) {
        sendPageView(url, title, referrer, null);
    }

    public static void sendPageView(String url, String title, String referrer, Map<String, Object> data) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put("url", url);
        data.put("ttl", title);
        data.put("ref", referrer);
        data.put("vid", viewId);
        send(EventType.PAGEVIEW, null, data);
    }

    public static void sendScreenView(String screenName) {
        sendScreenView(screenName, null);
    }

    public static void sendScreenView(String screenName, Map<String, Object> data) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put("sn", screenName);
        data.put("vid", viewId);
        send(EventType.SCREENVIEW, null, data);
    }

    public static void sendEvent(String category) {
        sendEvent(category, null, null, null, null);
    }

    public static void sendEvent(String category, String action) {
        sendEvent(category, action, null, null, null);
    }

    public static void sendEvent(String category, String action, String label) {
        sendEvent(category, action, label, null, null);
    }

    public static void sendEvent(String category, String action, String label, String value) {
        sendEvent(category, action, label, value, null);
    }

    public static void sendEvent(String category, String action, String label, String value, Map<String, Object> data) {
        if (data == null) {
            data = new HashMap<>();
        }
        if (category != null) {
            data.put("category", category);
        }
        if (action != null) {
            data.put("action", action);
        }
        if (label != null) {
            data.put("label", label);
        }
        if (value != null) {
            data.put("value", value);
        }
        send(EventType.EVENT, null, data);
    }

    public static void send(EventType type) {
        send(type, null, null);
    }

    public static void send(EventType type, Map<String, Object> data) {
        send(type, null, data);
    }

    public static void send(EventType type, String actionType,
                          Map<String, Object> data) {
        if (mInstance != null) {
            mInstance.sendEventToQueue(type, actionType, data);
        } else {
            throw new IllegalArgumentException("Initialize OSB Tracker first with OSB osb = OSB.initialize(this); osb.create(\"...\");");
        }

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

    private void sendEventToQueue(OSB.EventType type, String actionType,
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
