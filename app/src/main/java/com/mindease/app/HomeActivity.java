package com.mindease.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mindease.app.databinding.ActivityHomeBinding;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.util.Calendar;

// Main dashboard of the MindEase app. Displays a personalized greeting,
// the daily motivational quote (loaded from a local JSON asset), mood
// streak counter from Firestore, and navigation cards for all major
// app features including mood tracking, chat, breathing, games, and more.
public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupBottomNav();
        loadUserData();
        loadDailyQuote();
        setupCards();
    }

    private void setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_mood) {
                startActivity(new Intent(this, MoodTrackingActivity.class));
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, ChatActivity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            }
            return true;
        });
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
    }

    private void loadUserData() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        // Load cached name immediately
        String cachedName = SharedPrefManager.getInstance(this).getUserName();
        String hour = getGreeting();
        binding.tvGreeting.setText(hour + ", " + cachedName + " 👋");

        // Fetch from Firestore
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null) {
                            SharedPrefManager.getInstance(this).setUserName(name);
                            binding.tvGreeting.setText(hour + ", " + name + " 👋");
                        }
                        Long streak = doc.getLong("streak");
                        binding.tvStreakCount.setText(streak != null ? streak + " 🔥" : "0 🔥");
                    }
                });
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }

    private void loadDailyQuote() {
        try {
            InputStream is = getResources().openRawResource(R.raw.quotes);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            JSONArray quotes = new JSONArray(new String(buffer));
            int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            JSONObject quote = quotes.getJSONObject(day % quotes.length());
            binding.tvQuote.setText("\"" + quote.getString("text") + "\"");
            binding.tvQuoteAuthor.setText("— " + quote.getString("author"));
        } catch (Exception e) {
            binding.tvQuote.setText("\"You are stronger than you think.\"");
            binding.tvQuoteAuthor.setText("— MindEase");
        }
    }

    private void setupCards() {
        binding.cardMood.setOnClickListener(v ->
                startActivity(new Intent(this, MoodTrackingActivity.class)));
        binding.cardChat.setOnClickListener(v ->
                startActivity(new Intent(this, ChatActivity.class)));
        binding.cardCalendar.setOnClickListener(v ->
                startActivity(new Intent(this, MoodCalendarActivity.class)));
        binding.cardJournal.setOnClickListener(v ->
                startActivity(new Intent(this, JournalActivity.class)));
        binding.cardQuotes.setOnClickListener(v ->
                startActivity(new Intent(this, QuoteActivity.class)));
        binding.cardGames.setOnClickListener(v ->
                startActivity(new Intent(this, GamesActivity.class)));
        binding.cardBreathing.setOnClickListener(v ->
                startActivity(new Intent(this, BreathingActivity.class)));
        binding.cardEmergency.setOnClickListener(v ->
                startActivity(new Intent(this, EmergencyActivity.class)));
        binding.tvStreakLabel.setOnClickListener(v ->
                startActivity(new Intent(this, MoodStreakActivity.class)));
    }
}
