package com.mindease.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mindease.app.databinding.ActivityJournalBinding;
import java.util.ArrayList;
import java.util.List;

// Journal logbook screen. Loads and displays all journal entries from
// Firestore in a RecyclerView, sorted by most recent. Supports creating
// new entries and viewing existing ones.
public class JournalActivity extends AppCompatActivity {

    private ActivityJournalBinding binding;
    private JournalAdapter adapter;
    private final List<JournalItem> journals = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJournalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnNew.setOnClickListener(v -> startActivity(new Intent(this, NewJournalActivity.class)));

        adapter = new JournalAdapter(journals);
        binding.rvJournals.setLayoutManager(new LinearLayoutManager(this));
        binding.rvJournals.setAdapter(adapter);

        loadJournals();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadJournals();
    }

    private void loadJournals() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("journals")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    journals.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        journals.add(new JournalItem(
                                doc.getString("title"),
                                doc.getString("content"),
                                doc.getString("moodEmoji"),
                                doc.getTimestamp("timestamp") != null
                                        ? doc.getTimestamp("timestamp").toDate().toString() : ""
                        ));
                    }
                    adapter.notifyDataSetChanged();
                    binding.tvEmpty.setVisibility(journals.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    static class JournalItem {
        String title, content, moodEmoji, date;
        JournalItem(String t, String c, String e, String d) {
            title = t; content = c; moodEmoji = e; date = d;
        }
    }

    static class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.JVH> {
        private final List<JournalItem> items;
        JournalAdapter(List<JournalItem> items) { this.items = items; }

        @NonNull @Override
        public JVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_journal, parent, false);
            return new JVH(v);
        }

        @Override public void onBindViewHolder(@NonNull JVH holder, int pos) {
            JournalItem item = items.get(pos);
            holder.tvEmoji.setText(item.moodEmoji != null ? item.moodEmoji : "📔");
            holder.tvTitle.setText(item.title);
            holder.tvPreview.setText(item.content != null && item.content.length() > 60
                    ? item.content.substring(0, 60) + "..." : item.content);
            holder.tvDate.setText(item.date);
        }

        @Override public int getItemCount() { return items.size(); }

        static class JVH extends RecyclerView.ViewHolder {
            TextView tvEmoji, tvTitle, tvPreview, tvDate;
            JVH(@NonNull View v) {
                super(v);
                tvEmoji   = v.findViewById(R.id.tvJournalEmoji);
                tvTitle   = v.findViewById(R.id.tvJournalTitle);
                tvPreview = v.findViewById(R.id.tvJournalPreview);
                tvDate    = v.findViewById(R.id.tvJournalDate);
            }
        }
    }
}
