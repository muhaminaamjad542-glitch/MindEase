package com.mindease.app;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

// BroadcastReceiver that fires when a scheduled reminder alarm triggers.
// Builds and displays a local notification with the reminder message,
// tapping which opens the home dashboard.
public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "mindease_reminders";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String message = intent.getStringExtra("message");
        if (message == null) message = "Time to check in with MindEase! 🧠";

        Intent openIntent = new Intent(ctx, HomeActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                ctx, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("MindEase 🧠")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(ctx);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
