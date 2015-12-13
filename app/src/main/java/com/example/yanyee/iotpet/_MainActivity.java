package com.example.yanyee.iotpet;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class _MainActivity extends AppCompatActivity implements _HomeFragment.OnFragmentInteractionListener,_SummaryFragment.OnFragmentInteractionListener{


    NavigationView navigationView;
    DrawerLayout drawerLayOut;
    FragmentManager fragmentManager;
    private String[] contents = {"_HomeFragment", "_SummaryFragment"};

    private StatusUpdateReceiver statusUpdateIntentReceiver;
    private MQTTMessageReceiver messageIntentReceiver;
    String newTopic;
    String newData;
    String test2;
    _HomeFragment homeFragment;
    _SummaryFragment summaryFragment;
    SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayOut = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.nv_View);

        //Initialise all Fragments.
        fragmentManager = getFragmentManager();
        summaryFragment = new _SummaryFragment();
        homeFragment = new _HomeFragment();
        test2 = "old";

        Bundle bundle = new Bundle();
        bundle.putString("test",test2);
        bundle.putString("newData", newData);
        homeFragment.setArguments(bundle);

        fragmentManager.beginTransaction().replace(R.id.content_frame, homeFragment).commit();

        settings = getSharedPreferences(MqttService.APP_ID, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("broker", "tcp://broker.mqttdashboard.com:1883");
        editor.putString("topic", "IOTPet/readings");
        editor.commit();


        statusUpdateIntentReceiver = new StatusUpdateReceiver();
        IntentFilter intentSFilter = new IntentFilter(MqttService.MQTT_STATUS_INTENT);
        registerReceiver(statusUpdateIntentReceiver, intentSFilter);

        messageIntentReceiver = new MQTTMessageReceiver();
        IntentFilter intentCFilter = new IntentFilter(MqttService.MQTT_MSG_RECEIVED_INTENT);
        registerReceiver(messageIntentReceiver, intentCFilter);


        Intent svc = new Intent(this, MqttService.class);
        startService(svc);

        setupDrawerContent(navigationView);
    }



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
        switch(item.getItemId()){
            case R.id.nav_first_fragment:
                fragment =  homeFragment;
                Toast.makeText(this, "_HomeFragment Selected", Toast.LENGTH_SHORT).show();
                break;

            case R.id.nav_second_fragment:
                fragment = summaryFragment;
                Toast.makeText(this, "_SummaryFragment Selected", Toast.LENGTH_SHORT).show();
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

    public class StatusUpdateReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Bundle notificationData = intent.getExtras();
            String newStatus = notificationData.getString(MqttService.MQTT_STATUS_MSG);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("Mqtt Status", newStatus);
            editor.putString("newTopic", newTopic);
            editor.commit();
           // Toast.makeText( super , newStatus + " :statusReceiver", Toast.LENGTH_SHORT).show();

        }
    }
    public class MQTTMessageReceiver extends BroadcastReceiver
    {


        @Override
        public void onReceive(Context context, Intent intent)
        {
            Bundle notificationData = intent.getExtras();
            newTopic = notificationData.getString(MqttService.MQTT_MSG_RECEIVED_TOPIC);
            newData  = notificationData.getString(MqttService.MQTT_MSG_RECEIVED_MSG);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("newData", newData);
            editor.putString("newTopic", newTopic);
            editor.commit();
            //Toast.makeText(new _MainActivity() , newData + " :messageReceiver", Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        unregisterReceiver(statusUpdateIntentReceiver);
        unregisterReceiver(messageIntentReceiver);

    }


}
