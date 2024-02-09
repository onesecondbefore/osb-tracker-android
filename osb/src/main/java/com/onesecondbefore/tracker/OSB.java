package com.onesecondbefore.tracker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
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

public final class OSB implements DefaultLifecycleObserver {
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
    private WebView mWebView;
    private AlertDialog mConsentDialog;


    private static final String SPIdentifier = "osb-shared-preferences";
    private static final String SPConsentKey = "osb-consent";
    private static final String SPConsentExpirationKey = "osb-consent-expiration";
    private static final String SPCDUIDKey = "osb-cduid";


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

    private void hideConsentWebview() {
        mConsentDialog.dismiss();
//        if (mWebView != null && mWebView.getParent() != null && mActivity != null) {
//            mActivity.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    ((ViewGroup) mWebView.getParent()).removeView(mWebView);
//                    ((ViewGroup) mWebView.getParent()).invalidate();
//                }
//            });
//
//        }
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

    private void addObservers() {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "App in foreground");
        startGpsTracker();

        if (mQueue != null) {
            new Thread( new Runnable() { @Override public void run() {
                mQueue.resume();
            } } ).start();
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "App in background");
        if (mGpsTracker != null) {
            mGpsTracker.stopTracker();
        }

        if (mQueue != null) {
            mQueue.pause();
        }
    }

    public void config(Context context, String accountId, String url, String siteId) {
        clear();

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // Get a handler that can be used to post to the main thread
        Handler mainHandler = new Handler(context.getMainLooper());
        Runnable addObserversRunnable = new Runnable() {
            @Override
            public void run() {
                addObservers();
            }
        };
        mainHandler.post(addObserversRunnable);

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

    public void showConsentWebview(Activity activity, Boolean forceShow) {
        if (forceShow || shouldShowConsentWebview()){
            mWebView = new WebView(activity);
            mWebView.addJavascriptInterface(this, "osbCmpMessageHandler");

            WebSettings webSettings = mWebView.getSettings();
            webSettings.setDomStorageEnabled(true);
            webSettings.setJavaScriptEnabled(true);

            mWebView.loadUrl(getConsentWebviewURL());
            mWebView.loadUrl("https://tweakers.net");
            mWebView.setWebViewClient(new OSBWebViewClient());

//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
//            mWebView.setLayoutParams(params);
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            mConsentDialog = builder.setView(mWebView).show();

//            Window window = mDialog.getWindow();
//            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        }
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
        Set<String> set = preferences.getStringSet(SPConsentKey, new HashSet<String>());
        String[] arr = new String[set.size()];
        set.toArray(arr);

        return arr;
    }

    private void processConsentCallback(String consentCallbackString) {
        hideConsentWebview();

        try {
            JSONObject json = convertConsentCallbackToJSON(consentCallbackString);
            if (json != null) {
                JSONObject consent = json.getJSONObject("consent");
                String consentString = consent.getString("tcString");
                setConsent(consentString);

                int expirationDate = json.getInt("expirationDate");
                setConsentExpiration(expirationDate);
                String cduid = json.getString("cduid");
                setCDUID(cduid);
            }
        } catch (Throwable t) {
            Log.e(TAG, "OSB Error: Could not parse consent JSON.");
            Log.e(TAG, t.getMessage());
        }
    }

    private JSONObject convertConsentCallbackToJSON(String consentCallbackString) {
        try {
            JSONObject obj = new JSONObject(consentCallbackString);
            return obj;
        } catch (Throwable t) {
            Log.e(TAG, "OSB Error: Could not parse consentCallbackString to JSON.");
            Log.e(TAG, t.getMessage());
            return null;
        }
    }

    private String getOSBSDKVersion() {
        return BuildConfig.versionName;
    }

    private void setConsentExpiration(Integer timestamp) {
        SharedPreferences preferences = mContext.getSharedPreferences(SPIdentifier, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(SPConsentExpirationKey, timestamp);
        editor.apply();
    }

    private Integer getConsentExpiration() {
        SharedPreferences preferences = mContext.getSharedPreferences(SPIdentifier, Context.MODE_PRIVATE);
        return preferences.getInt(SPConsentExpirationKey, 0);
    }

    private Boolean shouldShowConsentWebview() {
        if (getConsentExpiration() != null && getConsentExpiration() != 0 && getConsentExpiration() > System.currentTimeMillis()) {
            return false;
        }
        return true;
    }

    private void setCDUID(String cduid) {
        SharedPreferences preferences = mContext.getSharedPreferences(SPIdentifier, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SPCDUIDKey, cduid);
        editor.apply();
    }

    private String getCDUID() {
        SharedPreferences preferences = mContext.getSharedPreferences(SPIdentifier, Context.MODE_PRIVATE);
        return preferences.getString(SPCDUIDKey, null);
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
        remove("page");
        send(HitType.SCREENVIEW, null, data);
    }

    public void sendPageView(String url, String title) {
        sendPageView(url, title, null);
    }

    public void sendPageView(String url, String title, String referrer) {
        sendPageView(url, title, referrer, null, null, null, null, null, null, null, null, null);
    }

    public void sendPageView(String url, String title, String referrer, String id) {
        sendPageView(url, title, referrer, null, id, null, null, null, null, null, null, null);
    }

    public void sendPageView(String url, String title, String referrer, Map<String, Object> data, String id, String osc_id, String osc_label, String oss_keyword, String oss_category, String oss_total_results, String oss_results_per_page, String oss_current_page) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put("url", url);
        data.put("title", title);
        data.put("ref", referrer);
        data.put("id", id);
        data.put("osc_id", osc_id);
        data.put("osc_label", osc_label);
        data.put("oss_keyword", oss_keyword);
        data.put("oss_category", oss_category);
        data.put("oss_total_results", oss_total_results);
        data.put("oss_results_per_page", oss_results_per_page);
        data.put("oss_current_page", oss_current_page);

        send(HitType.PAGEVIEW, null, data);

        // Store data object for next send() ^MB
        set(SetType.PAGE, data);
    }
    public void sendPageView(String url, String title, String referrer, Map<String, Object> data) {
        if (data == null) {
            data = new HashMap<>();
        }
        data.put("url", url);
        data.put("title", title);
        data.put("ref", referrer);
        send(HitType.PAGEVIEW, null, data);

        // Store data object for next send() ^MB
        set(SetType.PAGE, data);
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
        sendEvent(category, action, label, value, data, null);
    }
    public void sendEvent(String category, String action, String label, Double value, Map<String, Object> data, Boolean interaction) {
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
        if (interaction != null) {
            data.put("interaction", interaction.toString());
        }

        send(HitType.EVENT, data);
    }

    public void sendAggregateEvent(String scope, String name, AggregateType aggregateType, Double value) {
        sendAggregate(scope, name, aggregateType, value);
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
//        if (type == HitType.AGGREGATE) {
//            throw new IllegalArgumentException("Please use sendAggregate() instead of send(HitType.Aggregate, ...)");
//        }

        if (mIsInitialized) {
            mInstance.sendEventToQueue(type, actionType, data);
            removeHitScope();
        } else {
            throw new IllegalArgumentException("Initialize OSB Tracker first with: OSB osb = OSB.getInstance(); osb.config(...);");
        }
    }

    public void remove() {
        remove("action");
        remove("event");
        remove("item");
        remove("page");
        remove("hits");
        remove("ids");
    }

    public void remove(String type) {
        switch (type) {
            case "action":
                set(SetType.ACTION, (List<Map<String, Object>>) null);
                break;
            case "event":
                mEventData = null;
                break;
            case "item":
                set(SetType.ITEM, (List<Map<String, Object>>) null);
                break;
            case "page":
                set(SetType.PAGE, (List<Map<String, Object>>) null);
                break;
            case "hits":
                mHitsData = null;
                break;
            case "ids":
                mIds = null;
                break;
            default:
                break;
        }
    }
    /* Deprecated Functions */

    /**
     * @deprecated This enum is renamed to 'HitType'
     * <p> Use {@link HitType} instead. </p>
     */
    @Deprecated
    public enum EventType {
        IDS, SOCIAL, EVENT, ACTION, EXCEPTION, PAGEVIEW, SCREENVIEW, TIMING
    }

    /**
     * @deprecated This method is renamed to 'config'
     * <p> Use {@link OSB#config(Context, String, String, String)} instead. </p>
     */
    @Deprecated
    public void create(Context context, String accountId, String url) {
        config(context, accountId, url, null);
    }

    /**
     * @deprecated This method is renamed to 'config'
     * <p> Use {@link OSB#config(Context, String, String, String)} instead. </p>
     */
    @Deprecated
    public void create(Context context, String accountId, String url, String siteId) {
        config(context, accountId, url, siteId);
    }

    /**
     * @deprecated This method is renamed to 'config'
     * <p> Use {@link OSB#remove()} instead. </p>
     */
    @Deprecated
    public void reset() {
        remove();
    }

    /* Private Functions */
    private void removeHitScope() {
        mEventData = null;
        mHitsData = null;
        mIds = null;
        set(SetType.ITEM, (Map<String, Object>) null);
        set(SetType.ACTION, (Map<String, Object>) null);

        List<Map<String, Object>> pageData = (List<Map<String, Object>>) mSetDataObject.get("PAGE");
        if (pageData != null) {
            var page = pageData.get(0);
            if (page != null) {
                page.put("oss_category", null);
                page.put("oss_keyword", null);
                page.put("oss_total_results", null);
                page.put("oss_results_per_page", null);
                page.put("oss_current_page", null);
                page.put("osc_id", null);

                // Should these two be implemented as well? ^MB
                page.put("onsite_search", null);
                page.put("onsite_campaign", null);

                set(SetType.PAGE, page);
            }
        }
    }

    private String getAdvertisingClientId() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return null;
        }

        try {
            AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(mContext);
            return adInfo != null ? adInfo.getId() : null;
        } catch (Exception e) {
            Log.e(TAG, "getAdvertisingClientId - " + e.getMessage());
        }

        return null;
    }

    @SuppressLint("HardwareIds")
    private String getUniqueId() {
        return Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private String getUserUID()  {
        if (getCDUID() != null) {
            return getCDUID();
        }

        if (getAdvertisingClientId() != null) {
            return getAdvertisingClientId();
        }

        if (getUniqueId() != null) {
            return getUniqueId();
        }

        Log.e(TAG, "OSB Error: could not get userUID");

        return "";
    }

    private String getConsentWebviewURL() {
        String consent = "";
        if (getConsent().length > 0){
            consent = getConsent()[0];
        }
        var siteIdURL = "&sid=" + mConfig.getSiteId();
        String urlString = mConfig.getServerUrl() + "/consent?aid=" + mConfig.getAccountId() + siteIdURL + "&type=app&show=true&version=" + getOSBSDKVersion() + "&consent=" + consent + "&cduid=" + getUserUID();
        return urlString;
    }

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
                            mHitsData, getConsent(), getViewId(event), mIds, mSetDataObject, getAdvertisingClientId(), getUniqueId(), getCDUID());
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
        if (event.getType() == HitType.PAGEVIEW || event.getType() == HitType.SCREENVIEW) {
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

    @SuppressWarnings("unused")
    @JavascriptInterface
    public void postMessage(String consentCallbackString) {
        processConsentCallback(consentCallbackString);
    }

    private class OSBWebViewClient extends WebViewClient{
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
           Log.d(TAG, description);
        }
    }
}

