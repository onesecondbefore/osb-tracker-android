package com.onesecondbefore.tracker;

import java.util.Map;
import java.util.Hashtable;

public class Event {
    private EventType mType = EventType.EVENT;
    private String mNamespace = "default";
    private Map<String, Object> mData = new Hashtable<>();
    private String mSubType = "";
    private boolean mIsGpsEnabled = false;
    private double mLatitude = 0;
    private double mLongitude = 0;

    Event(EventType type, String subType, Map<String, Object> data,
          boolean isGpsEnabled, double latitude, double longitude) {
        mType = type;
        mSubType = subType != null ? subType : "";
        mIsGpsEnabled = isGpsEnabled;
        mLatitude = latitude;
        mLongitude = longitude;

        if (data != null) {
            mData = data;
        }
    }

    public String getNamespace()  {
        return mNamespace;
    }

    public String getTypeValue() {
        String value = "event";
        if (mType == EventType.EVENT) {
            value = "event";
        } else if (mType == EventType.SCREENVIEW) {
            value = "screenview";
        } else if (mType == EventType.PAGEVIEW) {
            value = "pageview";
        } else if (mType == EventType.ACTION) {
            value = "action";
        } else if (mType == EventType.IDS) {
            value = "ids";
        } else if (mType == EventType.EXCEPTION) {
            value = "exception";
        } else if (mType == EventType.SOCIAL) {
            value = "social";
        } else if (mType == EventType.TIMING) {
            value = "timing";
        }

        return value;
    }

    public String getTypeData() {
        String data = getTypeValue();
        if (mType == EventType.ACTION) {
            data = mSubType;
        }

        return data;
    }

    public String getTypeDataKey() {
        // Get the type of the hits
        String key = "ev";
        if (mType == EventType.EVENT) {
            key = "ev";
        } else if (mType == EventType.SCREENVIEW) {
            key = "sv";
        } else if (mType == EventType.PAGEVIEW) {
            key = "pg";
        } else if (mType == EventType.ACTION) {
            key = "ac";
        } else if (mType == EventType.IDS) {
            key = "id";
        } else if (mType == EventType.EXCEPTION) {
            key = "ex";
        } else if (mType == EventType.SOCIAL) {
            key = "sc";
        } else if (mType == EventType.TIMING) {
            key = "ti";
        }

        return key;
    }

    public String[] getDefaultEventKeys() {
        String[] keys = new String[] {};
        if (mType == EventType.EVENT || mType == EventType.EXCEPTION ||
                mType == EventType.SOCIAL || mType == EventType.TIMING) {
            keys = new String[] { "category", "action", "label", "value" };
        } else if (mType == EventType.SCREENVIEW) {
            keys = new String[] { "id", "name" };
        } else if (mType == EventType.PAGEVIEW) {
            keys = new String[] { "id", "title", "viewId", "url", "referrer" };
        } else if (mType == EventType.ACTION) {
            keys = new String[] { "id", "tax", "discount", "currencyCode", "revenue" };
        } else if (mType == EventType.IDS) {
            keys = new String[] { "key", "value", "label" };
        }

        return keys;
    }

    public Map<String, Object> getData() {
        return mData;
    }

    public boolean isGpsEnabled() {
        return mIsGpsEnabled;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }
}
