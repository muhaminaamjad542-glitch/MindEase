package com.mindease.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mindease.app.databinding.ActivityNewJournalBinding;
import java.util.HashMap;
import java.util.Map;

// Journal entry creation screen with a rich text editor supporting
// bold and italic formatting. Saves the entry title and HTML-formatted
// body to the user's Firestore journal subcollection.
public class NewJournalActivity extends AppCompatActivity {

    private ActivityNewJournalBinding binding;
    private String selectedEmoji = "😊";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewJournalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        Button[] emojiButtons = {
            binding.moodAwful, binding.moodSad, binding.moodOkay,
            binding.moodGood, binding.moodGreat
        };
        String[] emojis = {"😭", "😔", "😐", "🙂", "😄"};

        for (int i = 0; i < emojiButtons.length; i++) {
            final String emoji = emojis[i];
            emojiButtons[i].setOnClickListener(v -> {
                selectedEmoji = emoji;
                for (Button b : emojiButtons) b.setAlpha(0.4f);
                v.setAlpha(1.0f);
            });
        }

        // Bold
        binding.btnBold.setOnClickListener(v ->
                applySpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD)));
        // Italic
        binding.btnItalic.setOnClickListener(v ->
                applySpan(new android.text.style.StyleSpan(android.graphics.Typeface.ITALIC)));
        // Underline
        binding.btnUnderline.setOnClickListener(v ->
                applySpan(new android.text.style.UnderlineSpan()));

        binding.btnSave.setOnClickListener(v -> saveJournal());
    }

    private void applySpan(Object span) {
        int start = binding.etContent.getSelectionStart();
        int end   = binding.etContent.getSelectionEnd();
        if (start < end) {
            android.text.Spannable spannable = binding.etContent.getText();
            spannable.setSpan(span, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void saveJournal() {
        String title   = binding.etTitle.getText().toString().trim();
        String content = binding.etContent.getText().toString().trim();
        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please fill in title and content", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        Map<String, Object> journal = new HashMap<>();
        journal.put("title", title);
        journal.put("content", content);
        journal.put("moodEmoji", selectedEmoji);
        journal.put("timestamp", FieldValue.serverTimestamp());

        binding.btnSave.setEnabled(false);
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("journals").add(journal)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Journal saved! 📔", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnSave.setEnabled(true);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
