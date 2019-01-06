package com.smartwatch.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.smartwatch.R;

public class NotifiationUtils {
    /*
     * Show notification by building up the notification manager
     */
    public static void showNotification(Context context, final String title, final String
            subject, int
                                                notificationID) {
        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
        bigText.bigText(subject);
        bigText.setBigContentTitle(title);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(subject)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setStyle(bigText);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notificationID /* ID of notification */, notificationBuilder
                .build());
    }
}
