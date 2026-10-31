package com.mindease.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.mindease.app.databinding.ActivityEmergencyBinding;

// Emergency SOS screen providing quick-dial access to mental health
// crisis hotlines and an emergency call button with a confirmation
// dialog. Handles CALL_PHONE runtime permission for direct dialing.
public class EmergencyActivity extends AppCompatActivity {

    private ActivityEmergencyBinding binding;
    private static final int REQ_CALL = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmergencyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnSOS.setOnClickListener(v -> showSOSDialog());

        binding.btnCall1.setOnClickListener(v -> dial("1800-599-0019"));
        binding.btnCall2.setOnClickListener(v -> dial("0311-7786264"));
        binding.btnCall3.setOnClickListener(v -> dial("042-35761999"));
        binding.btnCall4.setOnClickListener(v -> dial("1122"));
    }

    private void showSOSDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🆘 Emergency Call")
                .setMessage("This will call emergency services. Are you sure?")
                .setPositiveButton("Yes, Call Now", (d, w) -> makeEmergencyCall("03174288665"))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void makeEmergencyCall(String number) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number)));
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE}, REQ_CALL);
        }
    }

    private void dial(String number) {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number)));
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == REQ_CALL && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            makeEmergencyCall("03174288665");
        }
    }
}
