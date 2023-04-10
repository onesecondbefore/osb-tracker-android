package com.onesecondbefore.tracker;

import java.util.Map;
import java.util.Hashtable;

public class Event {
    private OSB.HitType mType = OSB.HitType.EVENT;
    private String mNamespace = "default";
    private Map<String, Object> mData = new Hashtable<>();
    private String mSubType = "";
    private boolean mIsGpsEnabled = false;
    private double mLatitude = 0;
    private double mLongitude = 0;

    Event(OSB.HitType type, String subType, Map<String, Object> data,
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

    public String getTypeIdentifier() {
        if (mType == OSB.HitType.SCREENVIEW) {
            return "screenview";
        } else if (mType == OSB.HitType.PAGEVIEW) {
           return "pageview";
        } else if (mType == OSB.HitType.ACTION) {
            return "ac";
        } else if (mType == OSB.HitType.IDS) {
            return "ids";
        } else if (mType == OSB.HitType.EVENT) {
            return "event";
        } else if (mType == OSB.HitType.AGGREGATE) {
            return "aggregate";
        } else if (mType == OSB.HitType.VIEWABLE_IMPRESSION) {
            return "viewable_impression";
        }

        // TODO: Confirm if these can be removed (they are not currently present in the iOS SDK) ^MB
//        } else if (mType == OSB.HitType.EXCEPTION) {
//            return "exception";
//        } else if (mType == OSB.HitType.SOCIAL) {
//            return "social";
//        } else if (mType == OSB.HitType.TIMING) {
//            return "timing";
//        }

        return "event";
    }

    public String getTypeData() {
        String data = getTypeIdentifier();
        if (mType == OSB.HitType.ACTION) {
            data = mSubType;
        }

        return data;
    }

    public String getTypeDataKey() {
        // Get the type of the hits
        String key = "ev";
        if (mType == OSB.HitType.EVENT) {
            key = "ev";
        } else if (mType == OSB.HitType.SCREENVIEW) {
            key = "sv";
        } else if (mType == OSB.HitType.PAGEVIEW) {
            key = "pg";
        } else if (mType == OSB.HitType.ACTION) {
            key = "ac";
        } else if (mType == OSB.HitType.IDS) {
            key = "id";
        } else if (mType == OSB.HitType.EXCEPTION) {
            key = "ex";
        } else if (mType == OSB.HitType.SOCIAL) {
            key = "sc";
        } else if (mType == OSB.HitType.TIMING) {
            key = "ti";
        }

        return key;
    }

    public String[] getDefaultEventKeys() {
        String[] keys = new String[] {};
        if (mType == OSB.HitType.EVENT || mType == OSB.HitType.EXCEPTION ||
                mType == OSB.HitType.SOCIAL || mType == OSB.HitType.TIMING) {
            keys = new String[] { "category", "action", "label", "value" };
        } else if (mType == OSB.HitType.SCREENVIEW) {
            keys = new String[] { "id", "name" };
        } else if (mType == OSB.HitType.PAGEVIEW) {
            keys = new String[] { "id", "title", "viewId", "url", "referrer" };
        } else if (mType == OSB.HitType.ACTION) {
            keys = new String[] { "id", "tax", "discount", "currencyCode", "revenue" };
        } else if (mType == OSB.HitType.IDS) {
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
