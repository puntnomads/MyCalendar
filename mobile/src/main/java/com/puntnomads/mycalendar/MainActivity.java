package com.puntnomads.mycalendar;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{
    // Projection array. Creating indices for this array instead of doing
// dynamic lookups improves performance.
    public static final String[] EVENT_PROJECTION = new String[] {
            CalendarContract.Calendars._ID,                           // 0
            CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
            CalendarContract.Calendars.OWNER_ACCOUNT                  // 3
    };

    private static final String[] INSTANCE_PROJECTION = {
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END
    };

    // Request code for READ_CONTACTS. It can be any number > 0.
    private static final int PERMISSIONS_REQUEST_READ_CALENDAR = 1;

    private TextView textView;
    ArrayList<String> titles = new ArrayList<String>();
    ArrayList<String> descriptions = new ArrayList<String>();
    ArrayList<Long> beginTimes = new ArrayList<Long>();
    ArrayList<Long> endTimes = new ArrayList<Long>();

    GoogleApiClient googleClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.textView);
        // Build a new GoogleApiClient for the the Wearable API
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        askPermissions();
    }

    public void askPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CALENDAR)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CALENDAR},
                        PERMISSIONS_REQUEST_READ_CALENDAR);
            }
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.
            getCalendarEvents();
        }
    }

    public void getCalendarEvents() {
        final String[] INSTANCE_PROJECTION = new String[] {
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
        };

        // The indices for the projection array above.
        final int PROJECTION_TITLE_INDEX = 0;
        final int PROJECTION_DESCRIPTION_INDEX = 1;
        final int PROJECTION_BEGIN_INDEX = 2;
        final int PROJECTION_END_INDEX = 3;

        Date now = new Date();
        // Specify the date range you want to search for recurring event instances
        Calendar beginTime = Calendar.getInstance();
        beginTime.set(now.getYear()+1900, now.getMonth(), now.getDate()+0, now.getHours()+0, now.getMinutes()+0);
        long startMillis = beginTime.getTimeInMillis();
        Calendar endTime = Calendar.getInstance();
        endTime.set(now.getYear()+1900, now.getMonth(), now.getDate()+1, now.getHours()+0, now.getMinutes()+0);
        long endMillis = endTime.getTimeInMillis();

        // Construct the query with the desired date range.
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startMillis);
        ContentUris.appendId(builder, endMillis);
        String string = Long.toString(System.currentTimeMillis()) + " ";

        // Submit the query
        Cursor cur =  getContentResolver().query(builder.build(), INSTANCE_PROJECTION, null, null, null);
        while (cur.moveToNext()) {

            // Get the field values
            long begin = cur.getLong(PROJECTION_BEGIN_INDEX);
            long end = cur.getLong(PROJECTION_END_INDEX);
            String title = cur.getString(PROJECTION_TITLE_INDEX);
            String description = cur.getString(PROJECTION_DESCRIPTION_INDEX);

            String theEvent = title + " " + description + " " + begin + " " + end;
            Log.v("event : ", theEvent);
            string += " " + theEvent;
            titles.add(title);
            descriptions.add(description);
            beginTimes.add(begin);
            endTimes.add(end);
        }
        cur.close();
        textView.setText(string);

    }

    // Connect to the data layer when the Activity starts
    @Override
    protected void onStart() {
        super.onStart();
        googleClient.connect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {

        String WEARABLE_DATA_PATH = "/wearable_data";

        // Create a DataMap object and send it to the data layer
        DataMap dataMap = new DataMap();
        dataMap.putLong("numbers", titles.size());
        for(int x =0; x < titles.size(); x++){
            dataMap.putString("title"+x, titles.get(x));
            dataMap.putString("description"+x, descriptions.get(x));
            dataMap.putLong("begin"+x, beginTimes.get(x));
            dataMap.putLong("end"+x, endTimes.get(x));
        }
        dataMap.putLong("time", System.currentTimeMillis());

        //Requires a new thread to avoid blocking the UI
        new SendToDataLayerThread(WEARABLE_DATA_PATH, dataMap).start();
        Toast.makeText(getApplicationContext(), "Sended Data", Toast.LENGTH_SHORT).show();
    }

    // Disconnect from the data layer when the Activity stops
    @Override
    protected void onStop() {
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
        }
        super.onStop();
    }

    // Placeholders for required connection callbacks
    @Override
    public void onConnectionSuspended(int cause) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }

    class SendToDataLayerThread extends Thread {
        String path;
        DataMap dataMap;

        // Constructor for sending data objects to the data layer
        SendToDataLayerThread(String p, DataMap data) {
            path = p;
            dataMap = data;
        }

        public void run() {
            // Construct a DataRequest and send over the data layer
            PutDataMapRequest putDMR = PutDataMapRequest.create(path);
            putDMR.getDataMap().putAll(dataMap);
            PutDataRequest request = putDMR.asPutDataRequest().setUrgent();
            DataApi.DataItemResult result = Wearable.DataApi.putDataItem(googleClient, request).await();
            if (result.getStatus().isSuccess()) {
                Log.v("myTag", "DataMap: " + dataMap + " sent successfully to data layer ");
            } else {
                // Log an error
                Log.v("myTag", "ERROR: failed to send DataMap to data layer");
            }
        }
    }


}
