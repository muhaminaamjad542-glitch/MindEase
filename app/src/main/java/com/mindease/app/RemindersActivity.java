package com.mindease.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mindease.app.databinding.ActivityRemindersBinding;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

// Reminder preferences screen. Allows users to set daily wellness
// check-in reminders using the Android AlarmManager. Reminder times
// are persisted to Firestore and scheduled as repeating alarms that
// trigger ReminderReceiver.
public class RemindersActivity extends AppCompatActivity {

    private ActivityRemindersBinding binding;
    private final String[] times = {"08:00", "21:00", "09:00", "19:00", "18:00"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRemindersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        // Set initial time labels
        binding.tvTimeMood.setText(times[0]);
        binding.tvTimeJournal.setText(times[1]);
        binding.tvTimeBreathing.setText(times[2]);
        binding.tvTimeSelfCare.setText(times[3]);
        binding.tvTimeChat.setText(times[4]);

        // Time pickers
        binding.tvTimeMood.setOnClickListener(v -> showTimePicker(0, binding.tvTimeMood, "Time for your mood check-in! 😊"));
        binding.tvTimeJournal.setOnClickListener(v -> showTimePicker(1, binding.tvTimeJournal, "Time to write in your journal! 📔"));
        binding.tvTimeBreathing.setOnClickListener(v -> showTimePicker(2, binding.tvTimeBreathing, "Time for breathing exercises! 🌬"));
        binding.tvTimeSelfCare.setOnClickListener(v -> showTimePicker(3, binding.tvTimeSelfCare, "Don't forget your self-care! 💚"));
        binding.tvTimeChat.setOnClickListener(v -> showTimePicker(4, binding.tvTimeChat, "MindEase AI is here to listen! 💬"));

        binding.btnSave.setOnClickListener(v -> saveToFirestore());
    }

    private void showTimePicker(int idx, android.widget.TextView tv, String message) {
        String[] parts = times[idx].split(":");
        int hour = Integer.parseInt(parts[0]);
        int min  = Integer.parseInt(parts[1]);

        new TimePickerDialog(this, (view, h, m) -> {
            times[idx] = String.format("%02d:%02d", h, m);
            tv.setText(times[idx]);
            scheduleAlarm(idx, h, m, message);
        }, hour, min, false).show();
    }

    private void scheduleAlarm(int id, int hour, int minute, String message) {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("message", message);

        PendingIntent pi = PendingIntent.getBroadcast(this, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        if (cal.getTimeInMillis() < System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pi);
    }

    private void saveToFirestore() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("moodCheckIn",  Map.of("enabled", binding.switchMood.isChecked(),    "time", times[0]));
        prefs.put("journal",      Map.of("enabled", binding.switchJournal.isChecked(), "time", times[1]));
        prefs.put("breathing",    Map.of("enabled", binding.switchBreathing.isChecked(),"time", times[2]));
        prefs.put("selfCare",     Map.of("enabled", binding.switchSelfCare.isChecked(),"time", times[3]));
        prefs.put("chat",         Map.of("enabled", binding.switchChat.isChecked(),    "time", times[4]));

        FirebaseFirestore.getInstance().collection("reminders").document(uid)
                .set(prefs)
                .addOnSuccessListener(v -> Toast.makeText(this, "Reminders saved! ✅", Toast.LENGTH_SHORT).show());
    }
}
