package com.vitonhealth.android.login;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

/**
 * @author luochun
 */
public class VitonServiceFragment extends Fragment {

    public VitonServiceFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_viton_service, container, false);

        final Button button = (Button) rootView.findViewById(R.id.push_data_button);

        button.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                TextView textView = (TextView) rootView.findViewById(R.id.server_response);

                OAuth2Token token = OAuth2Token.load(rootView.getContext().getSharedPreferences(VitonClient.PREFS_NAME, 0));
                textView.setText("uploading.. please wait");
                PushDataTask mAuthTask = new PushDataTask(token, textView);
                mAuthTask.execute((Void) null);

            }
        });

        final Button refreshTokenButton = (Button) rootView.findViewById(R.id.refresh_token_button);
        refreshTokenButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                TextView textView = (TextView) rootView.findViewById(R.id.server_response);
                OAuth2Token token = OAuth2Token.load(rootView.getContext().getSharedPreferences(VitonClient.PREFS_NAME, 0));

                textView.setText("refreshing token.. please wait:" + token.getAccessToken());
                RefreshTokenTask mAuthTask = new RefreshTokenTask(token, textView);
                mAuthTask.execute((Void) null);

            }
        });

        return rootView;
    }

    // Async Task to push data to server
    public class PushDataTask extends AsyncTask<Void, Void, String> {

        OAuth2Token token;
        TextView textView;

        public PushDataTask(OAuth2Token token, TextView textView) {
            this.token = token;
            this.textView = textView;
        }

        @Override
        protected String doInBackground(Void... params) {

            // generate some random data for testing
            JSONArray data = new JSONArray();
            Date now = new Date();
            Random r = new Random(now.getTime());
            for (int i = 0; i < 60 * 60 * 2; i++) {
                JSONObject one = new JSONObject();
                try {
                    Date newD = new Date(now.getTime() - i * 1000);
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    one.put("value", 60 + r.nextDouble() * 40);
                    one.put("timestamp", dateFormat.format(newD));
                } catch (JSONException e) {
                    Log.v("main_activity", "failed to add random data", e);
                }
                data.put(one);
            }

            // post data to server, should use async call
            VitonClient client = new VitonClient();
            if (token.shouldRefresh()) {
                token = client.renewToken(token);
                token.save(textView.getRootView().getContext().getSharedPreferences(VitonClient.PREFS_NAME, 0));
            }
            String reply = client.pushHbrData(token.getAccessToken(), data);
            Log.v("main_activity", reply);
            return reply;
        }

        @Override
        protected void onPostExecute(final String reply) {
            textView.setText("server reply: " + reply);
        }

        @Override
        protected void onCancelled() {
        }

    }

    public class RefreshTokenTask extends AsyncTask<Void, Void, String> {

        OAuth2Token token;
        TextView textView;

        public RefreshTokenTask(OAuth2Token token, TextView textView) {
            this.token = token;
            this.textView = textView;
        }

        @Override
        protected String doInBackground(Void... params) {
            VitonClient client = new VitonClient();

            token = client.renewToken(token);
            token.save(textView.getRootView().getContext().getSharedPreferences(VitonClient.PREFS_NAME, 0));

            return token.getAccessToken();
        }

        @Override
        protected void onPostExecute(final String reply) {
            textView.setText("new token: " + reply);
        }
    }
}
