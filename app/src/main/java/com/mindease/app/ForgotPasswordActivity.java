package com.mindease.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.mindease.app.databinding.ActivityForgotPasswordBinding;

// Password recovery screen. Sends a Firebase password reset email
// to the provided address and displays a confirmation dialog upon
// success, returning the user to the login screen.
public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnSendReset.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.btnSendReset.setEnabled(false);
            
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            new AlertDialog.Builder(this)
                                    .setTitle("Reset Link Sent ✉️")
                                    .setMessage("A password reset link has been sent to " + email + ". Please check your inbox and follow the instructions to reset your password, then return here to log in.")
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        finish();
                                    })
                                    .setCancelable(false)
                                    .show();
                        } else {
                            binding.btnSendReset.setEnabled(true);
                            String error = task.getException() != null ? task.getException().getMessage() : "Unknown error occurred.";
                            new AlertDialog.Builder(this)
                                    .setTitle("Error ❌")
                                    .setMessage(error)
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    });
        });
    }
}
