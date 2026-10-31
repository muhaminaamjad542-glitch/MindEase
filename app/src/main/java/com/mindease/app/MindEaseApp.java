package com.mindease.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import com.google.firebase.FirebaseApp;

// Application entry point for MindEase. Initializes Firebase, applies the
// user's saved dark mode preference, and registers notification channels
// required by the reminder and FCM subsystems.
public class MindEaseApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        // Load and apply dark mode preference on startup
        boolean darkMode = SharedPrefManager.getInstance(this).isDarkMode();
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(darkMode
                ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "mindease_reminders",
                    "Daily Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("MindEase wellness reminders");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}
