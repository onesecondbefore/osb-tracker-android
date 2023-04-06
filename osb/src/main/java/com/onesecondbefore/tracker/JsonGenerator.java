package com.onesecondbefore.tracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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

import com.google.android.gms.ads.identifier.AdvertisingIdClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

final class JsonGenerator {
    private static final String TAG = "OSB:Json";

    private final Context mContext;

    JsonGenerator(Context context) {
        this.mContext = context;
    }

    public JSONObject generate(Config config, Event event, String eventKey,
        Map<String, Object> eventData, Map<String, Object> hitsData, String[] consent, String viewId) {

        // Get Hits Info
        JSONObject hitJson = getHitsInfo(event, hitsData);
        JSONArray hits = new JSONArray();
        hits.put(hitJson);

//        // Get Page Info
//        JSONObject pageInfoJson = getPageInfo(event);
//        // Get Ids Info
//        JSONObject idsInfoJson = getIdsInfo(event);


        JSONObject eventJson = new JSONObject();
        try {
            eventJson.put("sy", getSystemInfo(config));
            eventJson.put("dv", getDeviceInfo(event));
            eventJson.accumulate("hits", hits);
            eventJson.put("pg", getPageInfo(viewId));
            eventJson.put("consent",  Arrays.toString(consent));
//            eventJson.put("ids", idsInfoJson);


            if (eventKey != null && !eventKey.isEmpty() && eventData != null && eventData.size() > 0) {
                JSONObject eventDataJson = new JSONObject();
                for (Map.Entry<String, Object> entry: eventData.entrySet()) {
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

    private JSONObject getHitsInfo(Event event, Map<String, Object> hitsData) {
        JSONObject json = new JSONObject();

        try {
            for (Map.Entry<String, Object> entry: event.getData().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                json.put(key, value);
            }
            json.put("tp", event.getTypeData());
            json.put("ht", System.currentTimeMillis());

            if (hitsData != null && hitsData.size() > 0) {
                for (Map.Entry<String, Object> entry: hitsData.entrySet()) {
                    json.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "getHitsInfo - " + e.getMessage());
        }

        return json;
    }

    private JSONObject getSystemInfo(Config config) {
        JSONObject json = new JSONObject();
        try {
            json.put("st", System.currentTimeMillis());
            json.put("tv", "6.0." + BuildConfig.gitCommitIdAbbrev);
            json.put("cs", 0);
            json.put("is", 0);
            json.put("aid", config.getAccountId());
            json.put("sid", config.getSiteId());
            json.put("ns", "default");
            json.put("tt", "android-post");
        } catch (JSONException e) {
            Log.e(TAG, "getSystemInfo - " + e.getMessage());
        }

        return json;
    }

    private JSONObject getDeviceInfo(Event event) {
        JSONObject json = new JSONObject();
        try {
            Point size = this.getWindowSize();

            json.put("sw", size.x);
            json.put("sh", size.y);
            json.put("tz", this.getTimeZoneOffset());
            json.put("lang", this.getLanguage());
            json.put("idfa", this.getAdvertisingClientId());
            json.put("idfv", this.getUniqueId());
            json.put("conn", this.getNetworkType());
            json.put("mem", this.getDiskFreeMem());

            if (event.isGpsEnabled() && event.getLatitude() != 0 && event.getLongitude() != 0) {
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
            json.put("view_id", viewId);
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
        return locale.getLanguage() + "_" + locale.getCountry();
    }

    @SuppressLint("HardwareIds")
    private String getUniqueId() {
        return Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
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

    private String getAdvertisingClientId() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return null;
        }

        String clientId = null;
        try {
            AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(mContext);
            clientId = adInfo != null ? adInfo.getId() : null;
        } catch (Exception e) {
            Log.e(TAG, "getAdvertisingClientId - " + e.getMessage());
        }

        return clientId;
    }

    private String getDiskFreeMem() {
        long freeMem = 0;
        try {
            StatFs stat = new StatFs(Environment.getRootDirectory().getPath());
            if (Build.VERSION.SDK_INT >= 18) {
                 freeMem = (stat. getAvailableBlocksLong() * stat.getBlockSizeLong());
            } else {
                // Noinspection deprecation
                int blockSizeInternal = stat.getBlockSize();
                int availBlocksInternal = stat.getAvailableBlocks();
                freeMem = ((long)availBlocksInternal *  (long)blockSizeInternal);
            }
        } catch (Exception e) {
            Log.e(TAG, "getDiskUsed - " + e.getMessage());
        }

        return convertBytesToString(freeMem);
    }

    public String convertBytesToString(long totalBytes) {
        String[] symbols = new String[] {"B", "KB", "MB", "GB", "TB", "PB", "EB"};
        long scale = 1L;
        for (String symbol : symbols) {
            if (totalBytes < (scale * 1024L)) {
                return String.format("%s %s", new DecimalFormat("#.##").
                        format((double)totalBytes / scale), symbol);
            }
            scale *= 1024L;
        }
        return "0 B";
    }
}
