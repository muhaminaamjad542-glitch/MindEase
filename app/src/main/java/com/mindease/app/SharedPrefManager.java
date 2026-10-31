package com.mindease.app;

import android.content.Context;
import android.content.SharedPreferences;

// Thread-safe singleton wrapper around Android SharedPreferences.
// Centralizes all local key-value storage for user settings (dark mode,
// PIN, onboarding), cached profile data, and game high scores.
public class SharedPrefManager {

    private static final String PREF_NAME = "MindEasePrefs";
    private static SharedPrefManager instance;
    private final SharedPreferences prefs;

    private SharedPrefManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPrefManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new SharedPrefManager(ctx.getApplicationContext());
        }
        return instance;
    }

    // Onboarding
    public boolean isOnboardingDone()  { return prefs.getBoolean("onboarding_done", false); }
    public void    setOnboardingDone() { prefs.edit().putBoolean("onboarding_done", true).apply(); }

    // PIN
    public boolean isPinEnabled()         { return prefs.getBoolean("pin_enabled", false); }
    public String  getPin()               { return prefs.getString("pin", ""); }
    public void    setPin(String pin)     { prefs.edit().putBoolean("pin_enabled", true).putString("pin", pin).apply(); }
    public void    clearPin()             { prefs.edit().putBoolean("pin_enabled", false).putString("pin", "").apply(); }

    // Dark Mode
    public boolean isDarkMode()           { return prefs.getBoolean("dark_mode", false); }
    public void    setDarkMode(boolean v) { prefs.edit().putBoolean("dark_mode", v).apply(); }

    // User info (cached locally)
    public String  getUserName()          { return prefs.getString("user_name", "User"); }
    public void    setUserName(String n)  { prefs.edit().putString("user_name", n).apply(); }

    public String  getUserEmail()         { return prefs.getString("user_email", ""); }
    public void    setUserEmail(String e) { prefs.edit().putString("user_email", e).apply(); }

    // Last mood log date (for streak)
    public String  getLastMoodDate()          { return prefs.getString("last_mood_date", ""); }
    public void    setLastMoodDate(String d)  { prefs.edit().putString("last_mood_date", d).apply(); }

    // Notification channel setup flag
    public boolean isNotificationPermissionAsked()          { return prefs.getBoolean("notif_asked", false); }
    public void    setNotificationPermissionAsked()         { prefs.edit().putBoolean("notif_asked", true).apply(); }

    // Profile picture path
    public String  getProfilePicturePath()                  { return prefs.getString("profile_picture_path", ""); }
    public void    setProfilePicturePath(String path)       { prefs.edit().putString("profile_picture_path", path).apply(); }

    // High Scores
    public int getBubblePopHighScore() { return prefs.getInt("bubble_pop_highscore", 0); }
    public void setBubblePopHighScore(int score) { prefs.edit().putInt("bubble_pop_highscore", score).apply(); }

    public int getStarCatcherHighScore() { return prefs.getInt("star_catcher_highscore", 0); }
    public void setStarCatcherHighScore(int score) { prefs.edit().putInt("star_catcher_highscore", score).apply(); }
}
