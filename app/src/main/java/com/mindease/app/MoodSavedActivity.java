package com.mindease.app;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mindease.app.databinding.ActivityMoodSavedBinding;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Confirmation screen shown after a mood is successfully logged.
// Displays the selected emoji, label, and tags, then updates the
// user's streak and total days count in Firestore.
public class MoodSavedActivity extends AppCompatActivity {

    private ActivityMoodSavedBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMoodSavedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String emoji = getIntent().getStringExtra("emoji");
        String label = getIntent().getStringExtra("label");
        java.util.ArrayList<String> tags = getIntent().getStringArrayListExtra("tags");

        binding.tvSavedEmoji.setText(emoji);
        binding.tvSavedLabel.setText("Feeling " + label);
        if (tags != null && !tags.isEmpty()) {
            binding.tvSavedTags.setText(String.join("  •  ", tags));
        }

        updateStreak();

        binding.btnViewHistory.setOnClickListener(v ->
                startActivity(new Intent(this, MoodCalendarActivity.class)));
        binding.btnGoHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finishAffinity();
        });
    }

    private void updateStreak() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastDate = SharedPrefManager.getInstance(this).getLastMoodDate();

        if (!today.equals(lastDate)) {
            SharedPrefManager.getInstance(this).setLastMoodDate(today);
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        long streak = doc.getLong("streak") != null ? doc.getLong("streak") : 0;
                        long total  = doc.getLong("totalDays") != null ? doc.getLong("totalDays") : 0;
                        FirebaseFirestore.getInstance().collection("users").document(uid)
                                .update("streak", streak + 1, "totalDays", total + 1);
                        binding.tvStreakUpdate.setText("🔥 " + (streak + 1) + " day streak!");
                    });
        } else {
            binding.tvStreakUpdate.setText("Already logged today ✅");
        }
    }
}
