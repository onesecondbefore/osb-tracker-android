package com.onesecondbefore.tracker;

public class Config {
    private String mAccountId = "development";
    private String mServerUrl = "https://g.ab21.xyz";
    private String mSiteId = null;
    private boolean mIsDebug = false;

    public void setAccountId(String accountId) {
        this.mAccountId = accountId;
    }

    public void setServerUrl(String serverUrl) {
        this.mServerUrl = serverUrl;
    }

    public void setSiteId(String siteId) {
        this.mSiteId = siteId;
    }

    public void setDebug(boolean isDebug) {
        this.mIsDebug = isDebug;
    }

    public String getAccountId() {
        return mAccountId;
    }

    public String getServerUrl() {
        return mServerUrl;
    }

    public String getSiteId() {
        return mSiteId;
    }

    public boolean isDebugEnabled() {
        return mIsDebug;
    }
}
