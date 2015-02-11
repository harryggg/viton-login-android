package com.vitonhealth.android.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            showVitonWeb();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.viton_setting) {
            showVitonSetting();
            return true;
        } else if (id == R.id.viton_web) {
            showVitonWeb();
            return true;
        } else if (id == R.id.viton_service) {
            showVitonService();
            return true;
        } else if (id == R.id.start_watch_service) {

            Intent myIntent1 = new Intent(this.getApplicationContext(), DataTransferService.class);
            this.getApplicationContext().startService(myIntent1);
            return true;
        } else if (id == R.id.stop_watch_service) {

            Intent myIntent1 = new Intent(this.getApplicationContext(), DataTransferService.class);
            myIntent1.setAction("TERMINATION");
            this.getApplicationContext().startService(myIntent1);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showVitonWeb() {
        VitonWebFragment fragment = new VitonWebFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    private void showVitonSetting() {
        VitonSettingFragment fragment = new VitonSettingFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }

    private void showVitonService() {
        VitonServiceFragment fragment = new VitonServiceFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }


}
