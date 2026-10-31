package com.mindease.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mindease.app.databinding.ActivityMoodTrackingBinding;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Mood logging screen where users select an emoji mood, optional context
// tags (work, family, sleep, etc.), and write a brief note. The mood
// entry is saved to the user's Firestore subcollection with a server
// timestamp and numeric wellness score for analytics.
public class MoodTrackingActivity extends AppCompatActivity {

    private ActivityMoodTrackingBinding binding;
    private String selectedEmoji = "";
    private String selectedLabel = "";
    private int selectedScore = 0;
    private final List<String> selectedTags = new ArrayList<>();

    private final String[] emojis  = {"😄", "😔", "😐", "😡", "😰", "😱", "😞"};
    private final String[] labels  = {"Happy", "Sad", "Neutral", "Anger", "Anxiety", "Panic", "Depression"};
    private final int[]    scores  = {5, 2, 3, 2, 2, 1, 1};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMoodTrackingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        binding.btnBack.setOnClickListener(v -> finish());
        setupMoodButtons();
        setupTagButtons();
        binding.btnSave.setOnClickListener(v -> saveMood());
    }

    private void setupMoodButtons() {
        androidx.cardview.widget.CardView[] moodCards = {
            binding.cardMoodHappy, binding.cardMoodSad, binding.cardMoodNeutral,
            binding.cardMoodAnger, binding.cardMoodAnxiety, binding.cardMoodPanic,
            binding.cardMoodDepression
        };
        for (int i = 0; i < moodCards.length; i++) {
            final int idx = i;
            moodCards[i].setOnClickListener(v -> {
                selectedEmoji = emojis[idx];
                selectedLabel = labels[idx];
                selectedScore = scores[idx];
                for (androidx.cardview.widget.CardView card : moodCards) {
                    card.setCardBackgroundColor(getColor(R.color.white));
                }
                moodCards[idx].setCardBackgroundColor(getColor(R.color.primaryLight));
            });
        }
    }

    private void setupTagButtons() {
        Button[] tags = {
            binding.tagWork, binding.tagFamily, binding.tagSleep,
            binding.tagStudies, binding.tagHealth, binding.tagSocial
        };
        for (Button tag : tags) {
            tag.setOnClickListener(v -> {
                String tagText = tag.getText().toString();
                if (selectedTags.contains(tagText)) {
                    selectedTags.remove(tagText);
                    tag.setBackgroundResource(R.drawable.bg_tag_inactive);
                    tag.setTextColor(getColor(R.color.textGray));
                } else {
                    selectedTags.add(tagText);
                    tag.setBackgroundResource(R.drawable.bg_button_primary);
                    tag.setTextColor(Color.WHITE);
                }
            });
        }
    }

    private void saveMood() {
        if (selectedEmoji.isEmpty()) {
            Toast.makeText(this, "Please select a mood", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        Map<String, Object> mood = new HashMap<>();
        mood.put("emoji", selectedEmoji);
        mood.put("label", selectedLabel);
        mood.put("note", binding.etNote.getText().toString().trim());
        mood.put("tags", new ArrayList<>(selectedTags));
        mood.put("timestamp", FieldValue.serverTimestamp());
        mood.put("score", selectedScore);

        binding.btnBack.setEnabled(false);
        FirebaseFirestore.getInstance()
                .collection("users").document(uid).collection("moods")
                .add(mood)
                .addOnSuccessListener(v -> {
                    Intent intent = new Intent(this, MoodSavedActivity.class);
                    intent.putExtra("emoji", selectedEmoji);
                    intent.putExtra("label", selectedLabel);
                    intent.putStringArrayListExtra("tags", new ArrayList<>(selectedTags));
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnBack.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
