package com.mindease.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mindease.app.databinding.ActivityMoodCalendarBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Calendar-based mood history view. Displays a monthly grid where each
// day cell shows the emoji that was logged. Users can navigate between
// months and tap individual days to view or edit mood entries. Mood
// data is fetched from the user's Firestore subcollection.
public class MoodCalendarActivity extends AppCompatActivity {

    private ActivityMoodCalendarBinding binding;
    private final Map<String, String> moodMap = new HashMap<>();
    private final Calendar displayCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMoodCalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        
        binding.rvCalendarGrid.setLayoutManager(new GridLayoutManager(this, 7));

        binding.btnPrevMonth.setOnClickListener(v -> {
            displayCalendar.add(Calendar.MONTH, -1);
            updateCalendarGrid();
        });

        binding.btnNextMonth.setOnClickListener(v -> {
            displayCalendar.add(Calendar.MONTH, 1);
            updateCalendarGrid();
        });

        loadMoods();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMoods();
    }

    private void loadMoods() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("moods")
                .get()
                .addOnSuccessListener(snap -> {
                    int great = 0, good = 0, okay = 0, sad = 0, awful = 0;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    moodMap.clear();

                    for (QueryDocumentSnapshot doc : snap) {
                        com.google.firebase.Timestamp ts = doc.getTimestamp("timestamp");
                        String emoji = doc.getString("emoji");
                        if (ts != null && emoji != null) {
                            String date = sdf.format(ts.toDate());
                            moodMap.put(date, emoji);
                            switch (emoji) {
                                case "😄":
                                case "Great":
                                    great++;
                                    break;
                                case "🙂":
                                case "Good":
                                    good++;
                                    break;
                                case "😐":
                                case "Okay":
                                    okay++;
                                    break;
                                case "😔":
                                case "Sad":
                                    sad++;
                                    break;
                                case "😭":
                                case "Awful":
                                case "😡":
                                case "Anger":
                                case "😰":
                                case "Anxiety":
                                case "😱":
                                case "Panic":
                                case "😞":
                                case "Depression":
                                    awful++;
                                    break;
                            }
                        }
                    }
                    binding.tvSummaryGreat.setText("😄 Great/Happy: " + great);
                    binding.tvSummaryGood.setText("🙂 Good: " + good);
                    binding.tvSummaryOkay.setText("😐 Okay/Neutral: " + okay);
                    binding.tvSummarySad.setText("😔 Sad: " + sad);
                    binding.tvSummaryAwful.setText("😭 Awful/Anxious/Anger: " + awful);
                    
                    updateCalendarGrid();
                });
    }

    private void updateCalendarGrid() {
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        binding.tvMonthTitle.setText(monthYearFormat.format(displayCalendar.getTime()));

        List<String> daysInMonth = new ArrayList<>();
        Calendar cal = (Calendar) displayCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1 = Sunday, 2 = Monday, etc.
        int daysCount = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Add empty spaces for previous month's days
        for (int i = 1; i < firstDayOfWeek; i++) {
            daysInMonth.add("");
        }

        // Add actual days of the month
        for (int i = 1; i <= daysCount; i++) {
            daysInMonth.add(String.valueOf(i));
        }

        // Fill remaining spaces to make it a full grid row
        while (daysInMonth.size() % 7 != 0) {
            daysInMonth.add("");
        }

        CalendarAdapter adapter = new CalendarAdapter(daysInMonth, displayCalendar);
        binding.rvCalendarGrid.setAdapter(adapter);
    }

    private class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
        private final List<String> days;
        private final Calendar calendar;

        public CalendarAdapter(List<String> days, Calendar calendar) {
            this.days = days;
            this.calendar = (Calendar) calendar.clone();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_day, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String dayText = days.get(position);
            holder.tvDayNumber.setText(dayText);
            
            if (dayText.isEmpty()) {
                holder.tvMoodEmoji.setVisibility(View.GONE);
                holder.itemView.setOnClickListener(null);
                return;
            }

            int day = Integer.parseInt(dayText);
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            String dateKey = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);

            if (moodMap.containsKey(dateKey)) {
                String emoji = moodMap.get(dateKey);
                holder.tvMoodEmoji.setVisibility(View.VISIBLE);
                
                String emojiStr = "😐"; // default fallback
                if (emoji != null) {
                    switch (emoji) {
                        case "😄":
                        case "Great":
                            emojiStr = "😄";
                            break;
                        case "😐":
                        case "Okay":
                            emojiStr = "😐";
                            break;
                        case "😔":
                        case "Sad":
                            emojiStr = "😔";
                            break;
                        case "😡":
                        case "Anger":
                            emojiStr = "😡";
                            break;
                        case "😰":
                        case "Anxiety":
                            emojiStr = "😰";
                            break;
                        case "😱":
                        case "Panic":
                            emojiStr = "😱";
                            break;
                        case "😞":
                        case "Depression":
                            emojiStr = "😞";
                            break;
                        case "🙂":
                        case "Good":
                            emojiStr = "🙂";
                            break;
                        case "😭":
                        case "Awful":
                            emojiStr = "😭";
                            break;
                        default:
                            emojiStr = emoji;
                            break;
                    }
                }
                holder.tvMoodEmoji.setText(emojiStr);
            } else {
                holder.tvMoodEmoji.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                String emoji = moodMap.getOrDefault(dateKey, "No entry");
                binding.tvDayMood.setText("Mood on " + dateKey + ": " + emoji);
                
                Intent intent = new Intent(MoodCalendarActivity.this, MoodNoteEntryActivity.class);
                intent.putExtra("date", dateKey);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return days.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDayNumber;
            TextView tvMoodEmoji;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
                tvMoodEmoji = itemView.findViewById(R.id.tvMoodEmoji);
            }
        }
    }
}
