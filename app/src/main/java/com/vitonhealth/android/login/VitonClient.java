package com.vitonhealth.android.login;

import android.net.http.AndroidHttpClient;
import android.util.Log;
import android.webkit.CookieManager;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author luochun
 */
public class VitonClient {

    public static final String PREFS_NAME = "VITON-LOGIN";

    public static final String VITON_SERVER = "http://viton-server.herokuapp.com";
    private static final String OAUTH_ENDPOINT = VITON_SERVER + "/oauth/token";
    private static final String PUSH_DATA_ENDPOINT = VITON_SERVER + "/hbr";
    private static final String WEB_LOGIN_ENDPOINT = VITON_SERVER + "/login.html";
    private static final String CLIENT_APPID = "clientapp";
    private static final String CLIENT_SECRET = "123456";

    public boolean acquireWebSession(String username, String password) {

        AndroidHttpClient httpClient = AndroidHttpClient.newInstance("");
        try {
            Log.v("oauth", "call acquireWebSession()");
            HttpPost httpPost = new HttpPost(WEB_LOGIN_ENDPOINT);
            List<NameValuePair> parameters = new ArrayList<>();
            parameters.add(new BasicNameValuePair("password", password));
            parameters.add(new BasicNameValuePair("username", username));
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, HTTP.UTF_8);
            httpPost.setEntity(formEntity);
            httpPost.addHeader("Accept", "text/html,application/xhtml+xml,application/xml");

            HttpResponse httpResponse = httpClient.execute(httpPost);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            for (Header header : httpResponse.getHeaders("Set-Cookie")) {
                cookieManager.setCookie(VITON_SERVER, header.getValue());
                Log.v("viton_client", header.getValue());
            }
            return true;
        } catch (Exception e) {
            Log.v("vitonclient", "error acquire browser cookies", e);
            return false;
        } finally {
            httpClient.close();
        }
    }

    public OAuth2Token acquireToken(String username, String password) {
        AndroidHttpClient httpClient = AndroidHttpClient.newInstance("");

        try {
            Log.v("oauth", "call acquireToken()");
            HttpPost httpPost = new HttpPost(OAUTH_ENDPOINT);
            List<NameValuePair> parameters = new ArrayList<>();
            parameters.add(new BasicNameValuePair("password", password));
            parameters.add(new BasicNameValuePair("username", username));
            parameters.add(new BasicNameValuePair("grant_type", "password"));
            parameters.add(new BasicNameValuePair("scope", "read write"));
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, HTTP.UTF_8);
            httpPost.setEntity(formEntity);

            // basic authentication with client-id and client-secret(not actually secret)
            httpPost.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(CLIENT_APPID, CLIENT_SECRET), "UTF-8", false));
            httpPost.addHeader("Accept", "application/json");

            HttpResponse httpResponse = httpClient.execute(httpPost);
            int code = httpResponse.getStatusLine().getStatusCode();

            // only consider 200 as successful
            if (code != 200) {
                return null;
            }

            String responseString = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            Log.v("oauth", responseString);

            JSONObject jObject = new JSONObject(responseString);
            OAuth2Token newToken = new OAuth2Token();
            newToken.setAccessToken(jObject.getString("access_token"));
            newToken.setRefreshToken(jObject.getString("refresh_token"));
            newToken.setExpiresIn(jObject.getInt("expires_in"));
            newToken.setAcquiredTime(new Date().getTime());
            return newToken;

        } catch (Exception e) {
            Log.v("vitonclient", "error acquire access token", e);
            return null;

        } finally {
            httpClient.close();

        }

    }

    public OAuth2Token renewToken(OAuth2Token token) {
        AndroidHttpClient httpClient = AndroidHttpClient.newInstance("");

        try {
            Log.v("oauth", "call renewToken()");
            HttpPost httpPost = new HttpPost(OAUTH_ENDPOINT);
            List<NameValuePair> parameters = new ArrayList<>();
            parameters.add(new BasicNameValuePair("grant_type", "refresh_token"));
            parameters.add(new BasicNameValuePair("refresh_token", token.getRefreshToken()));
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, HTTP.UTF_8);
            httpPost.setEntity(formEntity);

            // basic authentication with client-id and client-secret(not actually secret)
            httpPost.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(CLIENT_APPID, CLIENT_SECRET), "UTF-8", false));
            httpPost.addHeader("Accept", "application/json");

            HttpResponse httpResponse = httpClient.execute(httpPost);
            int code = httpResponse.getStatusLine().getStatusCode();

            // only consider 200 as successful
            if (code != 200) {
                return null;
            }

            String responseString = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            Log.v("oauth", responseString);

            JSONObject jObject = new JSONObject(responseString);
            OAuth2Token newToken = new OAuth2Token();
            newToken.setAccessToken(jObject.getString("access_token"));
            newToken.setRefreshToken(jObject.getString("refresh_token"));
            newToken.setExpiresIn(jObject.getInt("expires_in"));
            newToken.setAcquiredTime(new Date().getTime());

            return newToken;

        } catch (Exception e) {
            Log.v("vitonclient", "error renew access token", e);
            return null;

        } finally {
            httpClient.close();

        }
    }

    public String pushHbrData(String accessToken, JSONArray data) {
        AndroidHttpClient httpClient = AndroidHttpClient.newInstance("");

        try {
            Log.v("oauth", "call pushHbrData() with token " + accessToken);
            Log.v("oauth", "call pushHbrData() with data " + data.toString());
            HttpPost httpPost = new HttpPost(PUSH_DATA_ENDPOINT);

            StringEntity entity = new StringEntity(data.toString());
            httpPost.setEntity(entity);
            // basic authentication with client-id and client-secret(not actually secret)
            httpPost.addHeader("Accept", "application/json,application/xml,application/xhtml+xml,text/html,text/plain");
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("Authorization", "Bearer " + accessToken);

            HttpResponse httpResponse = httpClient.execute(httpPost);
            String responseString = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");

            return responseString;
        } catch (Exception e) {
            Log.v("vitonclient", "error push data", e);
            return "failed";
        } finally {
            httpClient.close();
        }

    }


}
