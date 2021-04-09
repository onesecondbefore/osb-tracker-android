package com.onesecondbefore.tracker;

import java.util.Map;
import java.util.Hashtable;

public class Event {
    private OSB.EventType mType = OSB.EventType.EVENT;
    private String mNamespace = "default";
    private Map<String, Object> mData = new Hashtable<>();
    private String mSubType = "";
    private boolean mIsGpsEnabled = false;
    private double mLatitude = 0;
    private double mLongitude = 0;

    Event(OSB.EventType type, String subType, Map<String, Object> data,
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
        if (mType == OSB.EventType.EVENT) {
            value = "event";
        } else if (mType == OSB.EventType.SCREENVIEW) {
            value = "screenview";
        } else if (mType == OSB.EventType.PAGEVIEW) {
            value = "pageview";
        } else if (mType == OSB.EventType.ACTION) {
            value = "action";
        } else if (mType == OSB.EventType.IDS) {
            value = "ids";
        } else if (mType == OSB.EventType.EXCEPTION) {
            value = "exception";
        } else if (mType == OSB.EventType.SOCIAL) {
            value = "social";
        } else if (mType == OSB.EventType.TIMING) {
            value = "timing";
        }

        return value;
    }

    public String getTypeData() {
        String data = getTypeValue();
        if (mType == OSB.EventType.ACTION) {
            data = mSubType;
        }

        return data;
    }

    public String getTypeDataKey() {
        // Get the type of the hits
        String key = "ev";
        if (mType == OSB.EventType.EVENT) {
            key = "ev";
        } else if (mType == OSB.EventType.SCREENVIEW) {
            key = "sv";
        } else if (mType == OSB.EventType.PAGEVIEW) {
            key = "pg";
        } else if (mType == OSB.EventType.ACTION) {
            key = "ac";
        } else if (mType == OSB.EventType.IDS) {
            key = "id";
        } else if (mType == OSB.EventType.EXCEPTION) {
            key = "ex";
        } else if (mType == OSB.EventType.SOCIAL) {
            key = "sc";
        } else if (mType == OSB.EventType.TIMING) {
            key = "ti";
        }

        return key;
    }

    public String[] getDefaultEventKeys() {
        String[] keys = new String[] {};
        if (mType == OSB.EventType.EVENT || mType == OSB.EventType.EXCEPTION ||
                mType == OSB.EventType.SOCIAL || mType == OSB.EventType.TIMING) {
            keys = new String[] { "category", "action", "label", "value" };
        } else if (mType == OSB.EventType.SCREENVIEW) {
            keys = new String[] { "id", "name" };
        } else if (mType == OSB.EventType.PAGEVIEW) {
            keys = new String[] { "id", "title", "viewId", "url", "referrer" };
        } else if (mType == OSB.EventType.ACTION) {
            keys = new String[] { "id", "tax", "discount", "currencyCode", "revenue" };
        } else if (mType == OSB.EventType.IDS) {
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
