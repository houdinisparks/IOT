package com.example.yanyee.iotpet;

/**
 * Created by yanyee on 12/9/2015.
 */

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.*;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;

public class MqttService extends Service {

    public static final String APP_ID = "iotpet";

    // constants used to notify the Activity UI of received messages
    public static final String MQTT_MSG_RECEIVED_INTENT = "iotpet.MSGRECEIVED";
    public static final String MQTT_MSG_RECEIVED_TOPIC = "iotpet.MSGRECEIVED_TOPIC";
    public static final String MQTT_MSG_RECEIVED_MSG = "iotpet.MSGRECEIVED_MSGBODY";
    public static final String MQTT_DATA_RECEIVED = "iotpet.DATA_RECEIVED";

    // constants used to tell the Activity UI the connection status
    //actionr that took place and is being reported.
    public static final String MQTT_STATUS_INTENT = "iotpet.STATUS";
    public static final String MQTT_STATUS_MSG = "iotpet.STATUS_MSG";

    // constant used internally to schedule the next ping event
    public static final String MQTT_PING_ACTION = "iotpet.PING";

    // constants used by status bar notifications
    public static final int MQTT_NOTIFICATION_ONGOING = 1;
    public static final int MQTT_NOTIFICATION_UPDATE = 2;


    // constants used to define MQTT connection status
    public enum MQTTConnectionStatus {
        INITIAL,                            // initial status
        CONNECTING,                         // attempting to connect
        CONNECTED,                          // connected
        NOTCONNECTED_WAITINGFORINTERNET,    // can't connect because the phone
        //     does not have Internet access
        NOTCONNECTED_USERDISCONNECT,        // user has explicitly requested
        //     disconnection
        NOTCONNECTED_DATADISABLED,          // can't connect because the user
        //     has disabled data access
        NOTCONNECTED_UNKNOWNREASON          // failed to connect for some reason
    }

    // MQTT constants
    public static final int MAX_MQTT_CLIENTID_LENGTH = 22;

    /************************************************************************/
    /*    VARIABLES used to maintain state                                  */
    /************************************************************************/
    //status of MQTT client connection
    private MQTTConnectionStatus connectionStatus = MQTTConnectionStatus.INITIAL;


    /************************************************************************/
    /*    VARIABLES used to configure MQTT connection                       */
    /************************************************************************/

    // taken from preferences
    //    host name of the server we're receiving push notifications from
    private String brokerHostName;      //BROKER_URL
    // taken from preferences
    //    topic we want to receive messages about
    //    can include wildcards - e.g.  '#' matches anything
    private String topicName;
    //private String clientID = getMACAddress() + "-sub";
    private String clientID;

    // defaults - this sample uses very basic defaults for it's interactions
    //   with message brokers
    private int brokerPortNumber = 1883;
    private MqttClientPersistence usePersistence = null;
    private boolean cleanStart = false;
    private int[] qualitiesOfService = {0};

    //how often should te app ping the serve to keep the connection alive?
    private short keepAliveSeconds = 20 * 60;

    /************************************************************************/
    /*    VARIABLES  - other local variables                                */
    /************************************************************************/
    // connection to the message broker
    private MqttClient mqttClient = null;

    // receiver that notifies the Service when the phone gets data connection
    private NetworkConnectionIntentReceiver netConnReceiver;

    // receiver that notifies the Service when the user changes data use preferences
    private BackgroundDataChangeIntentReceiver dataEnabledReceiver;

    // receiver that wakes the Service up when it's time to ping the server
    private PingSender pingSender;

    //
    private LocalBinder<MqttService> mBinder;
    private String Tag = "Service.Status.IOTPet";


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Tag, "onCreate()");

        // reset status variable to initial state
        connectionStatus = MQTTConnectionStatus.INITIAL;

        // create a binder that will let the Activity UI send
        //   commands to the Service
        mBinder = new LocalBinder<MqttService>(this);

        // get the broker settings out of app preferences
        //   this is not the only way to do this - for example, you could use
        //   the Intent that starts the Service to pass on configuration values
        //sharedpreferences are simply sets of data that stored persistenly(even if the application is
        //stopped  or device is off)
        SharedPreferences settings = getSharedPreferences(APP_ID, MODE_PRIVATE);
        brokerHostName = settings.getString("broker", "tcp://broker.mqttdashboard.com:1883");
        topicName = settings.getString("topic", "IOTPet/readings");

        // register to be notified whenever the user changes their preferences
        //  relating to background data use - so that we can respect the current
        //  preference
        dataEnabledReceiver = new BackgroundDataChangeIntentReceiver();
        registerReceiver(dataEnabledReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        // define the connection to the broker
        defineConnectionToBroker(brokerHostName);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                handleStart(intent, startId);
            }
        }, "MqttService").start();

        // return START_NOT_STICKY - we want this Service to be left running
        //  unless explicitly stopped, and it's process is killed, we want it to
        //  be restarted
        //when the Service starts, it cals the handleStart function.
        Log.d(Tag, "Service Started");

        return START_STICKY;
    }

    synchronized void handleStart(Intent intent, int startId) {
        // before we start - check for a couple of reasons why we should stop
        Log.d(Tag, "handleStart()");

        if (mqttClient == null) {
            // we were unable to define the MQTT client connection, so we stop
            //  immediately - there is nothing that we can do
            Log.d(Tag, "mqttClient == null");
            stopSelf();
            return;
        }

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (!cm.getActiveNetworkInfo().isConnected()) // respect the user's request not to use data!
        {
            // user has disabled background data

            connectionStatus = MQTTConnectionStatus.NOTCONNECTED_DATADISABLED;

            // update the app to show that the connection has been disabled
            broadcastServiceStatus("Not connected - background data disabled");
            Log.d(Tag, "not connected, data disabled");
            // we have a listener running that will notify us when this
            //   preference changes, and will call handleStart again when it
            //   is - letting us pick up where we leave off now
            return;
        }

        // the Activity UI has started the MQTT service - this may be starting
        //  the Service new for the first time, or after the Service has been
        //  running for some time (multiple calls to startService don't start
        //  multiple Services, but it does call this method multiple times)
        // if we have been running already, we re-send any stored data
        rebroadcastStatus();
        rebroadcastReceivedMessages();

        // if the Service was already running and we're already connected - we
        //   don't need to do anything
        if (!isAlreadyConnected()) {
            // set the status to show we're trying to connect
            Log.d(Tag, "trying to connect");
            connectionStatus = MQTTConnectionStatus.CONNECTING;

            // we are creating a background service that will run forever until
            //  the user explicity stops it. so - in case they start needing
            //  to save battery life - we should ensure that they don't forget
            //  we're running, by leaving an ongoing notification in the status
            //  bar while we are running
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Intent notificationIntent = new Intent(this, _MainActivity.class);
            //_MainAcitivty is whatever activity you would want to be launched by notification.

            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new Notification.Builder((this))
                    .setContentIntent(contentIntent)
                    .setTicker("MQTT")
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(true)
                    .setContentTitle("MQTT")
                    .setContentText("MQTT service is running").build();


            nm.notify(MQTT_NOTIFICATION_ONGOING, notification);


            // before we attempt to connect - we check if the phone has a
            //  working data connection
            if (isOnline()) {
                Log.d(Tag, "is Online");
                // we think we have an Internet connection, so try to connect
                //  to the message broker
                /*if (connectToBroker()) {
                    // we subscribe to a topic - registering to receive push
                    //  notifications with a particular key
                    // in a 'real' app, you might want to subscribe to multiple
                    //  topics - I'm just subscribing to one as an example
                    // note that this topicName could include a wildcard, so
                    //  even just with one subscription, we could receive
                    //  messages for multiple topics
                    subscribeToTopic(topicName);*/
                connectToBroker();

            } else {
                // we can't do anything now because we don't have a working
                //  data connection
                connectionStatus = MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET;
                Log.d(Tag, "is offline");
                // inform the app that we are not connected
                broadcastServiceStatus("Waiting for network connection");
            }
        }

        // changes to the phone's network - such as bouncing between WiFi
        //  and mobile data networks - can break the MQTT connection
        // the MQTT connectionLost can be a bit slow to notice, so we use
        //  Android's inbuilt notification system to be informed of
        //  network changes - so we can reconnect immediately, without
        //  haing to wait for the MQTT timeout
        if (netConnReceiver == null) {
            netConnReceiver = new NetworkConnectionIntentReceiver();
            registerReceiver(netConnReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        }

        // creates the intents that are used to wake up the phone when it is
        //  time to ping the server
        if (pingSender == null) {
            Log.d(Tag, "pingSender Activated");
            pingSender = new PingSender();
            registerReceiver(pingSender, new IntentFilter(MQTT_PING_ACTION));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // disconnect immediately
        disconnectFromBroker();
        Log.d(Tag, "disconnected from broker");
        // inform the app that the app has successfully disconnected
        broadcastServiceStatus("Disconnected");

        // try not to leak the listener
        if (dataEnabledReceiver != null) {
            unregisterReceiver(dataEnabledReceiver);
            dataEnabledReceiver = null;
        }

        if (mBinder != null) {
            mBinder.close();
            mBinder = null;
        }
    }
    /************************************************************************/
    /*    METHODS - broadcasts and notifications                            */

    /************************************************************************/

    // methods used to notify the Activity UI of something that has happened
    //  so that it can be updated to reflect status and the data received
    //  from the server
    private void broadcastServiceStatus(String statusDescription) {
        // inform the app (for times when the Activity UI is running /
        //   active) of the current MQTT connection status so that it
        //   can update the UI accordingly
        Log.d(Tag, "initialising broadcastServiceStatus");
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MQTT_STATUS_INTENT);
        broadcastIntent.putExtra(MQTT_STATUS_MSG, statusDescription);
        sendBroadcast(broadcastIntent);
    }

    private void broadcastReceivedMessage(String topic, String message) {
        // pass a message received from the MQTT server on to the Activity UI
        //   (for times when it is running / active) so that it can be displayed
        //   in the app GUI

        Log.d(Tag, "broadcastReceiveMessage activated. Message passed to activity.");
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MQTT_MSG_RECEIVED_INTENT);
        broadcastIntent.putExtra(MQTT_MSG_RECEIVED_TOPIC, topic);
        broadcastIntent.putExtra(MQTT_MSG_RECEIVED_MSG, message);
        sendBroadcast(broadcastIntent);
    }

    private void broadcastData() {
        Log.d(Tag, "brodcastDAta activated.");
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MQTT_DATA_RECEIVED);
        sendBroadcast(broadcastIntent);
    }

    // methods used to notify the user of what has happened for times when
    //  the app Activity UI isn't running

    private void notifyUser(String alert, String title, String body) {
        Log.d(Tag, "botifyUser started. Alert: " + alert + "title : " + title + "body: " + body);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, _MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification.Builder((this))
                .setContentIntent(contentIntent)
                .setTicker(alert)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentTitle(title)
                .setContentInfo(body)
                .setContentText(alert).build();


        nm.notify(MQTT_NOTIFICATION_UPDATE, notification);
        Log.d(Tag, "NOTIFIED");
    }

    /************************************************************************/
    /*    METHODS - binding that allows access from the Actitivy            */

    /************************************************************************/

    // trying to do local binding while minimizing leaks - code thanks to
    //   Geoff Bruckner - which I found at
    //   http://groups.google.com/group/cw-android/browse_thread/thread/d026cfa71e48039b/c3b41c728fedd0e7?show_docid=c3b41c728fedd0e7
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    //mBinder is of LocalBinder Type

    public class LocalBinder<S> extends Binder {
        private WeakReference<S> mService;

        public LocalBinder(S service) {
            mService = new WeakReference<S>(service);
        }

        public S getService() {
            return mService.get();
        }

        public void close() {
            mService = null;
        }


    }
    //
    // public methods that can be used by Activities that bind to the Service
    //

    public MQTTConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void rebroadcastStatus() {
        String status = "";

        switch (connectionStatus) {
            case INITIAL:
                status = "Please wait";
                break;
            case CONNECTING:
                status = "Connecting...";
                break;
            case CONNECTED:
                status = "Connected";
                break;
            case NOTCONNECTED_UNKNOWNREASON:
                status = "Not connected - waiting for network connection";
                break;
            case NOTCONNECTED_USERDISCONNECT:
                status = "Disconnected";
                break;
            case NOTCONNECTED_DATADISABLED:
                status = "Not connected - background data disabled";
                break;
            case NOTCONNECTED_WAITINGFORINTERNET:
                status = "Unable to connect";
                break;
        }

        //
        // inform the app that the Service has successfully connected
        broadcastServiceStatus(status);
    }

    public void disconnect() {
        disconnectFromBroker();

        // set status
        connectionStatus = MQTTConnectionStatus.NOTCONNECTED_USERDISCONNECT;

        // inform the app that the app has successfully disconnected
        broadcastServiceStatus("Disconnected");
    }

    /************************************************************************/
    /*    METHODS - MQTT methods inherited from MQTT classes                */

    /************************************************************************/
    /*
     * callback - method called when we no longer have a connection to the
     *  message broker server
     */
    public void connectionLost() throws Exception {
        // we protect against the phone switching off while we're doing this
        //  by requesting a wake lock - we request the minimum possible wake
        //  lock - just enough to keep the CPU running until we've finished
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
        wl.acquire();


        //
        // have we lost our data connection?
        //

        if (isOnline() == false) {
            Log.d(Tag, "isOnline == false");
            connectionStatus = MQTTConnectionStatus.NOTCONNECTED_WAITINGFORINTERNET;

            // inform the app that we are not connected any more
            broadcastServiceStatus("Connection lost - no network connection");

            //
            // inform the user (for times when the Activity UI isn't running)
            //   that we are no longer able to receive messages
            notifyUser("Connection lost - no network connection",
                    "MQTT", "Connection lost - no network connection");

            //
            // wait until the phone has a network connection again, when we
            //  the network connection receiver will fire, and attempt another
            //  connection to the broker
        } else {
            //
            // we are still online
            //   the most likely reason for this connectionLost is that we've
            //   switched from wifi to cell, or vice versa
            //   so we try to reconnect immediately
            //

            connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;

            // inform the app that we are not connected any more, and are
            //   attempting to reconnect
            broadcastServiceStatus("Connection lost - reconnecting...");

            // try to reconnect
            connectToBroker();
        }

        // we're finished - if the phone is switched off, it's okay for the CPU
        //  to sleep now
        wl.release();
    }




    /*
     *   callback - called when we receive a message from the server
     */


    public void publishArrived(String topic, MqttMessage message, int qos, boolean retained) {
        // we protect against the phone switching off while we're doing this
        //  by requesting a wake lock - we request the minimum possible wake
        //  lock - just enough to keep the CPU running until we've finished

        String messageBody = message.toString();

        Log.d(Tag, "publishArrived");
        Log.d(Tag, "DataCache : " + dataCache.toString());
        timeNow = System.currentTimeMillis();
        saveTimeNow(); //save time if not yet saved.
        calculateAmountDrank(messageBody);

        if (xLabel <= 15) {
            storeData(String.valueOf(xLabel), totalAmountDrank);
        } else {
            if (fifteenMinutesHasPassed()) {
                storeData(String.valueOf(xLabel), amountDrankInFifteenMins);
            }
        }

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
        wl.acquire();

        //
        //  I'm assuming that all messages I receive are being sent as strings
        //   this is not an MQTT thing - just me making as assumption about what
        //   data I will be receiving - your app doesn't have to send/receive
        //   strings - anything that can be sent as bytes is valid


        //
        //  for times when the app's Activity UI is not running, the Service
        //   will need to safely store the data that it receives
        //if (addReceivedMessageToStore(topic, messageBody)) {
        // this is a new message - a value we haven't seen before

        //
        // inform the app (for times when the Activity UI is running) of the
        //   received message so the app UI can be updated with the new data
        broadcastReceivedMessage(topic, String.valueOf(totalAmountDrank));
        broadcastData();
        //
        // inform the user (for times when the Activity UI isn't running)
        //   that there is new data available
        notifyUser("New data received", topic, String.valueOf(messageBody));
        // }

        // receiving this message will have kept the connection alive for us, so
        //  we take advantage of this to postpone the next scheduled ping
        scheduleNextPing();

        // we're finished - if the phone is switched off, it's okay for the CPU
        //  to sleep now
        wl.release();
    }
    /************************************************************************/
    /*    METHODS - wrappers for some of the MQTT methods that we use       */

    /************************************************************************/

    /*
     * Create a client connection object that defines our connection to a
     *   message broker server Implemented subscriber methods here.
     */
    private void defineConnectionToBroker(String brokerHostName) {

        try {
            // define the connection to the broker
            clientID = getMACAddress() + "-sub";
            Log.d(Tag, "Client ID = " + clientID);
            Log.d(Tag, "brokerHostname = " + brokerHostName);
            mqttClient = new MqttClient(brokerHostName, clientID, usePersistence);
            Log.d(Tag, "created new MQTT Client");
            // register this client app has being able to receive messages

        } catch (MqttException e) {
            // something went wrong!
            mqttClient = null;
            connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;

            //
            // inform the app that we failed to connect so that it can update
            //  the UI accordingly
            broadcastServiceStatus("Invalid connection parameters");

            //
            // inform the user (for times when the Activity UI isn't running)
            //   that we failed to connect
            notifyUser("Unable to connect", "MQTT", "Unable to connect");
        }
    }

    /*
     * (Re-)connect to the message broker
     */
    private boolean connectToBroker() {
        try {
            // try to connect
            mqttClient.setCallback(new mSubscribeCallback());
            mqttClient.connect(); //removed (clientID, cleanStart, keepAliveSeconds);
            mqttClient.subscribe(topicName);
            Log.d(Tag, "Client subscribed");
            //
            // inform the app that the app has successfully connected
            broadcastServiceStatus("Connected");
            // we are connected
            connectionStatus = MQTTConnectionStatus.CONNECTED;

            // we need to wake up the phone's CPU frequently enough so that the
            //  keep alive messages can be sent
            // we schedule the first one of these now
            scheduleNextPing();

            return true;
        } catch (MqttException e) {
            // something went wrong!

            connectionStatus = MQTTConnectionStatus.NOTCONNECTED_UNKNOWNREASON;

            //
            // inform the app that we failed to connect so that it can update
            //  the UI accordingly
            broadcastServiceStatus("Unable to connect");

            //
            // inform the user (for times when the Activity UI isn't running)
            //   that we failed to connect
            notifyUser("Unable to connect", "MQTT", "Unable to connect - will retry later");

            // if something has failed, we wait for one keep-alive period before
            //   trying again
            // in a real implementation, you would probably want to keep count
            //  of how many times you attempt this, and stop trying after a
            //  certain number, or length of time - rather than keep trying
            //  forever.
            // a failure is often an intermittent network issue, however, so
            //  some limited retry is a good idea
            scheduleNextPing();

            return false;
        }
    }

    /*
     * Send a request to the message broker to be sent messages ed with
     *  the specified topic name. Wildcards are allowed.
     */
   /* private void subscribeToTopic(String topicName) {
        boolean subscribed = false;

        if (isAlreadyConnected() == false) {
            // quick sanity check - don't try and subscribe if we
            //  don't have a connection

            Log.e("mqtt", "Unable to subscribe as we are not connected");
        } else {
            try {
                String[] topics = {topicName};
                mqttClient.subscribe(topics, qualitiesOfService);

                subscribed = true;
            } catch (MqttNotConnectedException e) {
                Log.e("mqtt", "subscribe failed - MQTT not connected", e);
            } catch (IllegalArgumentException e) {
                Log.e("mqtt", "subscribe failed - illegal argument", e);
            } catch (MqttException e) {
                Log.e("mqtt", "subscribe failed - MQTT exception", e);
            }
        }

        if (subscribed == false) {
            //
            // inform the app of the failure to subscribe so that the UI can
            //  display an error
            broadcastServiceStatus("Unable to subscribe");

            //
            // inform the user (for times when the Activity UI isn't running)
            notifyUser("Unable to subscribe", "MQTT", "Unable to subscribe");
        }
    }*/

    /*
     * Terminates a connection to the message broker.
     */
    private void disconnectFromBroker() {
        // if we've been waiting for an Internet connection, this can be
        //  cancelled - we don't need to be told when we're connected now
        try {
            if (netConnReceiver != null) {
                unregisterReceiver(netConnReceiver);
                netConnReceiver = null;
            }

            if (pingSender != null) {
                unregisterReceiver(pingSender);
                pingSender = null;
            }
        } catch (Exception eee) {
            // probably because we hadn't registered it
            Log.e("mqtt", "unregister failed", eee);
        }

        try {
            if (mqttClient != null) {
                mqttClient.disconnect();
            }
        } catch (MqttPersistenceException e) {
            Log.e("mqtt", "disconnect failed - persistence exception", e);
        } catch (MqttException e) {
            Log.e("mqtt", "mqtt cant disconnect");
        } finally {
            mqttClient = null;
        }

        // we can now remove the ongoing notification that warns users that
        //  there was a long-running ongoing service running
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancelAll();
    }

    /*
     * Checks if the MQTT client thinks it has an active connection
     */

    private boolean isAlreadyConnected() {
        return ((mqttClient != null) && (mqttClient.isConnected() == true));
    }

    private class BackgroundDataChangeIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            // we protect against the phone switching off while we're doing this
            //  by requesting a wake lock - we request the minimum possible wake
            //  lock - just enough to keep the CPU running until we've finished
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
            wl.acquire();

            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (cm.getActiveNetworkInfo().isConnected()) {
                // user has allowed background data - we start again - picking
                //  up where we left off in handleStart before
                defineConnectionToBroker(brokerHostName);
                handleStart(intent, 0);
            } else {
                // user has disabled background data
                connectionStatus = MQTTConnectionStatus.NOTCONNECTED_DATADISABLED;

                // update the app to show that the connection has been disabled
                broadcastServiceStatus("Not connected - background data disabled");

                // disconnect from the broker
                disconnectFromBroker();
            }

            // we're finished - if the phone is switched off, it's okay for the CPU
            //  to sleep now
            wl.release();
        }
    }


    /*
     * Called in response to a change in network connection - after losing a
     *  connection to the server, this allows us to wait until we have a usable
     *  data connection again
     */
    private class NetworkConnectionIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            // we protect against the phone switching off while we're doing this
            //  by requesting a wake lock - we request the minimum possible wake
            //  lock - just enough to keep the CPU running until we've finished
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
            wl.acquire();

            if (isOnline()) {
                // we have an internet connection - have another try at connecting
                /*f (connectToBroker()) {
                    // we subscribe to a topic - registering to receive push
                    //  notifications with a particular key
                    subscribeToTopic(topicName);
                }*/
                connectToBroker();
            }

            // we're finished - if the phone is switched off, it's okay for the CPU
            //  to sleep now
            wl.release();
        }
    }

    private void scheduleNextPing() {
        // When the phone is off, the CPU may be stopped. This means that our
        //   code may stop running.
        // When connecting to the message broker, we specify a 'keep alive'
        //   period - a period after which, if the client has not contacted
        //   the server, even if just with a ping, the connection is considered
        //   broken.
        // To make sure the CPU is woken at least once during each keep alive
        //   period, we schedule a wake up to manually ping the server
        //   thereby keeping the long-running connection open
        // Normally when using this Java MQTT client library, this ping would be
        //   handled for us.
        // Note that this may be called multiple times before the next scheduled
        //   ping has fired. This is good - the previously scheduled one will be
        //   cancelled in favour of this one.
        // This means if something else happens during the keep alive period,
        //   (e.g. we receive an MQTT message), then we start a new keep alive
        //   period, postponing the next ping.

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(MQTT_PING_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT);

        // in case it takes us a little while to do this, we try and do it
        //  shortly before the keep alive period expires
        // it means we're pinging slightly more frequently than necessary
        Calendar wakeUpTime = Calendar.getInstance();
        wakeUpTime.add(Calendar.SECOND, keepAliveSeconds);

        AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        aMgr.set(AlarmManager.RTC_WAKEUP,
                wakeUpTime.getTimeInMillis(),
                pendingIntent);
    }


    //added push notifications from server that show children drank water.

    public class PingSender extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Note that we don't need a wake lock for this method (even though
            //  it's important that the phone doesn't switch off while we're
            //  doing this).
            // According to the docs, "Alarm Manager holds a CPU wake lock as
            //  long as the alarm receiver's onReceive() method is executing.
            //  This guarantees that the phone will not sleep until you have
            //  finished handling the broadcast."
            // This is good enough for our needs.

            try {
                MqttTopic ping = mqttClient.getTopic("IOTPet/ping");
                ping.publish(new MqttMessage(new byte[]{1}));
                Log.d(Tag, "ping sent");
            } catch (MqttException e) {
                // if something goes wrong, it should result in connectionLost
                //  being called, so we will handle it there
                Log.e("mqtt", "ping failed - MQTT exception", e);

                // assume the client connection is broken - trash it
                try {
                    mqttClient.disconnect();
                } catch (MqttPersistenceException e1) {
                    Log.e("mqtt", "disconnect failed - persistence exception", e1);
                } catch (MqttException e1) {
                    e1.printStackTrace();
                }

                // reconnect
                connectToBroker();
            }

            // start the next keep alive period
            scheduleNextPing();
        }

    }

    /************************************************************************/
    /*   APP SPECIFIC - stuff that would vary for different uses of MQTT    */
    /************************************************************************/

    //  apps that handle very small amounts of data - e.g. updates and
    //   notifications that don't need to be persisted if the app / phone
    //   is restarted etc. may find it acceptable to store this data in a
    //   variable in the Service
    //  that's what I'm doing in this sample: storing it in a local hashtable
    //  if you are handling larger amounts of data, and/or need the data to
    //   be persisted even if the app and/or phone is restarted, then
    //   you need to store the data somewhere safely
    //  see http://developer.android.com/guide/topics/data/data-storage.html
    //   for your storage options - the best choice depends on your needs

    // stored internally

    private Hashtable<String, Integer> dataCache = new Hashtable<String, Integer>();

//    private boolean addReceivedMessageToStore(String key, String value) {
//        String previousValue = null;
//        if(fifteenMinutesHasPassed()) {
//            if (value.length() == 0) {
//                previousValue = dataCache.remove(key);
//            } else {
//                previousValue = dataCache.put(key, value);
//            }
//        }
//
//        // is this a new value? or am I receiving something I already knew?
//        //  we return true if this is something new
//        return ((previousValue == null) ||
//                (previousValue.equals(value) == false));
//    }

    private void storeData(String key, int newData) {
        dataCache.put(key, newData);
    }

    public Hashtable<String, Integer> getStoredData() {
        return dataCache;
    }

    private boolean timeSaved = false;
    private long timeNow;
    private long saveTime;
    private long fifteenMinsAfter; //90000 = 15mins
    private int amountDrankInFifteenMins;
    private int totalAmountDrank = 0;
    private int xLabel = 15;


    //within the period of 15 mins, accumulate the water
    private void calculateAmountDrank(String newData) {
        int convData = Integer.parseInt(newData);
        totalAmountDrank += convData;
        if (!fifteenMinutesHasPassed()) {
            amountDrankInFifteenMins += convData;
        } else {
            xLabel += 15;
            amountDrankInFifteenMins = 0;
        }
    }

    private void saveTimeNow() {
        if (!timeSaved) {
            saveTime = timeNow;
            fifteenMinsAfter = saveTime + 900000;
            timeSaved = true;
        }
    }

    private boolean fifteenMinutesHasPassed() {
        if (timeNow > fifteenMinsAfter) {
            timeSaved = false;
            return true;
        } else {
            return false;
        }
    }

    // provide a public interface, so Activities that bind to the Service can
    //  request access to previously received messages

    public void rebroadcastReceivedMessages() {
//        Enumeration<String> e = dataCache.keys();
//        while (e.hasMoreElements()) {
//            Log.d(Tag, "rebroadcasting : " + e.nextElement());
//
//            String nextKey = e.nextElement();
//            String nextValue = dataCache.get(nextKey).toString();
//
//            broadcastReceivedMessage(nextKey, nextValue);
//        }
        String amountDrank = String.valueOf(totalAmountDrank);
        broadcastReceivedMessage("Total Amount Drank", amountDrank);
    }

    /************************************************************************/
    /*    METHODS - internal utility methods                                */

    /************************************************************************/

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isAvailable() &&
                cm.getActiveNetworkInfo().isConnected()) {
            return true;
        }

        return false;
    }


    public String getMACAddress() {

        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        String address = info.getMacAddress();
        return address;
        //**************FOR PC *****************************/
        /*InetAddress ip;
        StringBuilder sb = new StringBuilder();
        try {
            ip = InetAddress.getLocalHost();
            System.out.println("Current IP address : " + ip.getHostAddress());
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            byte[] mac = network.getHardwareAddress();
            System.out.print("Current MAC address : ");
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            System.out.println(sb.toString());

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return sb.toString();*/

    }


    class mSubscribeCallback implements MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {
            //This is called when the connection is lost. We could reconnect here.
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            System.out.println("Message arrived. Topic: " + topic + "  Message: " + message.toString());
            publishArrived(topic, message, 0, false);


            if ("home/LWT".equals(topic)) {
                System.err.println("Sensor gone!");
            }
        }


        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            //no-op
        }

        public MqttMessage getMessage(String topic, MqttMessage message) {
            return message;
        }
    }
}

