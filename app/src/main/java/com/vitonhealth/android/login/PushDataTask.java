package com.vitonhealth.android.login;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by matanghao1 on 23/5/15.
 */
public class PushDataTask extends AsyncTask<Void, Void, String> {

    OAuth2Token token;
    Context context;

    public PushDataTask(OAuth2Token token, Context context) {
        this.token = token;
        this.context = context;
    }

    @Override
    protected String doInBackground(Void... params) {


        String ads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/vitonBLE/";
        JSONArray data = generateJSON(ads);
        // post data to server, should use async call
        VitonClient client = new VitonClient();
        if (token.shouldRefresh()) {
            token = client.renewToken(token);
            token.save(context.getSharedPreferences(VitonClient.PREFS_NAME, 0));
        }
        String reply = client.pushHbrData(token.getAccessToken(), data);
        Log.v("DataTransferService", reply);
        return reply;


    }

    protected JSONArray generateJSON(String ads) {
        // generate some random data for testing
        JSONArray data = new JSONArray();


        File folder = new File(ads);
        String[] dirs = folder.list();
        int lineCounter = 0;
        for (String dir : dirs) {
            File subFolder = new File(ads + dir + "/");
            String[] files = subFolder.list();
            JSONObject one = new JSONObject();
            boolean allSuccess = true;
            for (String file : files) {
                BufferedReader br = null;

                try {

                    String sCurrentLine;

                    br = new BufferedReader(new FileReader(ads + dir + "/" + file));

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
                    /*File temp = new File(ads + dir + "/" + file);
                    temp.delete();*/
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (br != null) br.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();

                    }


                }

            }
            /*
            if (allSuccess) {
                File tempDir = new File(ads + dir + "/");
                tempDir.delete();
            }*/
        }
        return data;
    }


    @Override
    protected void onCancelled() {
    }
}