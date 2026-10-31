package com.mindease.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mindease.app.databinding.ActivityQuoteBinding;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// Daily motivational quote screen. Loads quotes from a local JSON asset,
// displays one based on the current day, and allows users to save
// favourites to their Firestore collection for later review.
public class QuoteActivity extends AppCompatActivity {

    private ActivityQuoteBinding binding;
    private JSONArray quotes;
    private JSONObject currentQuote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        loadQuotes();
        showDailyQuote();

        binding.btnNewQuote.setOnClickListener(v -> showRandomQuote());
        binding.btnShare.setOnClickListener(v -> shareQuote());
        binding.btnSave.setOnClickListener(v -> saveQuote());
    }

    private void loadQuotes() {
        try {
            InputStream is = getResources().openRawResource(R.raw.quotes);
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();
            quotes = new JSONArray(new String(buf));
        } catch (Exception e) {
            quotes = new JSONArray();
        }
    }

    private void showDailyQuote() {
        if (quotes.length() == 0) return;
        try {
            int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            currentQuote = quotes.getJSONObject(day % quotes.length());
            displayQuote();
        } catch (Exception e) { showRandomQuote(); }
    }

    private void showRandomQuote() {
        if (quotes.length() == 0) return;
        try {
            int idx = new Random().nextInt(quotes.length());
            currentQuote = quotes.getJSONObject(idx);
            displayQuote();
        } catch (Exception ignored) {}
    }

    private void displayQuote() throws Exception {
        binding.tvQuoteText.setText("\"" + currentQuote.getString("text") + "\"");
        binding.tvQuoteAuthor.setText("— " + currentQuote.getString("author"));
    }

    private void shareQuote() {
        if (currentQuote == null) return;
        try {
            String text = "\"" + currentQuote.getString("text") + "\"\n— " + currentQuote.getString("author")
                    + "\n\nShared via MindEase 🧠";
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(share, "Share Quote"));
        } catch (Exception ignored) {}
    }

    private void saveQuote() {
        if (currentQuote == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;
        try {
            Map<String, Object> q = new HashMap<>();
            q.put("text", currentQuote.getString("text"));
            q.put("author", currentQuote.getString("author"));
            q.put("savedAt", FieldValue.serverTimestamp());
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .collection("savedQuotes").add(q)
                    .addOnSuccessListener(v -> Toast.makeText(this, "Quote saved! ✅", Toast.LENGTH_SHORT).show());
        } catch (Exception ignored) {}
    }
}
