package com.onesecondbefore.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class OSB implements LifecycleObserver {
    private static final String TAG = "OSB:Api";
    private static OSB mInstance = null;

    private Config mConfig = new Config();
    private GpsTracker mGpsTracker = null;
    private ApiQueue mQueue = null;
    private Context mContext;

    private boolean mIsInitialized = false;
    private String mViewId = calculateViewId();
    private String mEventKey = null;
    private Map<String, Object> mEventData = null;
    private Map<String, Object> mHitsData = null;
    private Map<String, Object> mSetDataObject = new HashMap<>();
    private ArrayList<Map<String, Object>> mIds = new ArrayList<>();


    private static final String SPIdentifier = "osb-shared-preferences";
    private static final String SPConsentKey = "osb-consent";


    public enum HitType {
        IDS, SOCIAL, EVENT, ACTION, EXCEPTION, PAGEVIEW, SCREENVIEW, TIMING, VIEWABLE_IMPRESSION, AGGREGATE
    }

    public enum SetType {
        ACTION, EVENT, ITEM, PAGE
    }

    public enum AggregateType {
        MAX, MIN, COUNT, SUM, AVERAGE
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        if (mInstance != null) {
            mInstance.clear();
            mInstance = null;
        }
    }


    public static OSB getInstance() {
        if (mInstance == null) {
            mInstance = new OSB();
        }

        return mInstance;
    }

    public void clear() {
        if (mContext != null) {
            mContext = null;
        }

        if (mGpsTracker != null) {
            mGpsTracker.stopTracker();
            mGpsTracker = null;
        }

        if (mQueue != null) {
            mQueue.destroy();
            mQueue = null;
        }

        mIsInitialized = false;
    }

    public void config(Context context, String accountId, String url) {
        config(context, accountId, url, null);
    }

    public void config(Context context, String accountId, String url, String siteId) {
        clear();

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        mContext = context.getApplicationContext();
        mConfig.setAccountId(accountId);
        mConfig.setServerUrl(url);
        mConfig.setSiteId(siteId);

        mQueue = new ApiQueue(mContext);
        startGpsTracker();

        mIsInitialized = true;
        Log.i(TAG, "OSB - Initialized");
    }


    public void debug(boolean isEnabled) {
        mConfig.setDebug(isEnabled);
    }

    public void set(String name, Map<String, Object> data) {
        mEventKey = name;
        mEventData = data;
    }

    public void set(Map<String, Object> data) {
        mHitsData = data;
    }

    public void set(SetType type, Map<String, Object> data) {
        List<Map<String, Object>> mData = new ArrayList<>();
        mData.add(data);
        set(type, mData);
    }

    public void set(SetType type, List<Map<String, Object>> data) {
        mSetDataObject.put(type.name(), data);
    }

    public void setIds(Map<String, Object> data) {
        ArrayList<Map<String, Object>> arr = new ArrayList<>();
        arr.add(data);
        setIds(arr);
    }

    public void setIds(ArrayList<Map<String, Object>> data) {
        mIds = data;
    }

    public void setConsent(String data) {
        setConsent(new String[]{data});
    }

    public void setConsent(String[] data) {
        SharedPreferences preferences = mContext.getSharedPreferences(SPIdentifier, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(SPConsentKey, new HashSet<>(Arrays.asList(data)));
        editor.apply();
    }

    public String[] getConsent() {
        SharedPreferences preferences = mContext.getSharedPreferences(SPIdentifier, Context.MODE_PRIVATE);
        Set<String> set = preferences.getStringSet(SPConsentKey, null);
        String[] arr = new String[set.size()];
        set.toArray(arr);

        return arr;
    }

    public void sendScreenView(String screenName) {
        sendScreenView(screenName, null, null);
    }

    public void sendScreenView(String screenName, String className) {
        sendScreenView(screenName, className, null);
    }

    public void sendScreenView(String screenName, Map<String, Object> data) {
        sendScreenView(screenName, null, data);
    }

    public void sendScreenView(String screenName, String className, Map<String, Object> data) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put("sn", screenName);
        data.put("cn", className);
        send(HitType.SCREENVIEW, null, data);
    }

    public void sendPageView(String url, String title) {
        sendPageView(url, title, null, null);
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
        send(HitType.PAGEVIEW, null, data);
    }

    public void sendEvent(String category) {
        sendEvent(category, null, null, null, null);
    }

    public void sendEvent(String category, String action) {
        sendEvent(category, action, null, null, null);
    }

    public void sendEvent(String category, String action, String label) {
        sendEvent(category, action, label, null, null);
    }

    public void sendEvent(String category, String action, String label, Double value) {
        sendEvent(category, action, label, value, null);
    }

    public void sendEvent(String category, String action, String label, Double value, Map<String, Object> data) {
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
            data.put("value", String.format(Locale.ENGLISH, "%.2f", value));
        }
        send(HitType.EVENT, data);
    }

    public void sendAggregate(String scope, String name, AggregateType aggregateType, Double value) {
        Map<String, Object> actionData = new HashMap<>();
        if (!TextUtils.isEmpty(scope)) {
            actionData.put("scope", scope);
        }

        if (!TextUtils.isEmpty(name)) {
            actionData.put("name", name);
        }

        actionData.put("value", String.format(Locale.ENGLISH, "%.1f", value));
        actionData.put("aggregate", aggregateTypeToString(aggregateType));

        send(HitType.AGGREGATE, actionData);
    }

    public void send(HitType type) {
        send(type, null, null);
    }

    public void send(HitType type, Map<String, Object> data) {
        send(type, null, data);
    }

    public void send(HitType type, String actionType,
                     Map<String, Object> data) {
        if (type == HitType.AGGREGATE) {
            throw new IllegalArgumentException("Please use sendAggregate() instead of send(HitType.Aggregate, ...)");
        }

        if (mIsInitialized) {
            mInstance.sendEventToQueue(type, actionType, data);
        } else {
            throw new IllegalArgumentException("Initialize OSB Tracker first with: OSB osb = OSB.getInstance(); osb.config(...);");
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

    /* Deprecated Functions */

    /**
     * @deprecated This enum is renamed to 'HitType'
     * <p> Use {@link HitType} instead. </p>
     */
    public enum EventType {
        IDS, SOCIAL, EVENT, ACTION, EXCEPTION, PAGEVIEW, SCREENVIEW, TIMING
    }

    /**
     * @deprecated This method is renamed to 'config'
     * <p> Use {@link OSB#config(Context, String, String, String)} instead. </p>
     */
    public void create(Context context, String accountId, String url) {
        config(context, accountId, url, null);
    }

    /**
     * @deprecated This method is renamed to 'config'
     * <p> Use {@link OSB#config(Context, String, String, String)} instead. </p>
     */
    public void create(Context context, String accountId, String url, String siteId) {
        config(context, accountId, url, siteId);
    }

    /* Private Functions */
    private void startGpsTracker() {
        if (mContext == null) {
            return;
        }

        if (mGpsTracker == null) {
            mGpsTracker = new GpsTracker(mContext);
        }

        mGpsTracker.startTracker();
    }

    private void sendEventToQueue(OSB.HitType type, String actionType,
                                  Map<String, Object> data) {
        if (mQueue != null) {
            this.startGpsTracker();

            final Event event = new Event(type, actionType, data,
                    mGpsTracker.canGetLocation(), mGpsTracker.getLatitude(), mGpsTracker.getLongitude());
            Thread t = new Thread(new Runnable() {
                public void run() {
                    JsonGenerator generator = new JsonGenerator(mContext);
                    JSONObject jsonData = generator.generate(mConfig, event, mEventKey, mEventData,
                            mHitsData, getConsent(), getViewId(event), mIds, mSetDataObject);
                    mQueue.addToQueue(mConfig.getServerUrl(), jsonData);
                }
            });

            t.start();
        }
    }

    private String calculateViewId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String getViewId(Event event) {
        if (event.getType() == HitType.PAGEVIEW) {
            mViewId = calculateViewId();
        }
        return mViewId;
    }

    private String aggregateTypeToString(AggregateType aggregateType) {
        switch (aggregateType) {
            case MAX:
                return "max";
            case MIN:
                return "min";
            case SUM:
                return "sum";
            case COUNT:
                return "count";
            case AVERAGE:
                return "avg";
            default:
                return "";
        }
    }
}

