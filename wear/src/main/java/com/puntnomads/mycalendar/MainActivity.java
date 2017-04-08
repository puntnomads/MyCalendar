package com.puntnomads.mycalendar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends WearableActivity {

    private TextView mTextView;
    ArrayList<String> titles = new ArrayList<String>();
    ArrayList<String> descriptions = new ArrayList<String>();
    ArrayList<Long> beginTimes = new ArrayList<Long>();
    ArrayList<Long> endTimes = new ArrayList<Long>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });

        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    public class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle data = intent.getBundleExtra("datamap");
            long numbers = data.getLong("numbers");
            for(int x = 0; x < numbers; x++){
                titles.add(data.getString("title"+x));
                descriptions.add(data.getString("description"+x));
                beginTimes.add(data.getLong("begin"+x));
                endTimes.add(data.getLong("end"+x));
            }
            if(titles.size()==numbers){
                displayInfo();
                sendNotifications();
            }
        }
    }

    private void scheduleNotification(Notification notification, Long beginTime, int time) {

        Intent notificationIntent = new Intent(this, NotificationReceiver.class);
        notificationIntent.putExtra(NotificationReceiver.NOTIFICATION_ID, 1);
        notificationIntent.putExtra(NotificationReceiver.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Date date = new Date(beginTime);
        String dateString = null;
        SimpleDateFormat sdfr = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        dateString = sdfr.format(date);
        Log.v("time", dateString);
        long futureInMillis = beginTime; //- time;
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, futureInMillis, pendingIntent);
    }

    public Notification getNotification(String title, String description, long begin, long end) {
        Date startDate = new Date(begin);
        String startDateString = null;
        Date endDate = new Date(end);
        String endDateString = null;
        SimpleDateFormat sdfr = new SimpleDateFormat("HH:mm");
        startDateString = sdfr.format(startDate);
        endDateString = sdfr.format(endDate);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText(startDateString + "-" + endDateString + "\n" + description)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                .build();
        return notification;
    }

    public void sendNotifications(){
        for(int i = 0; i < titles.size(); i++){
            scheduleNotification(getNotification(titles.get(i),descriptions.get(i), beginTimes.get(i), endTimes.get(i)),
                    beginTimes.get(i), 1000);
        }
    }

    public void displayInfo(){
        String display = "";
        display = "Received from the data Layer\n";
        for(int x = 0; x < titles.size(); x++){
            display += titles.get(x) + ": ";
            display += descriptions.get(x) + "\n";
        }
        mTextView.setText(display);
    }

}

