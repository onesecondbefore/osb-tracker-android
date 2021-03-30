package com.onesecondbefore.tracker;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import org.json.JSONObject;

import java.util.Dictionary;

public final class OSB implements LifecycleObserver {
    private static final String TAG = "OSB:Api";
    private static OSB mInstance = null;

    private Config mConfig = new Config();
    private GpsTracker mGpsTracker = null;
    private ApiQueue mQueue = null;
    private Context mContext;

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        clear();
    }

    public static OSB getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new OSB();
            mInstance.mContext = context;
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

    public void sendEvent(EventType type) {
        sendEventToQueue(type, "", null);
    }

    public void sendEvent(EventType type, Dictionary<String, Object> data) {
        sendEventToQueue(type, "", data);
    }

    public void sendEvent(EventType type, String actionType,
                          Dictionary<String, Object> data) {
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
                                  Dictionary<String, Object> data) {
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
}
