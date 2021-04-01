package com.onesecondbefore.tracker;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
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
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

final class JsonGenerator {
    private static final String TAG = "OSB:Json";

    private class AppInfo {
        public String name = null;
        public String version = null;
    }

    private Context mContext;

    JsonGenerator(Context context) {
        this.mContext = context;
    }

    public JSONObject generate(Config config, Event event) {
        // Get System Info
        JSONObject sysInfoJson = getSystemInfo(config, event);

        // Get Device Info
        JSONObject deviceInfoJson = getDeviceInfo(event);

        // Get Hits Info
        JSONObject hitJson = getHitsInfo(event);
        JSONArray hits = new JSONArray();
        hits.put(hitJson);

        JSONObject eventJson = new JSONObject();
        try {
            eventJson.put("sy", sysInfoJson);
            eventJson.put("dv", deviceInfoJson);
            eventJson.accumulate("hits", hits);
        } catch(Exception e) {
            Log.e(TAG, "generate - " + e.getMessage());
        }

        Log.i(TAG, eventJson.toString());
        return eventJson;
    }

    /* Private Functions */

    private JSONObject getHitsInfo(Event event) {
        JSONObject json = new JSONObject();

        String type = event.getTypeData();
        String hitsType = event.getTypeDataKey();
        String[] defaultEventKeys = event.getDefaultEventKeys();
        List<String> defaultEventList = Arrays.asList(defaultEventKeys);
        Map<String, Object> data = event.getData();
        JSONObject hitsData = new JSONObject();
        JSONObject customData = new JSONObject();

        try {
            if (data != null) {
                for (Map.Entry<String, Object> entry:  data.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (defaultEventList.contains(key)) {
                        hitsData.put(key, value);
                    } else {
                        customData.put(key, value);
                    }
                }
            }
        } catch(Exception e) {
            Log.e(TAG, "getHitsInfo - " + e.getMessage());
        }

        try {
            json.put("tp", type);
            json.put(hitsType, hitsData);
            json.put("dt", customData);
            json.put("ht", getCurrentTimestamp());
        } catch(Exception e) {
            Log.e(TAG, "getHitsInfo - " + e.getMessage());
        }

        return json;
    }

    private JSONObject getSystemInfo(Config config, Event event) {
        JSONObject json = new JSONObject();
        try {
            json.put("st", this.getCurrentTimestamp());
            json.put("tv", "5.0.0." + BuildConfig.gitCommitIdAbbrev);
            json.put("cs", 0);
            json.put("is", 0);
            json.put("aid", config.getAccountId());
            json.put("ns", event.getNamespace());
            json.put("tt", "android-post");
        } catch(Exception e) {
            Log.e(TAG, "getDeviceInfo - " + e.getMessage());
        }

        return json;
    }

    private JSONObject getDeviceInfo(Event event) {
        JSONObject json = new JSONObject();
        try {
            String product =  Build.PRODUCT;
            boolean isEmulator = product.equals("sdk") || product.contains("_sdk") || product.contains("sdk_");

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
        } catch(Exception e) {
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

    private long getCurrentTimestamp() {
        Date date= new Date();
        return date.getTime();
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
        String[] simbols = new String[] {"B", "KB", "MB", "GB", "TB", "PB", "EB"};
        long scale = 1L;
        for (String simbol : simbols) {
            if (totalBytes < (scale * 1024L)) {
                return String.format("%s %s", new DecimalFormat("#.##").
                        format((double)totalBytes / scale), simbol);
            }
            scale *= 1024L;
        }
        return "-1 B";
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private long getAvailableBytes(StatFs stat) {
        return stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
    }
}
