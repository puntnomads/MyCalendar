package com.puntnomads.mycalendar;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

/**
 * Created by puntnomads on 28/10/2016.
 */

public class NotificationPublisherService extends IntentService {

    public static String NOTIFICATION_ID = "notification-id";
    public static String NOTIFICATION = "notification";

    public NotificationPublisherService() {
        super("NotificationPublisherService");
    }
    @Override
    protected void onHandleIntent(Intent intent) {

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = intent.getParcelableExtra(NOTIFICATION);
        int id = intent.getIntExtra(NOTIFICATION_ID, 0);
        notificationManager.notify(id, notification);

        NotificationReceiver.completeWakefulIntent(intent);
    }

}
