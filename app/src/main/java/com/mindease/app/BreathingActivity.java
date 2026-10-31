package com.mindease.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.animation.LinearInterpolator;
import androidx.appcompat.app.AppCompatActivity;
import com.mindease.app.databinding.ActivityBreathingBinding;
import java.util.Locale;

// Guided breathing exercise screen. Offers three selectable techniques
// (Box Breathing 4-4-4-4, 4-7-8, and Deep Calm 4-6) with animated
// visual feedback via BreathingVisualizerView. Runs through
// a configurable number of rounds with pause/resume support.
public class BreathingActivity extends AppCompatActivity {

    private ActivityBreathingBinding binding;
    private ValueAnimator phaseAnimator;
    private boolean isPaused = false;
    private int currentRound = 0;
    private int currentPhaseIndex = 0;
    private static final int TOTAL_ROUNDS = 5;

    // Selected technique: {inhale, hold, exhale, hold2} in seconds
    private int[] technique = {4, 4, 4, 4}; // default: Box breathing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBreathingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> { stopAnimation(); finish(); });

        binding.btnBox.setOnClickListener(v -> {
            technique = new int[]{4, 4, 4, 4};
            binding.tvTechnique.setText("Box Breathing (4-4-4-4)");
            binding.breathVisualizer.setAnimationMode(BreathingVisualizerView.MODE_SQUARE);
            startBreathing();
        });
        binding.btn478.setOnClickListener(v -> {
            technique = new int[]{4, 7, 8, 0};
            binding.tvTechnique.setText("4-7-8 Technique");
            binding.breathVisualizer.setAnimationMode(BreathingVisualizerView.MODE_SQUARE);
            startBreathing();
        });
        binding.btnDeep.setOnClickListener(v -> {
            technique = new int[]{4, 0, 6, 0};
            binding.tvTechnique.setText("Deep Calm (4-6)");
            binding.breathVisualizer.setAnimationMode(BreathingVisualizerView.MODE_SQUARE);
            startBreathing();
        });

        binding.btnPause.setOnClickListener(v -> togglePause());

        binding.breathVisualizer.setAnimationMode(BreathingVisualizerView.MODE_SQUARE);
        startBreathing();
    }

    private void startBreathing() {
        stopAnimation();
        isPaused = false;
        binding.btnPause.setText("⏸ Pause");
        currentRound = 0;
        binding.progressRounds.setMax(TOTAL_ROUNDS);
        binding.progressRounds.setProgress(0);
        binding.tvRounds.setText("0/" + TOTAL_ROUNDS + " rounds");
        binding.breathVisualizer.setTechnique(technique);
        runPhase(0);
    }

    private void runPhase(int phaseIndex) {
        currentPhaseIndex = phaseIndex;
        String[] phaseNames = {"Inhale", "Hold", "Exhale", "Hold"};
        int duration = technique[phaseIndex] * 1000;
        
        if (duration == 0) {
            transitionToPhase((phaseIndex + 1) % 4);
            return;
        }

        binding.tvPhase.setText(phaseNames[phaseIndex]);

        stopAnimation();

        phaseAnimator = ValueAnimator.ofFloat(0f, 1f);
        phaseAnimator.setDuration(duration);
        phaseAnimator.setInterpolator(new LinearInterpolator());
        
        phaseAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            binding.breathVisualizer.setPhase(phaseNames[phaseIndex], phaseIndex, progress);
            
            float remaining = (duration * (1f - progress)) / 1000f;
            binding.tvTimer.setText(String.format(Locale.getDefault(), "%.1f", remaining));
        });

        phaseAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isPaused) return;
                int next = (phaseIndex + 1) % 4;
                transitionToPhase(next);
            }
        });

        phaseAnimator.start();
    }

    private void transitionToPhase(int next) {
        if (next == 0) {
            currentRound++;
            binding.progressRounds.setProgress(currentRound);
            binding.tvRounds.setText(currentRound + "/" + TOTAL_ROUNDS + " rounds");
            if (currentRound >= TOTAL_ROUNDS) {
                binding.tvPhase.setText("Complete! 🎉");
                binding.tvTimer.setText("Done");
                binding.breathVisualizer.setPhase("Complete", 1.0f);
                return;
            }
        }
        runPhase(next);
    }

    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            if (phaseAnimator != null && phaseAnimator.isRunning()) {
                phaseAnimator.pause();
            }
            binding.btnPause.setText("▶ Resume");
        } else {
            binding.btnPause.setText("⏸ Pause");
            if (phaseAnimator != null && phaseAnimator.isPaused()) {
                phaseAnimator.resume();
            } else {
                runPhase(currentPhaseIndex);
            }
        }
    }

    private void stopAnimation() {
        if (phaseAnimator != null) {
            phaseAnimator.removeAllUpdateListeners();
            phaseAnimator.removeAllListeners();
            phaseAnimator.cancel();
            phaseAnimator = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAnimation();
    }
}
