package com.mindease.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.mindease.app.databinding.ActivityPinLockBinding;
import java.util.concurrent.Executor;

// PIN lock screen displayed on app launch when PIN security is enabled.
// Supports both numeric PIN entry (with dot indicators) and biometric
// authentication via the AndroidX Biometric API as an alternative
// unlock method.
public class PinLockActivity extends AppCompatActivity {

    private ActivityPinLockBinding binding;
    private StringBuilder enteredPin = new StringBuilder();
    private ImageView[] dots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPinLockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dots = new ImageView[]{binding.dot1, binding.dot2, binding.dot3, binding.dot4};

        int[] btnIds = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };

        for (int id : btnIds) {
            Button btn = binding.getRoot().findViewById(id);
            btn.setOnClickListener(v -> onDigit(((Button) v).getText().toString()));
        }

        binding.btnBackspace.setOnClickListener(v -> onBackspace());
        binding.btnFingerprint.setVisibility(android.view.View.VISIBLE); // Ensure it's visible for debugging
        binding.btnFingerprint.setOnClickListener(v -> {
            // Fallback for biometric prompt trigger
            showBiometric();
        });
        showBiometric(); // auto-prompt on open
    }

    private void onDigit(String digit) {
        if (enteredPin.length() >= 4) return;
        enteredPin.append(digit);
        updateDots();
        if (enteredPin.length() == 4) checkPin();
    }

    private void onBackspace() {
        if (enteredPin.length() > 0) {
            enteredPin.deleteCharAt(enteredPin.length() - 1);
            updateDots();
        }
    }

    private void updateDots() {
        for (int i = 0; i < dots.length; i++) {
            dots[i].setImageResource(i < enteredPin.length()
                    ? R.drawable.circle_primary
                    : R.drawable.circle_gray);
        }
    }

    private void checkPin() {
        String savedPin = SharedPrefManager.getInstance(this).getPin();
        if (enteredPin.toString().equals(savedPin)) {
            goHome();
        } else {
            Toast.makeText(this, "Wrong PIN. Try again.", Toast.LENGTH_SHORT).show();
            enteredPin.setLength(0);
            updateDots();
        }
    }

    private void showBiometric() {
        // Trigger Biometric prompt
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK);
        
        Log.d("Biometric", "canAuthenticate result: " + canAuthenticate);

        switch (canAuthenticate) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                // Can authenticate
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                binding.btnFingerprint.setVisibility(android.view.View.GONE);
                return;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(this, "No biometric features available", Toast.LENGTH_SHORT).show();
                return;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this, "No fingerprints enrolled. Please set up fingerprint in phone settings.", Toast.LENGTH_LONG).show();
                return;
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                Toast.makeText(this, "A security update is required for biometric authentication.", Toast.LENGTH_SHORT).show();
                return;
            default:
                binding.btnFingerprint.setVisibility(android.view.View.GONE);
                return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        // Authentication succeeded
                        goHome();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            // User clicked "Use PIN" - do nothing, let them use PIN
                        } else if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_CANCELED) {
                            // User cancelled
                        } else {
                            Toast.makeText(PinLockActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(PinLockActivity.this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("MindEase")
                .setSubtitle("Use fingerprint to unlock")
                .setNegativeButtonText("Use PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build();

        Log.d("Biometric", "Starting authentication");
        biometricPrompt.authenticate(info);
    }

    private void goHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
