package com.mindease.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.mindease.app.databinding.ActivityStarCatcherBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Falling-star catcher mini-game. Stars spawn at the top of the screen
// and the player moves a basket by dragging to catch them before they
// fall off-screen. Tracks lives, score, and persistent high scores.
// Contains an inner StarView custom View for rendering.
public class StarCatcherActivity extends AppCompatActivity {

    private ActivityStarCatcherBinding binding;
    private int score = 0;
    private int lives = 3;
    private int highScore = 0;
    private boolean isGameOver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStarCatcherBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        highScore = SharedPrefManager.getInstance(this).getStarCatcherHighScore();
        binding.tvHighScore.setText("High: " + highScore);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnPlayAgain.setOnClickListener(v -> restartGame());
        binding.btnExit.setOnClickListener(v -> finish());

        binding.starView.setActivity(this);
        restartGame();
    }

    private void restartGame() {
        score = 0;
        lives = 3;
        isGameOver = false;
        binding.tvScore.setText("Score: 0");
        binding.tvLives.setText("❤️❤️❤️");
        binding.layoutGameOver.setVisibility(View.GONE);
        binding.starView.resetGame();
    }

    public void onStarEscaped() {
        if (isGameOver) return;
        runOnUiThread(() -> {
            lives--;
            updateLivesDisplay();
            if (lives <= 0) {
                endGame();
            }
        });
    }

    private void updateLivesDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lives; i++) {
            sb.append("❤️");
        }
        if (lives <= 0) {
            sb.append("💔");
        }
        binding.tvLives.setText(sb.toString());
    }

    public void incrementScore() {
        if (isGameOver) return;
        score++;
        binding.tvScore.setText("Score: " + score);
    }

    private void endGame() {
        isGameOver = true;
        binding.layoutGameOver.setVisibility(View.VISIBLE);
        binding.tvFinalScore.setText("Final Score: " + score);
        
        if (score > highScore) {
            highScore = score;
            SharedPrefManager.getInstance(this).setStarCatcherHighScore(highScore);
            binding.tvHighScore.setText("High: " + highScore);
            binding.tvGameOverHighScore.setText("New High Score! 🎉");
        } else {
            binding.tvGameOverHighScore.setText("High Score: " + highScore);
        }
        binding.starView.stopGame();
    }

    // Custom View that renders falling stars and a draggable catch basket.
    public static class StarView extends View {

        private static final long SPAWN_INTERVAL_MS = 1000;
        private static final long GAME_TICK_MS = 16;      // ~60 FPS
        private static final float FALL_SPEED = 8f;
        private static final float BASKET_HALF_WIDTH = 70f;
        private static final float CATCH_ZONE_HEIGHT = 80f;

        private final List<Star> stars = new ArrayList<>();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Random rng = new Random();
        private StarCatcherActivity activity;
        private float basketX = 200;
        private boolean isRunning = false;

        private final Runnable spawnRunnable = new Runnable() {
            @Override public void run() {
                if (!isRunning) return;
                if (getWidth() > 0)
                    stars.add(new Star(rng.nextFloat() * getWidth(), 0));
                handler.postDelayed(this, SPAWN_INTERVAL_MS);
            }
        };

        private final Runnable gameLoopRunnable = new Runnable() {
            @Override public void run() {
                if (!isRunning) return;
                for (int i = stars.size() - 1; i >= 0; i--) {
                    stars.get(i).y += FALL_SPEED;
                    Star s = stars.get(i);
                    // Check if star landed inside the basket's catch zone
                    if (s.y > getHeight() - CATCH_ZONE_HEIGHT && Math.abs(s.x - basketX) < BASKET_HALF_WIDTH) {
                        stars.remove(i);
                        if (activity != null) {
                            activity.incrementScore();
                        }
                    } else if (s.y > getHeight()) {
                        stars.remove(i);
                        if (activity != null) {
                            activity.onStarEscaped();
                        }
                    }
                }
                invalidate();
                handler.postDelayed(this, GAME_TICK_MS);
            }
        };

        public StarView(Context ctx, AttributeSet attrs) {
            super(ctx, attrs);
        }

        public void setActivity(StarCatcherActivity act) {
            this.activity = act;
        }

        public void resetGame() {
            stopGame();
            stars.clear();
            isRunning = true;
            handler.post(spawnRunnable);
            handler.post(gameLoopRunnable);
        }

        public void stopGame() {
            isRunning = false;
            handler.removeCallbacks(spawnRunnable);
            handler.removeCallbacks(gameLoopRunnable);
        }

        @Override protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.parseColor("#F7FAFF"));
            paint.setTextSize(90f);
            for (Star s : stars) canvas.drawText("⭐", s.x, s.y, paint);
            
            // Basket
            paint.setColor(Color.parseColor("#2E9E8F"));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(basketX - 70, getHeight() - 60, basketX + 70, getHeight() - 20, 20, 20, paint);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (isRunning) {
                basketX = e.getX();
            }
            return true;
        }

        static class Star { float x, y; Star(float x, float y) { this.x = x; this.y = y; } }
    }
}
