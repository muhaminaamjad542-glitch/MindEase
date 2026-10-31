package com.mindease.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.mindease.app.databinding.ActivityOtpBinding;
import java.util.Locale;

// OTP verification screen for the password reset flow. Presents six
// single-digit input fields with auto-focus advancement and a resend
// countdown timer. Currently bypassed in favor of Firebase's built-in
// email reset link mechanism.
public class OtpActivity extends AppCompatActivity {

    private ActivityOtpBinding binding;
    private String email;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        email = getIntent().getStringExtra("email");
        if (email != null) binding.tvEmailHint.setText("Code sent to " + email);

        setupOtpFields();
        startCountdown();

        binding.btnVerify.setOnClickListener(v -> {
            startActivity(new Intent(this, ResetPasswordActivity.class));
            finish();
        });

        binding.tvResend.setOnClickListener(v -> {
            if (email != null) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnSuccessListener(u -> {
                            Toast.makeText(this, "Code resent!", Toast.LENGTH_SHORT).show();
                            startCountdown();
                        });
            }
        });

        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupOtpFields() {
        EditText[] boxes = {
                binding.otp1, binding.otp2, binding.otp3,
                binding.otp4, binding.otp5, binding.otp6
        };
        for (int i = 0; i < boxes.length; i++) {
            final int idx = i;
            boxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && idx < boxes.length - 1) {
                        boxes[idx + 1].requestFocus();
                    }
                }
            });
        }
    }

    private void startCountdown() {
        if (countDownTimer != null) countDownTimer.cancel();
        binding.tvResend.setEnabled(false);
        countDownTimer = new CountDownTimer(5 * 60 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long min = millisUntilFinished / 60000;
                long sec = (millisUntilFinished % 60000) / 1000;
                binding.tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
            }
            @Override
            public void onFinish() {
                binding.tvTimer.setText("00:00");
                binding.tvResend.setEnabled(true);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
