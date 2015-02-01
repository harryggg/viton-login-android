package com.vitonhealth.android.login;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import java.util.Date;

/**
 * @author luochun
 */
public class OAuth2Token {

    String accessToken;
    String refreshToken;
    int expiresIn;
    long acquiredTime;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public long getAcquiredTime() {
        return acquiredTime;
    }

    public void setAcquiredTime(long acquiredTime) {
        this.acquiredTime = acquiredTime;
    }

    public boolean shouldRefresh() {
        // refresh when 6 hours before expiration
        return acquiredTime + expiresIn * 1000 + 6 * 3600 * 1000 >= new Date().getTime();
    }

    public static OAuth2Token load(SharedPreferences sp) {
        OAuth2Token token = new OAuth2Token();
        token.setAccessToken(sp.getString("sec.access_token", null));
        token.setRefreshToken(sp.getString("sec.refresh_token", null));
        token.setExpiresIn(sp.getInt("sec.token_expires_in", 0));
        token.setAcquiredTime(sp.getLong("sec.token_acquired_time", 0L));
        return token;
    }

    public void save(SharedPreferences sp) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("sec.access_token", getAccessToken());
        editor.putString("sec.refresh_token", getRefreshToken());
        editor.putInt("sec.token_expires_in", getExpiresIn());
        editor.putLong("sec.token_acquired_time", getAcquiredTime());
        editor.commit();
        Log.v("login_activity", "edited preference");
    }

}
