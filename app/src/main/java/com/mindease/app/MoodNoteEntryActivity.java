package com.mindease.app;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mindease.app.databinding.ActivityMoodNoteEntryBinding;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// Detailed mood note entry and editing screen. Allows creating or
// updating a mood entry for a specific date, including emoji selection,
// wellness score, context tags, and a free-text note. Supports both
// new entries and editing existing entries loaded from Firestore.
public class MoodNoteEntryActivity extends AppCompatActivity {

    private ActivityMoodNoteEntryBinding binding;
    private String selectedDateStr = "";
    private String existingDocId = null;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final String[] spinnerItems = {
        "😄 Happy",
        "😔 Sad",
        "😐 Neutral",
        "😡 Anger",
        "😰 Anxiety",
        "😱 Panic",
        "😞 Depression"
    };

    private final String[] emojis = {"😄", "😔", "😐", "😡", "😰", "😱", "😞"};
    private final String[] labels = {"Happy", "Sad", "Neutral", "Anger", "Anxiety", "Panic", "Depression"};
    private final int[] scores = {5, 2, 3, 2, 2, 1, 1};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMoodNoteEntryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        binding.btnBack.setOnClickListener(v -> finish());

        // Setup Mood Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, spinnerItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerMood.setAdapter(adapter);

        // Get Date from Intent or use Today
        String passedDate = getIntent().getStringExtra("date");
        if (passedDate != null && !passedDate.isEmpty()) {
            selectedDateStr = passedDate;
        } else {
            selectedDateStr = sdf.format(new Date());
        }
        binding.etDate.setText(selectedDateStr);

        binding.etDate.setOnClickListener(v -> showDatePicker());
        binding.btnSave.setOnClickListener(v -> saveMoodNote());
        binding.btnDelete.setOnClickListener(v -> deleteMoodNote());

        checkAndLoadMoodNote();
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        try {
            Date d = sdf.parse(selectedDateStr);
            if (d != null) cal.setTime(d);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            binding.etDate.setText(selectedDateStr);
            checkAndLoadMoodNote();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void checkAndLoadMoodNote() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        Calendar calStart = Calendar.getInstance();
        Calendar calEnd = Calendar.getInstance();
        try {
            Date d = sdf.parse(selectedDateStr);
            if (d != null) {
                calStart.setTime(d);
                calStart.set(Calendar.HOUR_OF_DAY, 0);
                calStart.set(Calendar.MINUTE, 0);
                calStart.set(Calendar.SECOND, 0);
                calStart.set(Calendar.MILLISECOND, 0);

                calEnd.setTime(d);
                calEnd.set(Calendar.HOUR_OF_DAY, 23);
                calEnd.set(Calendar.MINUTE, 59);
                calEnd.set(Calendar.SECOND, 59);
                calEnd.set(Calendar.MILLISECOND, 999);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);
        binding.btnDelete.setVisibility(View.GONE);

        // Fetch moods for this date range
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("moods")
                .whereGreaterThanOrEqualTo("timestamp", calStart.getTime())
                .whereLessThanOrEqualTo("timestamp", calEnd.getTime())
                .get()
                .addOnSuccessListener(snap -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSave.setEnabled(true);
                    
                    if (snap != null && !snap.isEmpty()) {
                        DocumentSnapshot doc = snap.getDocuments().get(0);
                        existingDocId = doc.getId();
                        
                        String emoji = doc.getString("emoji");
                        String cause = doc.getString("cause");
                        String note = doc.getString("note");
                        
                        binding.etCause.setText(cause != null ? cause : "");
                        binding.etNotes.setText(note != null ? note : "");
                        
                        // Select in spinner
                        if (emoji != null) {
                            for (int i = 0; i < emojis.length; i++) {
                                if (emojis[i].equals(emoji)) {
                                    binding.spinnerMood.setSelection(i);
                                    break;
                                }
                            }
                        }
                        binding.btnDelete.setVisibility(View.VISIBLE);
                    } else {
                        existingDocId = null;
                        binding.etCause.setText("");
                        binding.etNotes.setText("");
                        binding.spinnerMood.setSelection(0);
                        binding.btnDelete.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSave.setEnabled(true);
                    Toast.makeText(this, "Error loading: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveMoodNote() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        int selectedIdx = binding.spinnerMood.getSelectedItemPosition();
        String emoji = emojis[selectedIdx];
        String label = labels[selectedIdx];
        int score = scores[selectedIdx];
        String cause = binding.etCause.getText().toString().trim();
        String notes = binding.etNotes.getText().toString().trim();

        Map<String, Object> moodData = new HashMap<>();
        moodData.put("emoji", emoji);
        moodData.put("label", label);
        moodData.put("score", score);
        moodData.put("cause", cause);
        moodData.put("note", notes);
        moodData.put("tags", new ArrayList<String>()); // empty tags for simple form

        // Convert selectedDateStr back to Timestamp
        Calendar saveCal = Calendar.getInstance();
        try {
            Date d = sdf.parse(selectedDateStr);
            if (d != null) {
                saveCal.setTime(d);
                // Keep current hours to make sure it falls in the day
                Calendar current = Calendar.getInstance();
                saveCal.set(Calendar.HOUR_OF_DAY, current.get(Calendar.HOUR_OF_DAY));
                saveCal.set(Calendar.MINUTE, current.get(Calendar.MINUTE));
                saveCal.set(Calendar.SECOND, current.get(Calendar.SECOND));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        moodData.put("timestamp", new Timestamp(saveCal.getTime()));

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);
        binding.btnDelete.setVisibility(View.GONE);

        if (existingDocId != null) {
            // Update
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .collection("moods").document(existingDocId)
                    .set(moodData)
                    .addOnSuccessListener(v -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSave.setEnabled(true);
                        Toast.makeText(this, "Mood note updated! ✅", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSave.setEnabled(true);
                        binding.btnDelete.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Save error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Create
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .collection("moods")
                    .add(moodData)
                    .addOnSuccessListener(v -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSave.setEnabled(true);
                        Toast.makeText(this, "Mood note saved! ✅", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnSave.setEnabled(true);
                        Toast.makeText(this, "Save error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void deleteMoodNote() {
        if (existingDocId == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);
        binding.btnDelete.setVisibility(View.GONE);

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("moods").document(existingDocId)
                .delete()
                .addOnSuccessListener(v -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSave.setEnabled(true);
                    Toast.makeText(this, "Mood note deleted! 🗑️", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSave.setEnabled(true);
                    binding.btnDelete.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Delete error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
