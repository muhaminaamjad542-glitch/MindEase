package com.mindease.app;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mindease.app.databinding.ActivityMoodStreakBinding;

// Wellness streak detail screen. Loads the user's consecutive mood-logging
// streak from Firestore and displays milestone badges (fire icons) that
// indicate progress toward streak goals (e.g. 7-day, 30-day).
public class MoodStreakActivity extends AppCompatActivity {

    private ActivityMoodStreakBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMoodStreakBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        loadStreak();
    }

    private void loadStreak() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    long streak = doc.getLong("streak") != null ? doc.getLong("streak") : 0;
                    long total  = doc.getLong("totalDays") != null ? doc.getLong("totalDays") : 0;

                    binding.tvStreakCount.setText(streak + " 🔥");
                    binding.tvTotalDays.setText("Total: " + total + " days");

                    // Milestones
                    binding.tvMilestone1.setText("1 Day Streak"    + (streak >= 1   ? " ✅" : " 🔒"));
                    binding.tvMilestone7.setText("7 Day Streak"    + (streak >= 7   ? " ✅" : " 🔒"));
                    binding.tvMilestone30.setText("30 Day Streak"  + (streak >= 30  ? " ✅" : " 🔒"));
                    binding.tvMilestone100.setText("100 Day Streak" + (streak >= 100 ? " ✅" : " 🔒"));

                    // Week dots (fill last 7 from total)
                    ImageView[] dots = {
                        binding.dot1, binding.dot2, binding.dot3, binding.dot4,
                        binding.dot5, binding.dot6, binding.dot7
                    };
                    for (int i = 0; i < dots.length; i++) {
                        dots[i].setImageResource(i < (int) Math.min(streak, 7)
                                ? R.drawable.circle_primary
                                : R.drawable.circle_gray);
                    }
                });

    }
}
