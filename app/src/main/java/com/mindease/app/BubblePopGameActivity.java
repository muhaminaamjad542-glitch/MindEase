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
import com.mindease.app.databinding.ActivityBubblePopBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Stress-relief mini-game where colorful bubbles float upward and the
// player taps to pop them. Features a lives system (3 lives), persistent
// high score tracking via SharedPreferences, and a game-over overlay.
// Contains an inner BubbleView custom View for rendering.
public class BubblePopGameActivity extends AppCompatActivity {

    private ActivityBubblePopBinding binding;
    private int score = 0;
    private int lives = 3;
    private int highScore = 0;
    private boolean isGameOver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBubblePopBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        highScore = SharedPrefManager.getInstance(this).getBubblePopHighScore();
        binding.tvHighScore.setText("High: " + highScore);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnPlayAgain.setOnClickListener(v -> restartGame());
        binding.btnExit.setOnClickListener(v -> finish());

        binding.bubbleView.setActivity(this);
        restartGame();
    }

    private void restartGame() {
        score = 0;
        lives = 3;
        isGameOver = false;
        binding.tvScore.setText("Score: 0");
        binding.tvLives.setText("❤️❤️❤️");
        binding.layoutGameOver.setVisibility(View.GONE);
        binding.bubbleView.resetGame();
    }

    public void onBubbleEscaped() {
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
            SharedPrefManager.getInstance(this).setBubblePopHighScore(highScore);
            binding.tvHighScore.setText("High: " + highScore);
            binding.tvGameOverHighScore.setText("New High Score! 🎉");
        } else {
            binding.tvGameOverHighScore.setText("High Score: " + highScore);
        }
        binding.bubbleView.stopSpawning();
    }

    // Custom View that renders and animates floating bubbles.
    public static class BubbleView extends View {

        private static final long SPAWN_INTERVAL_MS = 1200;
        private static final long GAME_TICK_MS = 16;   // ~60 FPS
        private static final float MIN_RADIUS = 40f;
        private static final float MAX_RADIUS = 90f;
        private static final float MIN_SPEED = 3f;
        private static final float MAX_SPEED = 8f;

        private final List<Bubble> bubbles = new ArrayList<>();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random rng = new Random();
        private final Handler handler = new Handler(Looper.getMainLooper());
        private BubblePopGameActivity activity;
        private boolean isRunning = false;

        // Bubble color palette — teal, orange, purple, blue, red.
        private final int[] colors = {
            Color.parseColor("#2E9E8F"), Color.parseColor("#FF8C38"),
            Color.parseColor("#8B5CF6"), Color.parseColor("#3B82F6"),
            Color.parseColor("#EF4444")
        };

        private final Runnable spawnRunnable = new Runnable() {
            @Override public void run() {
                if (!isRunning) return;
                if (getWidth() > 0) {
                    // Randomize radius, horizontal position, and rise speed
                    float r = MIN_RADIUS + rng.nextFloat() * (MAX_RADIUS - MIN_RADIUS);
                    float x = r + rng.nextFloat() * (getWidth() - 2 * r);
                    float y = getHeight() + r;
                    float speed = MIN_SPEED + rng.nextFloat() * (MAX_SPEED - MIN_SPEED);
                    bubbles.add(new Bubble(x, y, r, colors[rng.nextInt(colors.length)], speed));
                    invalidate();
                }
                handler.postDelayed(this, SPAWN_INTERVAL_MS);
            }
        };

        private final Runnable gameLoopRunnable = new Runnable() {
            @Override public void run() {
                if (!isRunning) return;
                for (int i = bubbles.size() - 1; i >= 0; i--) {
                    Bubble b = bubbles.get(i);
                    b.y -= b.speed;
                    if (b.y < -b.radius) {
                        bubbles.remove(i);
                        if (activity != null) {
                            activity.onBubbleEscaped();
                        }
                    }
                }
                invalidate();
                handler.postDelayed(this, GAME_TICK_MS);
            }
        };

        public BubbleView(Context ctx, AttributeSet attrs) {
            super(ctx, attrs);
        }

        public void setActivity(BubblePopGameActivity act) {
            this.activity = act;
        }

        public void resetGame() {
            stopSpawning();
            bubbles.clear();
            isRunning = true;
            handler.post(spawnRunnable);
            handler.post(gameLoopRunnable);
        }

        public void stopSpawning() {
            isRunning = false;
            handler.removeCallbacks(spawnRunnable);
            handler.removeCallbacks(gameLoopRunnable);
        }

        @Override protected void onDraw(Canvas canvas) {
            for (Bubble b : bubbles) {
                paint.setColor(b.color);
                paint.setAlpha(180);
                canvas.drawCircle(b.x, b.y, b.radius, paint);
                paint.setColor(Color.WHITE);
                paint.setAlpha(100);
                canvas.drawCircle(b.x - b.radius * 0.3f, b.y - b.radius * 0.3f, b.radius * 0.2f, paint);
            }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN && isRunning) {
                float tx = e.getX(), ty = e.getY();
                for (int i = bubbles.size() - 1; i >= 0; i--) {
                    Bubble b = bubbles.get(i);
                    float dx = tx - b.x, dy = ty - b.y;
                    // Circle hit test: point is inside if distance² <= radius²
                    if (dx * dx + dy * dy <= b.radius * b.radius) {
                        bubbles.remove(i);
                        if (activity != null) {
                            activity.incrementScore();
                        }
                        invalidate();
                        break;
                    }
                }
            }
            return true;
        }

        static class Bubble {
            float x, y, radius, speed; int color;
            Bubble(float x, float y, float r, int c, float s) {
                this.x = x; this.y = y; radius = r; color = c; speed = s;
            }
        }
    }
}
