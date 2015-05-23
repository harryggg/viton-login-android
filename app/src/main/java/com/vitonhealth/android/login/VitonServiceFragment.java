package com.vitonhealth.android.login;

import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
    private static final String TAG = "viton service";
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



            String ads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()+"/vitonBLE/";
            JSONArray data = generateJSON(ads);
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

        protected JSONArray generateJSON(String ads){
            // generate some random data for testing
            JSONArray data = new JSONArray();


            File folder = new File(ads);
            String[] dirs = folder.list();
            int lineCounter = 0;
            for (String dir:dirs){
                File subFolder = new File(ads + dir+"/");
                String[] files = subFolder.list();
                JSONObject one = new JSONObject();
                boolean allSuccess = true;
                for (String file:files) {
                    BufferedReader br = null;

                    try {

                        String sCurrentLine;

                        br = new BufferedReader(new FileReader(ads+dir+"/"+file));

                        while ((sCurrentLine = br.readLine()) != null) {
                            String[] datas = sCurrentLine.split(" ");
                            try {
                                one.put("value", Double.parseDouble(datas[1]));
                                one.put("timestamp", datas[0]);
                                data.put(one);
                                lineCounter++;
                            } catch (JSONException e) {
                                e.printStackTrace();
                                allSuccess = false;
                            }

                        }
                        File temp = new File(ads + dir+"/"+file);
                        temp.delete();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (br != null)br.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();

                        }


                    }

                }
                if(allSuccess) {
                    File tempDir = new File(ads + dir + "/");
                    tempDir.delete();
                }
            }
            return data;
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
