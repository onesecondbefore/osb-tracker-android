package com.onesecondbefore.tracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

final class JsonGenerator {
    private static final String TAG = "OSB:Json";

    private final Context mContext;
    private Map<String, Object> mSetDataObject;

    JsonGenerator(Context context) {
        this.mContext = context;
    }

    public JSONObject generate(Config config, Event event, String eventKey,
                               Map<String, Object> eventData, Map<String, Object> hitsData, String[] consent, String viewId, ArrayList<Map<String, Object>> ids, Map<String, Object> setDataObject, String idfa, String idfv, String cduid) {

        mSetDataObject = setDataObject;

        // Get Hits Info
        JSONObject hitJson = getHitsInfo(event, hitsData);
        JSONArray hits = new JSONArray();
        hits.put(hitJson);

        JSONObject eventJson = new JSONObject();
        try {
            eventJson.put("sy", getSystemInfo(config, event));
            eventJson.put("dv", getDeviceInfo(event, idfa, idfv, cduid));
            eventJson.accumulate("hits", hits);
            eventJson.put("pg", getPageInfo(viewId));
            eventJson.put("consent", new JSONArray(Arrays.asList(consent)));
            eventJson.put("ids", new JSONArray(ids));


            if (eventKey != null && !eventKey.isEmpty() && eventData != null && eventData.size() > 0) {
                JSONObject eventDataJson = new JSONObject();
                for (Map.Entry<String, Object> entry : eventData.entrySet()) {
                    eventDataJson.put(entry.getKey(), entry.getValue());
                }
                eventJson.put(eventKey, eventDataJson);
            }
        } catch (JSONException e) {
            Log.e(TAG, "generate - " + e.getMessage());
        }

        Log.i(TAG, eventJson.toString());
        return eventJson;
    }

    /* Private Functions */

    private List<Map<String, Object>> getSetDataForType(OSB.SetType type) {
        if (mSetDataObject == null) {
            return null;
        }
        try {
            return (List<Map<String, Object>>) mSetDataObject.get(type.name());
        } catch (ClassCastException e) {
            return null;
        }
    }

    private JSONObject getHitsInfo(Event event, Map<String, Object> hitsData) {
        JSONObject hitObj = new JSONObject();
        JSONObject dataObj = new JSONObject();

        try {
            // Always add page, action & item data ^MB
            List<Map<String, Object>> pageData = getSetDataForType(OSB.SetType.PAGE);
            if (pageData != null) {
                for (Map<String, Object> page : pageData) {
                    for (Map.Entry<String, Object> entry : page.entrySet()) {
                        if (!isSpecialKey(entry.getKey(), OSB.HitType.PAGEVIEW)) {
                            dataObj.put(entry.getKey(), entry.getValue());
                        } // If it's a special key we will have added it to the page (pg) object ^MB
                    }
                }
            }

            List<Map<String, Object>> actionData = getSetDataForType(OSB.SetType.ACTION);
            JSONObject actionJsonObj = new JSONObject();
            if (actionData != null) {
                for (Map<String, Object> actionObj : actionData) {
                    if (actionObj != null) {
                        for (Map.Entry<String, Object> entry : actionObj.entrySet()) {
                            if (isSpecialKey(entry.getKey(), OSB.HitType.ACTION)) {
                                actionJsonObj.put(entry.getKey(), entry.getValue());
                            } else {
                                dataObj.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                }
            }
            if (actionJsonObj.length() > 0) {
                hitObj.put("action", actionJsonObj);
            }

            List<Map<String, Object>> itemData = getSetDataForType(OSB.SetType.ITEM);
            if (itemData != null) {
                hitObj.put("items", new JSONArray(itemData));
            }

            switch (event.getType()) {
                case EVENT:
                    List<Map<String, Object>> eventData = getSetDataForType(OSB.SetType.EVENT);
                    if (eventData != null) {
                        for (Map<String, Object> eventObj : eventData) {
                            for (Map.Entry<String, Object> entry : eventObj.entrySet()) {
                                if (isSpecialKey(entry.getKey(), OSB.HitType.EVENT)) {
                                    hitObj.put(entry.getKey(), entry.getValue());
                                } else {
                                    dataObj.put(entry.getKey(), entry.getValue());
                                }
                            }
                        }
                    }
                    break;
                default:
                    break;
            }

            // Add/Overwrite all data that was added with the send command. ^MB
            if (event.getData() != null) {
                for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (isSpecialKey(key, event.getType())) {
                        hitObj.put(key, value);
                    } else {
                        dataObj.put(key, value);
                    }
                }
            }

            hitObj.put("tp", event.getTypeData());
            hitObj.put("ht", System.currentTimeMillis());

            if (hitsData != null && hitsData.size() > 0) {
                for (Map.Entry<String, Object> entry : hitsData.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (isSpecialKey(key, event.getType())) {
                        hitObj.put(key, value);
                    } else {
                        dataObj.put(key, value);
                    }
                }
            }

            if (dataObj.length() > 0) {
                hitObj.put("data", dataObj);
            }

        } catch (JSONException e) {
            Log.e(TAG, "getHitsInfo - " + e.getMessage());
        }

        return hitObj;
    }

    private JSONObject getSystemInfo(Config config, Event event) {
        JSONObject json = new JSONObject();
        try {
            json.put("st", System.currentTimeMillis());
            json.put("tv", "7.3." + BuildConfig.gitCommitIdAbbrev);
            json.put("cs", 0);
            json.put("is", hasValidGeoLocation(event) ? 0 : 1);
            json.put("aid", config.getAccountId());
            json.put("sid", config.getSiteId());
            json.put("tt", "android-post");
        } catch (JSONException e) {
            Log.e(TAG, "getSystemInfo - " + e.getMessage());
        }

        return json;
    }

    private JSONObject getDeviceInfo(Event event, String idfa, String idfv, String cduid) {
        JSONObject json = new JSONObject();
        try {
            Point size = this.getWindowSize();

            json.put("sw", size.x);
            json.put("sh", size.y);
            json.put("tz", this.getTimeZoneOffset());
            json.put("lang", this.getLanguage());
            json.put("idfa", idfa);
            json.put("idfv", idfv);
            json.put("cduid", cduid);
            json.put("conn", this.getNetworkType());
            json.put("mem", this.getDiskFreeMem());

            if (hasValidGeoLocation(event)) {
                JSONObject geoJson = new JSONObject();
                geoJson.put("latitude", event.getLatitude());
                geoJson.put("longitude", event.getLongitude());
                json.put("geo", geoJson);
            }
        } catch (JSONException e) {
            Log.e(TAG, "getDeviceInfo - " + e.getMessage());
        }

        return json;
    }

    private JSONObject getPageInfo(String viewId) {
        JSONObject json = new JSONObject();
        try {
            json.put("vid", viewId);
            // Make sure to only add 'special' page keys from the set(page) data, other data will be added to hits->data object. ^MB
            List<Map<String, Object>> pageData = getSetDataForType(OSB.SetType.PAGE);
            if (pageData != null) {
                for (Map<String, Object> page : pageData) {
                    for (Map.Entry<String, Object> entry : page.entrySet()) {
                        if (isSpecialKey(entry.getKey(), OSB.HitType.PAGEVIEW)) {
                            json.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "getPageInfo - " + e.getMessage());
        }

        return json;
    }

    private Point getWindowSize() {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        return size;
    }

    private int getTimeZoneOffset() {
        TimeZone tz = TimeZone.getDefault();
        return tz.getRawOffset() / (1000 * 60);
    }

    private String getLanguage() {
        Locale locale = mContext.getResources().getConfiguration().locale;
        return locale.getLanguage() + "-" + locale.getCountry();
    }


    private String getNetworkType() {
        String type = "offline";
        try {
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                    type = "wifi";
                } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                    type = "cellular";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getNetworkType - " + e.getMessage());
        }

        return type;
    }


    private long getDiskFreeMem() {
        long freeMem = 0;
        try {
            StatFs stat = new StatFs(Environment.getRootDirectory().getPath());
            if (Build.VERSION.SDK_INT >= 18) {
                freeMem = (stat.getAvailableBlocksLong() * stat.getBlockSizeLong());
            } else {
                // Noinspection deprecation
                int blockSizeInternal = stat.getBlockSize();
                int availBlocksInternal = stat.getAvailableBlocks();
                freeMem = ((long) availBlocksInternal * (long) blockSizeInternal);
            }
        } catch (Exception e) {
            Log.e(TAG, "getDiskUsed - " + e.getMessage());
        }

        return freeMem;
    }

    private Boolean isSpecialKey(String key, OSB.HitType hitType) {
        switch (hitType) {
            case EVENT:
                return key.equals("category") || key.equals("value") || key.equals("label") || key.equals("action") || key.equals("interaction");
            case AGGREGATE:
                return key.equals("scope") || key.equals("name") || key.equals("value") || key.equals("aggregate");
            case SCREENVIEW:
                return key.equals("sn") || key.equals("cn");
            case PAGEVIEW:
                return key.equals("title") || key.equals("id") || key.equals("url") || key.equals("ref") || key.equals("osc_id") || key.equals("osc_label") || key.equals("oss_keyword") || key.equals("oss_category") || key.equals("oss_total_results") || key.equals("oss_results_per_page") || key.equals("oss_current_page");
            case ACTION:
                return key.equals("tax") || key.equals("id") || key.equals("discount") || key.equals("currencyCode") || key.equals("revenue") || key.equals("currency_code") || key.equals("shipping");
            default:
                return false;
        }
    }

    private Boolean hasValidGeoLocation(Event event) {
        return (event.isGpsEnabled() && event.getLatitude() != 0 && event.getLongitude() != 0);
    }
}
