package com.onesecondbefore.tracker;

public class Config {
    private String mAccountId = "";
    private String mServerUrl = "";
    private String mSiteId = "";
    private String mDomain = "";
    private boolean mIsDebug = false;

    public void setAccountId(String accountId) {
        this.mAccountId = accountId != null ? accountId : "";
    }

    public void setServerUrl(String serverUrl) {
        this.mServerUrl = serverUrl != null ? serverUrl : "";
    }

    public void setSiteId(String siteId) {
        this.mSiteId = siteId != null ? siteId : "";
    }

    public void setDomain(String domain) {
        this.mDomain = domain != null ? domain : "";
    }

    public void setDebug(boolean isDebug) {
        this.mIsDebug = isDebug;
    }

    public String getAccountId() {
        return mAccountId;
    }

    public String getDomain() {
        return mDomain;
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
