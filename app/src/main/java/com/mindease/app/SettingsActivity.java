package com.mindease.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.auth.FirebaseAuth;
import com.mindease.app.databinding.ActivitySettingsBinding;

// Application settings screen. Provides toggles for dark mode, PIN
// security management (set/change/remove), and account deletion with
// a confirmation dialog to prevent accidental data loss.
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        // Dark mode
        boolean darkMode = SharedPrefManager.getInstance(this).isDarkMode();
        binding.switchDarkMode.setChecked(darkMode);
        binding.switchDarkMode.setOnCheckedChangeListener((btn, checked) -> {
            SharedPrefManager.getInstance(this).setDarkMode(checked);
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });

        binding.btnPinSecurity.setOnClickListener(v -> showPinOptions());

        binding.btnDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());
    }

    private void showPinOptions() {
        SharedPrefManager spm = SharedPrefManager.getInstance(this);
        String[] options = spm.isPinEnabled()
                ? new String[]{"Change PIN", "Remove PIN"}
                : new String[]{"Set up PIN"};

        new AlertDialog.Builder(this)
                .setTitle("PIN & Security")
                .setItems(options, (d, which) -> {
                    if (options[which].equals("Remove PIN")) {
                        spm.clearPin();
                        Toast.makeText(this, "PIN removed", Toast.LENGTH_SHORT).show();
                    } else {
                        startActivity(new Intent(this, PinSetupActivity.class));
                    }
                }).show();
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("This will permanently delete your account and all data. This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            FirebaseAuth.getInstance().getCurrentUser().delete()
                    .addOnSuccessListener(v -> {
                        SharedPrefManager.getInstance(this).clearPin();
                        startActivity(new Intent(this, LoginActivity.class));
                        finishAffinity();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}
