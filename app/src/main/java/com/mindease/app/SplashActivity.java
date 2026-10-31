package com.mindease.app;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

// Splash screen displayed on app launch. Shows the MindEase branding for
// a brief delay, then routes the user to the appropriate destination based
// on their authentication state, onboarding status, and PIN preference.
public class SplashActivity extends AppCompatActivity {

    // Duration in milliseconds the splash logo stays visible.
    private static final long SPLASH_DELAY_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView ivLogo = findViewById(R.id.ivLogo);
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvSubtitle = findViewById(R.id.tvSubtitle);

        // Logo pop animation
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.5f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.5f, 1f);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f);
        ObjectAnimator logoAnimator = ObjectAnimator.ofPropertyValuesHolder(ivLogo, scaleX, scaleY, alpha);
        logoAnimator.setDuration(800);
        logoAnimator.setInterpolator(new OvershootInterpolator());
        logoAnimator.start();

        // Text slide and fade
        tvTitle.setAlpha(0f);
        tvTitle.setTranslationY(50f);
        tvTitle.animate().alpha(1f).translationY(0f).setDuration(800).setStartDelay(300).start();

        tvSubtitle.setAlpha(0f);
        tvSubtitle.setTranslationY(50f);
        tvSubtitle.animate().alpha(1f).translationY(0f).setDuration(800).setStartDelay(500).start();

        // Delay routing to let animations play
        new Handler(Looper.getMainLooper()).postDelayed(this::navigate, SPLASH_DELAY_MS);
    }

    private void navigate() {
        SharedPrefManager spm = SharedPrefManager.getInstance(this);
        Intent intent;

        if (!spm.isOnboardingDone()) {
            intent = new Intent(this, OnboardingActivity.class);
        } else if (spm.isPinEnabled()) {
            intent = new Intent(this, PinLockActivity.class);
        } else if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            intent = new Intent(this, HomeActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
