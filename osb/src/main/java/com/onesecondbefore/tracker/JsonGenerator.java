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
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

final class JsonGenerator {
    private static final String TAG = "OSB:Json";

    private class AppInfo {
        public String name = null;
        public String version = null;
    }

    private final Context mContext;

    JsonGenerator(Context context) {
        this.mContext = context;
    }

    public JSONObject generate(Config config, Event event, String eventKey,
        Map<String, Object> eventData, Map<String, Object> hitsData) {
        // Get System Info
        JSONObject sysInfoJson = getSystemInfo(config, event);

        // Get Device Info
        JSONObject deviceInfoJson = getDeviceInfo(event);

        // Get Hits Info
        JSONObject hitJson = getHitsInfo(event, hitsData);
        JSONArray hits = new JSONArray();
        hits.put(hitJson);

        JSONObject eventJson = new JSONObject();
        try {
            eventJson.put("sy", sysInfoJson);
            eventJson.put("dv", deviceInfoJson);
            eventJson.accumulate("hits", hits);

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

    private JSONObject getSystemInfo(Config config, Event event) {
        JSONObject json = new JSONObject();
        try {
            json.put("st", System.currentTimeMillis());
            json.put("tv", "5.0.0." + BuildConfig.gitCommitIdAbbrev);
            json.put("cs", 0);
            json.put("is", 0);
            json.put("aid", config.getAccountId());
            json.put("ns", event.getNamespace());
            json.put("tt", "android-post");
        } catch (JSONException e) {
            Log.e(TAG, "getSystemInfo - " + e.getMessage());
        }

        return json;
    }

    private JSONObject getDeviceInfo(Event event) {
        JSONObject json = new JSONObject();
        try {
            String product = Build.PRODUCT;
            boolean isEmulator = product.equals("sdk");

            Point size = this.getWindowSize();
            AppInfo info = this.getAppInfo();

            json.put("os", "Android");
            json.put("ov", Build.VERSION.RELEASE);
            json.put("brand", Build.BRAND);
            json.put("model", isEmulator ? "Emulator" : Build.MODEL);
            json.put("sw", size.x);
            json.put("sh", size.y);
            json.put("tz", this.getTimeZoneOffset());
            json.put("lang", this.getLanguage());
            json.put("idfa", this.getAdvertisingClientId());
            json.put("idfv", this.getUniqueId());
            json.put("an", info.name);
            json.put("av", info.version);
            json.put("conn", this.getNetworkType());
            json.put("mem", this.getDiskUsed());

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

    private Point getWindowSize() {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        return size;
    }

    private AppInfo getAppInfo() {
        AppInfo info = new AppInfo();
        try {
            String packageName = this.mContext.getPackageName();
            PackageInfo pInfo = this.mContext.getPackageManager().getPackageInfo(packageName, 0);
            ApplicationInfo applicationInfo = this.mContext.getPackageManager().getApplicationInfo(packageName, 0);
            info.version = pInfo.versionName;
            info.name = (String)((applicationInfo != null) ?
                    this.mContext.getPackageManager().getApplicationLabel(applicationInfo) : "unknown");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getAppInfo - " + e.getMessage());
        }

        return info;
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
            return "";
        }

        String clientId = "";
        try {
            AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(mContext);
            clientId = adInfo != null ? adInfo.getId() : null;
        } catch (Exception e) {
            Log.e(TAG, "getAdvertisingClientId - " + e.getMessage());
        }

        return clientId;
    }

    private String getDiskUsed() {
        long usedMem = 0;
        try {
            StatFs stat = new StatFs(Environment.getRootDirectory().getPath());
            if(Build.VERSION.SDK_INT >= 18){
                long totalMem = (stat. getBlockCountLong() * stat.getBlockSizeLong());
                long freeMem = (stat. getAvailableBlocksLong() * stat.getBlockSizeLong());
                usedMem = totalMem - freeMem;
            }
            else {
                // Noinspection deprecation
                int totalBlocksInternal = stat.getBlockCount();
                int blockSizeInternal = stat.getBlockSize();
                int availBlocksInternal = stat.getAvailableBlocks();
                long totalMem = ((long)totalBlocksInternal *  (long)blockSizeInternal);
                long freeMem = ((long)availBlocksInternal *  (long)blockSizeInternal);
                usedMem = totalMem - freeMem;
            }
        } catch (Exception e) {
            Log.e(TAG, "getDiskUsed - " + e.getMessage());
        }

        return convertBytesToString(usedMem);
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
