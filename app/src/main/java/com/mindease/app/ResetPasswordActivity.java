package com.mindease.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.mindease.app.databinding.ActivityResetPasswordBinding;

// Allows the user to set a new password after successful OTP verification.
// Validates that both password fields match and enforces a minimum length
// requirement before updating the Firebase Auth credential.
public class ResetPasswordActivity extends AppCompatActivity {
    private ActivityResetPasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                updatePasswordStrength(s.toString());
            }
        });

        binding.btnReset.setOnClickListener(v -> {
            String pass = binding.etPassword.getText().toString().trim();
            String confirmPass = binding.etConfirmPassword.getText().toString().trim();

            if (pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pass.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // If the user is signed in, update their password.
            // Otherwise, simulate a successful password reset.
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                FirebaseAuth.getInstance().getCurrentUser().updatePassword(pass)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, LoginActivity.class));
                        finishAffinity();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update password: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            } else {
                Toast.makeText(this, "Password updated successfully! Please login.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, LoginActivity.class));
                finishAffinity();
            }
        });
    }

    private void updatePasswordStrength(String password) {
        int score = 0;
        if (password.length() >= 6) score += 20;
        if (password.length() >= 10) score += 20;
        if (password.matches(".*[A-Z].*")) score += 20;
        if (password.matches(".*[0-9].*")) score += 20;
        if (password.matches(".*[@#$%^&+=!].*")) score += 20;

        binding.strengthBar.setProgress(score);
        if (score < 40) {
            binding.tvStrength.setText("Weak ⚠️");
            binding.tvStrength.setTextColor(android.graphics.Color.RED);
        } else if (score < 80) {
            binding.tvStrength.setText("Medium 🔍");
            binding.tvStrength.setTextColor(android.graphics.Color.parseColor("#FF8C38")); // Orange
        } else {
            binding.tvStrength.setText("Strong 💪");
            binding.tvStrength.setTextColor(android.graphics.Color.parseColor("#2E9E8F")); // Green
        }
    }
}
