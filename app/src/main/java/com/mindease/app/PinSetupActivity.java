package com.mindease.app;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.mindease.app.databinding.ActivityPinSetupBinding;

// PIN creation and change screen. Guides the user through a two-step
// process: enter a 4-digit PIN, then confirm it. On match, the PIN
// is saved to SharedPreferences and PIN lock is enabled.
public class PinSetupActivity extends AppCompatActivity {

    private ActivityPinSetupBinding binding;
    private StringBuilder enteredPin = new StringBuilder();
    private String firstPin = "";
    private boolean isConfirming = false;
    private ImageView[] dots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPinSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dots = new ImageView[]{binding.dot1, binding.dot2, binding.dot3, binding.dot4};

        int[] btnIds = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };

        for (int id : btnIds) {
            MaterialButton btn = findViewById(id);
            btn.setOnClickListener(v -> onDigit(btn.getText().toString()));
        }

        binding.btnBackspace.setOnClickListener(v -> onBackspace());
        binding.btnCancel.setOnClickListener(v -> finish());
    }

    private void onDigit(String digit) {
        if (enteredPin.length() >= 4) return;
        enteredPin.append(digit);
        updateDots();

        if (enteredPin.length() == 4) {
            if (!isConfirming) {
                firstPin = enteredPin.toString();
                isConfirming = true;
                enteredPin.setLength(0);
                binding.tvTitle.setText("Confirm PIN");
                binding.tvSubtitle.setText("Enter your PIN again to confirm");
                updateDots();
            } else {
                if (enteredPin.toString().equals(firstPin)) {
                    SharedPrefManager.getInstance(this).setPin(firstPin);
                    Toast.makeText(this, "PIN set successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "PINs don't match. Start over.", Toast.LENGTH_SHORT).show();
                    resetSetup();
                }
            }
        }
    }

    private void resetSetup() {
        isConfirming = false;
        firstPin = "";
        enteredPin.setLength(0);
        binding.tvTitle.setText("Set Up PIN");
        binding.tvSubtitle.setText("Create a 4-digit PIN for security");
        updateDots();
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
}
