package com.onesecondbefore.tracker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.preference.PreferenceManager;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.iabtcf.decoder.TCString;
import com.iabtcf.exceptions.TCStringDecodeException;

public final class OSB implements DefaultLifecycleObserver {
    private static final String TAG = "OSB:Api";
    private static OSB mInstance = null;
    private Config mConfig = new Config();
    private GpsTracker mGpsTracker = null;
    private ApiQueue mQueue = null;
    private Context mContext;
    private boolean mIsInitialized = false;
    private boolean hasLocationConsent = false;
    private String mViewId = calculateViewId();
    private String mEventKey = null;
    private Map<String, Object> mEventData = null;
    private Map<String, Object> mHitsData = null;
    private Map<String, Object> mSetDataObject = new HashMap<>();
    private ArrayList<Map<String, Object>> mIds = new ArrayList<>();
    private WebView mWebView;
    private AlertDialog mConsentDialog;



    private OnGoogleConsentModeCallback onGoogleConsentModeCallback;
    public interface OnGoogleConsentModeCallback {
        public void onGoogleConsentMode(Map<String, String> consent);
    }

    private static final String SPIdentifier = "osb-shared-preferences";
    private static final String SPConsentKey = "osb-consent";
    private static final String SPConsentExpirationKey = "osb-consent-expiration";
    private static final String SPCDUIDKey = "osb-cduid";
    private static final String SPRemoteCmpKey = "osb-remote-cmp";
    private static final String SPLocalCmpKey = "osb-local-cmp";
    private static final String SPCmpCheckTimestamp = "osb-cmp-check-timestamp";
    private static final String SPGoogleConsentKey = "osb-cmp-google-consent";



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

        fetchRemoteCmpVersion();

        mQueue = new ApiQueue(mContext);
        startGpsTracker();

        mIsInitialized = true;
        Log.i(TAG, "OSB - Initialized");
    }

    public void addGoogleConsentCallback(OnGoogleConsentModeCallback callBack) {
        this.onGoogleConsentModeCallback = callBack;
    }

    public void debug(boolean isEnabled) {
        mConfig.setDebug(isEnabled);
    }

    public void showConsentWebview(Activity activity, Boolean forceShow) {
        if (forceShow || shouldShowConsentWebview()){
            View view = LayoutInflater.from(activity).inflate(R.layout.osb_webview, null);
            mWebView = view.findViewById(R.id.osbwebview);
            mWebView.addJavascriptInterface(this, "osbCmpMessageHandler");

            WebSettings webSettings = mWebView.getSettings();
            webSettings.setDomStorageEnabled(true);
            webSettings.setJavaScriptEnabled(true);

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            mConsentDialog = builder.setView(view.findViewById(R.id.osbwebviewlayout)).show();

            mWebView.loadUrl(getConsentWebviewURL());
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

    public boolean shouldResurfaceCmp() {
        if (getRemoteCmpVersion() > getLocalCmpVersion()) {
            setLocalCmpVersion(getRemoteCmpVersion());
            return true;
        }

        return false;
    }

    public Map<String, String> getGoogleConsentPayload() {

        return getGoogleConsentMode();
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


    private void processConsentCallback(String consentCallbackString) {
        hideConsentWebview();

        try {
            JSONObject json = convertConsentCallbackToJSON(consentCallbackString);
            if (json != null) {
                JSONObject consent = json.getJSONObject("consent");
                String consentString = consent.getString("tcString");
                setConsent(consentString);

                Long expirationDate = json.getLong("expirationDate");
                setConsentExpiration(expirationDate);
                String cduid = json.getString("cduid");
                setCDUID(cduid);


                JSONArray purposes = consent.getJSONArray("purposes");
                ArrayList<Integer> purposesList = new ArrayList<>();
                for (int i = 0; i < purposes.length(); i++) {
                    purposesList.add(purposes.getInt(i));
                }

                Map<String, String> consentMode = mapConsentMode(purposesList);
                setGoogleConsentMode(consentMode);

                if (this.onGoogleConsentModeCallback != null) {
                    onGoogleConsentModeCallback.onGoogleConsentMode(consentMode);
                }

                decodeAndStoreIABConsent(consentString);
                setLocalCmpVersion(getRemoteCmpVersion());

                JSONArray specialFeatures = consent.getJSONArray("specialFeatures");
                ArrayList<Integer> specialFeaturesList = new ArrayList<>();
                for (int i = 0; i < specialFeatures.length(); i++) {
                    specialFeaturesList.add(specialFeatures.getInt(i));
                }
                processSpecialFeatures(specialFeaturesList);
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

    private void setConsentExpiration(Long timestamp) {
        SharedPreferences preferences = mContext.getSharedPreferences(SPIdentifier, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(SPConsentExpirationKey, timestamp);
        editor.apply();
    }

    private Long getConsentExpiration() {
        SharedPreferences preferences = mContext.getSharedPreferences(SPIdentifier, Context.MODE_PRIVATE);
        return preferences.getLong(SPConsentExpirationKey, 0);
    }

    private Boolean shouldShowConsentWebview() {
        if (shouldResurfaceCmp()) {
            return true;
        }
        return getConsentExpiration() < System.currentTimeMillis();
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

    private void processSpecialFeatures(ArrayList<Integer> specialFeatures) {
        this.hasLocationConsent = specialFeatures.contains(1);
    }

    private void setGoogleConsentMode(Map<String,String> consent) {
        SharedPreferences preferences = mContext.getSharedPreferences(SPIdentifier, Context.MODE_PRIVATE);
        if (preferences != null){
            JSONObject jsonObject = new JSONObject(consent);
            String jsonString = jsonObject.toString();
            preferences.edit()
                    .remove(SPGoogleConsentKey)
                    .putString(SPGoogleConsentKey, jsonString)
                    .apply();
        }
    }

    private Map<String,String> getGoogleConsentMode() {
        Map<String,String> outputMap = new HashMap<>();
        SharedPreferences preferences = mContext.getSharedPreferences(SPIdentifier, Context.MODE_PRIVATE);
        try {
            if (preferences != null) {
                String jsonString = preferences.getString(SPGoogleConsentKey, (new JSONObject()).toString());
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keysItr = jsonObject.keys();
                while (keysItr.hasNext()) {
                    String type = keysItr.next();
                    outputMap.put(type, jsonObject.getString(type));
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return outputMap;
    }

    private void setRemoteCmpVersion(int cmpVersion) {
        SharedPreferences preferences = mContext.getSharedPreferences(SPIdentifier, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(SPRemoteCmpKey, cmpVersion);
        editor.putLong(SPCmpCheckTimestamp, System.currentTimeMillis());
        editor.apply();
    }

    private int getRemoteCmpVersion() {
        SharedPreferences preferences = mContext.getSharedPreferences(SPIdentifier, Context.MODE_PRIVATE);
        return preferences.getInt(SPRemoteCmpKey, 0);
    }

    private void setLocalCmpVersion(int cmpVersion) {
        SharedPreferences preferences = mContext.getSharedPreferences(SPIdentifier, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(SPLocalCmpKey, cmpVersion);
        editor.apply();
    }

    private int getLocalCmpVersion() {
        SharedPreferences preferences = mContext.getSharedPreferences(SPIdentifier, Context.MODE_PRIVATE);
        return preferences.getInt(SPLocalCmpKey, 0);
    }

    private long getCmpCheckTimestamp() {
        SharedPreferences preferences = mContext.getSharedPreferences(SPIdentifier, Context.MODE_PRIVATE);
        return preferences.getLong(SPCmpCheckTimestamp, 0);
    }

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
                    hasLocationConsent && mGpsTracker.canGetLocation(), mGpsTracker.getLatitude(), mGpsTracker.getLongitude());

            String viewId = getViewId(event);
            Map<String, Object> setData = new HashMap<String, Object>(mSetDataObject);

            Thread t = new Thread(new Runnable() {
                public void run() {
                    JsonGenerator generator = new JsonGenerator(mContext);
                    JSONObject jsonData = generator.generate(mConfig, event, mEventKey, mEventData,
                            mHitsData, getConsent(), viewId, mIds, setData, getAdvertisingClientId(), getUniqueId(), getCDUID());
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

    private void processFetchedCmpResponse(String cmpResponse) {
        try {
            JSONObject json = convertCmpResponseToJSON(cmpResponse);
            if (json != null) {
                int cmpVersion = json.getInt("cmpVersion");
                setRemoteCmpVersion(cmpVersion);
            }
        } catch (Throwable t) {
            Log.e(TAG, "OSB Error: Could not parse consent JSON.");
            Log.e(TAG, t.getMessage());
        }
    }

    private JSONObject convertCmpResponseToJSON(String cmpResponse) {
        try {
            JSONObject obj = new JSONObject(cmpResponse);
            return obj;
        } catch (Throwable t) {
            Log.e(TAG, "OSB Error: Could not parse cmpResponse to JSON.");
            Log.e(TAG, t.getMessage());
            return null;
        }
    }

    private void fetchRemoteCmpVersion() {
        if (getCmpCheckTimestamp() + (24 * 60 * 60 * 1000) < System.currentTimeMillis()){
            requestRemoteCmpVersion();
        }
    }

    private void requestRemoteCmpVersion() {
        RequestQueue mRequestQueue = Volley.newRequestQueue(mContext);
        StringRequest mStringRequest = new StringRequest(Request.Method.GET, getCmpVersionUrl(), this::processFetchedCmpResponse, error -> Log.i(TAG,"OSB Error :" + error.toString()));
        mRequestQueue.add(mStringRequest);
    }

    private String getCmpVersionUrl() {
        final HashCode hashCode = Hashing.sha1().hashString(mConfig.getAccountId() + '-' + mConfig.getSiteId(), Charset.defaultCharset());
        String hash = hashCode.toString().substring(0, 8);
        return "https://cdn.onesecondbefore.com/cmp/"+hash+".json";
    }

    private void decodeAndStoreIABConsent(String consent) {
        SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = mPreferences.edit();
        try {
            TCString tcString = TCString.decode(consent);

            editor.putInt("IABTCF_CmpSdkID", tcString.getCmpId());
            editor.putInt("IABTCF_CmpSdkVersion", tcString.getCmpVersion());
            editor.putInt("IABTCF_PolicyVersion", tcString.getTcfPolicyVersion());
            editor.putInt("IABTCF_gdprApplies", 1);
            editor.putString("IABTCF_PublisherCC", tcString.getPublisherCC());
            editor.putInt("IABTCF_PurposeOneTreatment", tcString.getPurposeOneTreatment() ? 1 : 0);
            editor.putInt("IABTCF_UseNonStandardTexts", tcString.getUseNonStandardStacks() ? 1 : 0);
            editor.putString("IABTCF_TCString", consent);

            StringBuilder vendorConsents = new StringBuilder();
            StringBuilder vendorLegitimateInterests = new StringBuilder();
            StringBuilder purposeConsents = new StringBuilder();
            StringBuilder purposeLegitimateInterests = new StringBuilder();
            StringBuilder specialFeaturesOptIns = new StringBuilder();
            StringBuilder publisherConsents = new StringBuilder();
            StringBuilder publisherLegitimateInterests = new StringBuilder();
            StringBuilder publisherCustomPurposesConsents = new StringBuilder();
            StringBuilder publisherCustomPurposesLegitimateInterests = new StringBuilder();

            for (int i = 1; i <= 11; i++) { // TCF 2.2 contains 11 purposes.
                purposeConsents.append(tcString.getPurposesConsent().contains(i) ? "1" : "0");
                purposeLegitimateInterests.append(tcString.getPurposesLITransparency().contains(i) ? "1" : "0");
                publisherConsents.append(tcString.getPurposesConsent().contains(i) ? "1" : "0");
                publisherLegitimateInterests.append(tcString.getPubPurposesLITransparency().contains(i) ? "1" : "0");
                publisherCustomPurposesConsents.append(tcString.getCustomPurposesConsent().contains(i) ? "1" : "0");
                publisherCustomPurposesLegitimateInterests.append(tcString.getCustomPurposesLITransparency().contains(i) ? "1" : "0");
            }

            Set<Integer> vendorIds = tcString.getVendorConsent().toSet();
            int maxId = 0;
            for (int i: vendorIds) {
                if (maxId < i) {
                    maxId = i;
                }
            }
            for (int i = 1; i <= maxId; i++) {
                vendorConsents.append(tcString.getVendorConsent().contains(i) ? "1" : "0");
                vendorLegitimateInterests.append(tcString.getVendorLegitimateInterest().contains(i) ? "1" : "0");
            }

            for (int i = 1; i <= 2; i++) {
                specialFeaturesOptIns.append(tcString.getSpecialFeatureOptIns().contains(i) ? "1" : "0");
            }

            editor.putString("IABTCF_VendorConsents", vendorConsents.toString());
            editor.putString("IABTCF_VendorLegitimateInterests", vendorLegitimateInterests.toString());
            editor.putString("IABTCF_PurposeConsents", purposeConsents.toString());
            editor.putString("IABTCF_PurposeLegitimateInterests", purposeLegitimateInterests.toString());
            editor.putString("IABTCF_SpecialFeaturesOptIns", specialFeaturesOptIns.toString());
//            IABTCF_PublisherRestrictions{ID} TODO: implement. ^MB
            editor.putString("IABTCF_PublisherConsent", publisherConsents.toString());
            editor.putString("IABTCF_PublisherLegitimateInterests", publisherLegitimateInterests.toString());
            editor.putString("IABTCF_PublisherCustomPurposesConsents", publisherCustomPurposesConsents.toString());
            editor.putString("IABTCF_PublisherCustomPurposesLegitimateInterests", publisherCustomPurposesLegitimateInterests.toString());

        } catch (TCStringDecodeException ex) {
            editor.remove("IABTCF_TCString");
            editor.remove("IABTCF_CmpSdkID");
            editor.remove("IABTCF_CmpSdkVersion");
            editor.remove("IABTCF_PolicyVersion");
            editor.remove("IABTCF_PublisherCC");
            editor.remove("IABTCF_PurposeOneTreatment");
            editor.remove("IABTCF_UseNonStandardTexts");
            editor.remove("IABTCF_TCString");
            editor.remove("IABTCF_VendorConsents");
            editor.remove("IABTCF_VendorLegitimateInterests");
            editor.remove("IABTCF_PurposeConsents");
            editor.remove("IABTCF_PurposeLegitimateInterests");
            editor.remove("IABTCF_SpecialFeaturesOptIns");
            editor.remove("IABTCF_PublisherConsent");
            editor.remove("IABTCF_PublisherLegitimateInterests");
            editor.remove("IABTCF_PublisherCustomPurposesConsents");
            editor.remove("IABTCF_PublisherCustomPurposesLegitimateInterests");
        } finally {
            editor.commit();
        }
    }

    private Map<String, String> mapConsentMode(List<Integer> purposes) {
        Map<String, String> consent = new HashMap<>() {
            {
                put("AD_STORAGE", "DENIED");
                put("AD_USER_DATA", "DENIED");
                put("AD_PERSONALIZATION", "DENIED");
                put("ANALYTICS_STORAGE", "GRANTED");
//                put("FUNCTIONALITY_STORAGE", "GRANTED");
//                put("PERSONALIZATION_STORAGE", "GRANTED");
//                put("SECURITY_STORAGE", "GRANTED");
            }};

        if (purposes.contains(1)) {
            consent.put("AD_STORAGE", "GRANTED");
        }

        if (purposes.contains(1) && purposes.contains(7)) {
            consent.put("AD_USER_DATA", "GRANTED");
        }

        if (purposes.contains(3) && purposes.contains(4)) {
            consent.put("AD_PERSONALIZATION", "GRANTED");
        }

        return consent;
    }

    @SuppressWarnings("unused")
    @JavascriptInterface
    public void postMessage(String consentCallbackString) {
        processConsentCallback(consentCallbackString);
    }
}

interface OnLoginCompleteListener {
    void onLoginComplete(String response);
}