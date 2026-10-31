package com.mindease.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.mindease.app.databinding.ActivityNotificationPermissionBinding;

// Requests the POST_NOTIFICATIONS runtime permission (required on Android 13+).
// Shown once after first login, records the permission state in SharedPreferences
// to avoid re-prompting on subsequent launches.
public class NotificationPermissionActivity extends AppCompatActivity {

    private static final int REQ_NOTIF = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityNotificationPermissionBinding binding =
                ActivityNotificationPermissionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPrefManager.getInstance(this).setOnboardingDone();

        binding.btnAllow.setOnClickListener(v -> requestPermission());
        binding.btnLater.setOnClickListener(v -> goToLogin());
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
        } else {
            goToLogin();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        goToLogin(); // proceed regardless of grant/deny
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
