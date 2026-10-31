package com.mindease.app;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mindease.app.databinding.ActivityProfileBinding;

// User profile overview screen. Displays the user's name, email, avatar,
// streak statistics, and journal entry count. Provides navigation to
// sub-screens: edit profile, reminders, settings, journal, and streak.
public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        loadStats();

        binding.btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));
        binding.btnReminders.setOnClickListener(v ->
                startActivity(new Intent(this, RemindersActivity.class)));
        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        binding.btnJournal.setOnClickListener(v ->
                startActivity(new Intent(this, JournalActivity.class)));
        binding.btnStreak.setOnClickListener(v ->
                startActivity(new Intent(this, MoodStreakActivity.class)));
        binding.btnLogout.setOnClickListener(v -> logout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
    }

    private void loadStats() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        String name = SharedPrefManager.getInstance(this).getUserName();
        String email = SharedPrefManager.getInstance(this).getUserEmail();
        binding.tvName.setText(name);
        binding.tvEmail.setText(email);

        // Load profile picture cache
        String picPath = SharedPrefManager.getInstance(this).getProfilePicturePath();
        if (!picPath.isEmpty()) {
            binding.ivProfilePicture.setVisibility(android.view.View.VISIBLE);
            binding.tvAvatarFallback.setVisibility(android.view.View.GONE);
            binding.ivProfilePicture.setImageURI(android.net.Uri.parse(picPath));
        } else {
            binding.ivProfilePicture.setVisibility(android.view.View.GONE);
            binding.tvAvatarFallback.setVisibility(android.view.View.VISIBLE);
        }

        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long streak = doc.getLong("streak");
                        Long total  = doc.getLong("totalDays");
                        binding.tvStreak.setText(streak != null ? String.valueOf(streak) : "0");
                        binding.tvTotalDays.setText(total != null ? String.valueOf(total) : "0");
                        
                        Object ageObj = doc.get("age");
                        if (ageObj != null) {
                            binding.tvAge.setText("Age: " + ageObj.toString());
                        } else {
                            binding.tvAge.setText("Age: Not specified");
                        }
                    }
                });

        // Count journals
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("journals").get()
                .addOnSuccessListener(snap ->
                        binding.tvJournalCount.setText(String.valueOf(snap.size())));
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        SharedPrefManager.getInstance(this).clearPin();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finishAffinity();
    }
}
