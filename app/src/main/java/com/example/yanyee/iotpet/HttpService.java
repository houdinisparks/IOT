package com.example.yanyee.iotpet;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by yanyee on 12/14/2015.
 */


public class HttpService extends Service {

    private LocalBinder<HttpService> mBinder ;
    String Tag = "Http Service";
    private Hashtable<String, Float> dataCache = new Hashtable<String, Float>();
    public static final String HTTP_DATA_RECEIVED = "iotpet.DATARECEIVED";
    public static final String HTTP_DATA_RECEIVED_INTENT = "iotpet.DATARECEIVEDINTENT";
    public static final String APP_ID = "IOTPET";



    int count = 0;

    private void storeData() {
        Log.d("storeDAta", String.valueOf(count));
        count += 1;
        timeNow = System.currentTimeMillis();
        saveTimeNow(); //save time if not yet saved.
        //calculate total amount drank
        if (fifteenMinutesHasPassed()) {
            storeData(String.valueOf(xLabel), totalAmountDrank);
            xLabel += 15;
        }
        else {
            storeData(String.valueOf(xLabel), totalAmountDrank);
        }

    }

    Timer timer;
    final android.os.Handler handler = new android.os.Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        callAsynchronousTask();
        mBinder = new LocalBinder<HttpService>(this);


    }

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

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                handleStart(intent, startId);

            }
        }, "HttpService").start();

        // return START_NOT_STICKY - we want this Service to be left running
        //  unless explicitly stopped, and it's process is killed, we want it to
        //  be restarted
        //when the Service starts, it cals the handleStart function.
        Log.d(Tag, "Service Started");

        return START_STICKY;
    }

    synchronized void handleStart(Intent intent, int startId) {


    retrieveData();

        Log.d(Tag, "broadcaseReceivedMessage");
    }

    public class PerformBackgroundTask extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] params) {
            retrieveData();
            return null;
        }
    }

    public void callAsynchronousTask() {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            PerformBackgroundTask performBackgroundTask = new PerformBackgroundTask();
                            // PerformBackgroundTask this class is the class that extends AsynchTask
                            performBackgroundTask.execute();
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 50000); //execute in every 50000 ms
    }

    private void retrieveData() {
        String URL = retrieveURLString();
        String data = retrieveDataFromURL(URL);
        calculateAmountDrank(data);
        storeData();
        broadcastReceivedMessage(data);

    }

    /*****
     * THIS THING WORKSS OMG
     **********/
    public String retrieveURLString(){
        String httpString = "";
        HttpUrl httpUrl = new HttpUrl();
        try {
            httpString = httpUrl.getHttpString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return httpString;
    }

    public String retrieveDataFromURL(String URL) {
        JSONObject jsonObject;
        //JSONArray jsonArray;
        String data = "";
        try {
            //jsonArray = new JSONArray();
            jsonObject = new JSONObject(URL);
            data = jsonObject.getString("field1");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(Tag, data);
        return data;

    }


    public class HttpUrl {

        private String getHttpString() throws Exception {

            String url = "https://api.thingspeak.com/channels/67983/feeds/last.json";
            String result = "";
            StringBuilder sb = new StringBuilder();
            ;

            try {
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");
                //add request header

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));

                String line = null;
                while ((line = in.readLine()) != null) {
                    sb.append(line + "\n");
                }
            } catch (Exception e) {
                Log.e("error", "Error getting data from URL: " + e.toString());

                return "";
            }

            result = sb.toString();
            //print result
            return result;

        }

    }

    private void broadcastReceivedMessage(String message) {
        // pass a message received from the MQTT server on to the Activity UI
        //   (for times when it is running / active) so that it can be displayed
        //   in the app GUI
        Log.d(Tag, "broadcastReceiveMessage activated. Message passed to activity.");
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(HTTP_DATA_RECEIVED_INTENT);
        broadcastIntent.putExtra(HTTP_DATA_RECEIVED, message);
        sendBroadcast(broadcastIntent);
    }


    private void storeData(String key, float newData) {
        dataCache.put(key, newData);
        Log.d(Tag, dataCache.toString());
    }

    public Hashtable<String, Float> getStoredData() {
        return dataCache;
    }

    private boolean timeSaved = false;
    private long timeNow;
    private long saveTime;
    private long fifteenMinsAfter; //900000 = 15mins
    private int amountDrankInFifteenMins;
    private float totalAmountDrank = 0;
    private int xLabel = 15;


    //within the period of 15 mins, accumulate the water
    private void calculateAmountDrank(String newData) {
        if(newData.length() != 0) {
            float convData = Float.parseFloat(newData);
            totalAmountDrank = 800 - convData;
            if (!fifteenMinutesHasPassed()) {
                amountDrankInFifteenMins += convData;
            }
        }

    }

    private void saveTimeNow() {
        if (!timeSaved) {
            saveTime = timeNow;
            fifteenMinsAfter = saveTime + 90000;
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

}
