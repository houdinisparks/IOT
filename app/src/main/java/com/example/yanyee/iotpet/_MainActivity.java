package com.example.yanyee.iotpet;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class _MainActivity extends AppCompatActivity implements _HomeFragment.OnFragmentInteractionListener, _SummaryFragment.OnFragmentInteractionListener
        , _TestFragment.OnFragmentInteractionListener {


    NavigationView navigationView;
    DrawerLayout drawerLayOut;
    FragmentManager fragmentManager;
    private String[] contents = {"_HomeFragment", "_SummaryFragment"};

    private HTTPDataReceiver httpDataReceiver;

    String newTopic;
    String newData;
    String test2;
    _HomeFragment homeFragment;
    _SummaryFragment summaryFragment;
    _TestFragment testFragment;
    SharedPreferences settings;
    String Tag = "ThingSpeak";
    MqttService mqttService;
    HttpService httpService;
    JSONObject jsonObject;
    String something;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Log.d("json", jsonObject.toString());
//        try {
//            Toast.makeText(this, jsonObject.getString("field1"),Toast.LENGTH_LONG).show();
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
        settings = getSharedPreferences(HttpService.APP_ID, 0);

        if (savedInstanceState != null) {
            summaryFragment = (_SummaryFragment) getFragmentManager().getFragment(savedInstanceState, "summaryFragment");
            testFragment = new _TestFragment();
            homeFragment = (_HomeFragment) getFragmentManager().getFragment(savedInstanceState, "homeFragment");
        } else {
            summaryFragment = new _SummaryFragment();
            homeFragment = new _HomeFragment();
            testFragment = new _TestFragment();
        }


        drawerLayOut = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.nv_View);

        //Initialise all Fragments.
        fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, homeFragment).commit();

        httpDataReceiver = new HTTPDataReceiver();
        IntentFilter intentLFilter = new IntentFilter(HttpService.HTTP_DATA_RECEIVED_INTENT);
        registerReceiver(httpDataReceiver, intentLFilter);

        Intent svc1= new Intent(this, HttpService.class);
        bindService(svc1, mConnection, BIND_AUTO_CREATE);
        startService(svc1);



        setupDrawerContent(navigationView);

    }


    public HttpService getHttpService() {
        return httpService;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            HttpService.LocalBinder<HttpService> binder = (HttpService.LocalBinder) service;
            httpService = binder.getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };


    public void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {

                    @Override
                    public boolean onNavigationItemSelected(MenuItem item) {
                        selectDrawerItem(item);
                        return true;
                    }
                }
                //item refers to the item you have selected on.
        );
    }

    public void selectDrawerItem(MenuItem item) {
        Fragment fragment;
        switch (item.getItemId()) {
            case R.id.nav_first_fragment:
                fragment = homeFragment;
                Toast.makeText(this, "_HomeFragment Selected", Toast.LENGTH_SHORT).show();
                break;

            case R.id.nav_second_fragment:
                fragment = summaryFragment;
                Toast.makeText(this, "_SummaryFragment Selected", Toast.LENGTH_SHORT).show();
                break;

            case R.id.nav_third_fragment:
                fragment = testFragment;
                Toast.makeText(this, "_TestFragment Selected", Toast.LENGTH_SHORT).show();
                break;

            default:
                fragment = new _HomeFragment();
        }

        fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

        item.setChecked(true);
        setTitle(item.getTitle());
        drawerLayOut.closeDrawers();
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
        // automatically handle clicks on the _HomeFragment/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }


    public class HTTPDataReceiver extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle notificationData = intent.getExtras();
            newData = notificationData.getString(HttpService.HTTP_DATA_RECEIVED);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("newData", newData);
            editor.commit();

            Log.d(Tag, "received broadcast Message");

            //Toast.makeText(new _MainActivity() , newData + " :messageReceiver", Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        unregisterReceiver(httpDataReceiver);


    }


}
