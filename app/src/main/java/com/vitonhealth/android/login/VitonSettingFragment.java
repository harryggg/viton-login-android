package com.vitonhealth.android.login;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * @author luochun
 */
public class VitonSettingFragment extends Fragment {
    public VitonSettingFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_viton_setting, container, false);
        final WebView webView = (WebView) rootView.findViewById(R.id.viton_setting_view);
        webView.setNetworkAvailable(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.loadUrl(VitonClient.VITON_SERVER + "/m/setting.html");
        return rootView;
    }
}
