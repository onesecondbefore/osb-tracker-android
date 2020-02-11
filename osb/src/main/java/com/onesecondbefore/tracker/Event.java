package com.onesecondbefore.tracker;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

public class Event {
    private EventType mType = EventType.EVENT;
    private ArrayList<String> mNameSpaces = new ArrayList<String>();
    private Dictionary<String, Object> mData = new Hashtable<String, Object>();
    private String mSubType = "";
    private boolean mIsGpsEnabled = false;
    private double mLatitude = 0;
    private double mLongitude = 0;

    Event(EventType type, String subType, Dictionary<String, Object> data,
          String[] namespaces, boolean isGpsEnabled, double latitude, double longitude) {
        mType = type;
        mSubType = subType != null ? subType : "";
        mIsGpsEnabled = isGpsEnabled;
        mLatitude = latitude;
        mLongitude = longitude;

        if (data != null) {
            mData = data;
        }

        if (namespaces != null) {
            for (int i = 0; i < namespaces.length; i++) {
                mNameSpaces.add(namespaces[i]);
            }
        }
    }

    public ArrayList<String> getNamespaces()  {
        return mNameSpaces;
    }

    public String getNamespacesValue() {
        if (mNameSpaces.isEmpty()) {
            return "default";
        }

        StringBuilder sb = new StringBuilder();
        String separator = ",";

        for (int i = 0; i < mNameSpaces.size(); i++) {
            sb.append(mNameSpaces.get(i));

            // if not the last item
            if (i != mNameSpaces.size() - 1) {
                sb.append(separator);
            }
        }

        return sb.toString();
    }

    public String getTypeValue() {
        String value = "event";
        if (mType == EventType.EVENT) {
            value = "event";
        } else if (mType == EventType.SCREENVIEW) {
            value = "screenview";
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
            keys = new String[] { "id", "title", "viewId", "url", "referrer" };
        } else if (mType == EventType.ACTION) {
            keys = new String[] { "id", "tax", "discount", "currencyCode", "revenue" };
        } else if (mType == EventType.IDS) {
            keys = new String[] { "key", "value", "label" };
        }

        return keys;
    }

    public Dictionary<String, Object> getData() {
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
