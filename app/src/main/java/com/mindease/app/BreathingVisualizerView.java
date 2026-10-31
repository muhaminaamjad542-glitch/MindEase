package com.mindease.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Custom View that renders an animated lung visualization for the
// breathing exercises. Smoothly scales, recolors, and draws particle
// effects based on the current breathing phase (Inhale, Hold, Exhale)
// and progress value provided by BreathingActivity.
public class BreathingVisualizerView extends View {
    public static final int MODE_LUNGS = 0;
    public static final int MODE_SQUARE = 1;
    
    private int animationMode = MODE_SQUARE; // Default to square since box is default
    private int currentPhaseIndex = 0;
    private int[] technique = null;

    public void setTechnique(int[] technique) {
        this.technique = technique;
        invalidate();
    }

    private float lungsScale = 1.0f;
    private String currentPhase = "Inhale"; // Inhale, Hold, Exhale, Hold
    private float phaseProgress = 0f; // 0 to 1

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path leftLungPath = new Path();
    private final Path rightLungPath = new Path();
    private final List<AirParticle> particles = new ArrayList<>();
    private final Random rng = new Random();
    
    private int primaryColor = Color.parseColor("#2E9E8F");
    private int accentColor = Color.parseColor("#FCBA4A");
    
    public BreathingVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAnimationMode(int mode) {
        this.animationMode = mode;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // Define left lung path relative to center (0, 0)
        leftLungPath.reset();
        leftLungPath.moveTo(0, -60);
        leftLungPath.cubicTo(-60, -90, -110, -30, -110, 30);
        leftLungPath.cubicTo(-110, 80, -70, 110, -20, 100);
        leftLungPath.cubicTo(-10, 95, 0, 50, 0, 20);
        leftLungPath.close();

        // Define right lung path (mirrored)
        rightLungPath.reset();
        rightLungPath.moveTo(0, -60);
        rightLungPath.cubicTo(60, -90, 110, -30, 110, 30);
        rightLungPath.cubicTo(110, 80, 70, 110, 20, 100);
        rightLungPath.cubicTo(10, 95, 0, 50, 0, 20);
        rightLungPath.close();
    }

    public void setPhase(String phase, float progress) {
        setPhase(phase, 0, progress);
    }

    public void setPhase(String phase, int phaseIndex, float progress) {
        this.currentPhase = phase;
        this.currentPhaseIndex = phaseIndex;
        this.phaseProgress = progress;
        
        if (animationMode == MODE_LUNGS) {
            if ("Inhale".equalsIgnoreCase(phase)) {
                // Scale goes from 0.8 to 1.3
                lungsScale = 0.8f + progress * 0.5f;
                generateParticlesInflow();
            } else if ("Exhale".equalsIgnoreCase(phase)) {
                // Scale goes from 1.3 to 0.8
                lungsScale = 1.3f - progress * 0.5f;
                generateParticlesOutflow();
            } else {
                // Hold: pulse slightly
                float pulse = (float) Math.sin(System.currentTimeMillis() / 200.0) * 0.04f;
                lungsScale = "Hold".equalsIgnoreCase(phase) && progress > 0.5f ? 0.8f + pulse : 1.3f + pulse;
                generateParticlesHold();
            }
            updateParticles();
        }
        invalidate();
    }

    private void generateParticlesInflow() {
        if (rng.nextFloat() < 0.4f) {
            float angle = rng.nextFloat() * 2 * (float)Math.PI;
            float dist = 280 + rng.nextFloat() * 120;
            float px = getWidth()/2f + (float)Math.cos(angle) * dist;
            float py = getHeight()/2f - 80 + (float)Math.sin(angle) * dist;
            particles.add(new AirParticle(px, py, true));
        }
    }

    private void generateParticlesOutflow() {
        if (rng.nextFloat() < 0.4f) {
            float px = getWidth()/2f + (rng.nextFloat() - 0.5f) * 60;
            float py = getHeight()/2f - 60 + (rng.nextFloat() - 0.5f) * 60;
            particles.add(new AirParticle(px, py, false));
        }
    }

    private void generateParticlesHold() {
        if (rng.nextFloat() < 0.1f) {
            float px = getWidth()/2f + (rng.nextFloat() - 0.5f) * 160;
            float py = getHeight()/2f + (rng.nextFloat() - 0.5f) * 160;
            particles.add(new AirParticle(px, py, false));
        }
    }

    private void updateParticles() {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f - 60;
        
        for (int i = particles.size() - 1; i >= 0; i--) {
            AirParticle p = particles.get(i);
            p.life -= 0.015f;
            if (p.life <= 0) {
                particles.remove(i);
                continue;
            }
            
            if (p.isInflow) {
                p.x += (cx - p.x) * 0.08f;
                p.y += (cy - p.y) * 0.08f;
            } else {
                p.x += p.vx;
                p.y += p.vy;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (animationMode == MODE_SQUARE) {
            drawSquareAnimation(canvas);
        } else {
            drawLungsAnimation(canvas);
        }
    }

    private void drawSquareAnimation(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float density = getResources().getDisplayMetrics().density;

        // Size of the square Box Breathing visualizer
        float size = Math.min(w, h) * 0.55f;
        float half = size / 2f;
        float left = cx - half;
        float right = cx + half;
        float top = cy - half;
        float bottom = cy + half;

        // 1. Draw glowing background radial gradient
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        
        int glowColor;
        if ("Inhale".equalsIgnoreCase(currentPhase)) {
            glowColor = Color.argb((int)(60 * phaseProgress), 0, 149, 152); // #009598
        } else if ("Exhale".equalsIgnoreCase(currentPhase)) {
            glowColor = Color.argb((int)(60 * (1 - phaseProgress)), 0, 149, 152);
        } else {
            glowColor = Color.argb(45, 149, 222, 221); // #95DEDD
        }
        
        RadialGradient radial = new RadialGradient(cx, cy, size * 0.8f, glowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP);
        paint.setShader(radial);
        canvas.drawCircle(cx, cy, size * 0.9f, paint);
        paint.setShader(null);

        // 2. Draw square filled background (#009598 with opacity)
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#009598"));
        paint.setAlpha(200);
        float cornerRadius = 12 * density;
        canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, paint);

        // 3. Draw square border (#95DEDD)
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#95DEDD"));
        paint.setStrokeWidth(6 * density);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, paint);

        // 4. Calculate dot position (moving around the square perimeter depending on technique)
        float dotX = left;
        float dotY = bottom;

        boolean isBox = (technique == null || technique.length < 4 || (technique[1] > 0 && technique[3] > 0));
        boolean is478 = (technique != null && technique.length >= 4 && technique[1] > 0 && technique[3] == 0);
        boolean isDeep = (technique != null && technique.length >= 4 && technique[1] == 0);

        if (isBox) {
            if ("Inhale".equalsIgnoreCase(currentPhase)) {
                // Move UP the left edge
                dotX = left;
                dotY = bottom - phaseProgress * size;
            } else if ("Hold".equalsIgnoreCase(currentPhase) && currentPhaseIndex == 1) {
                // Move RIGHT along the top edge
                dotX = left + phaseProgress * size;
                dotY = top;
            } else if ("Exhale".equalsIgnoreCase(currentPhase)) {
                // Move DOWN the right edge
                dotX = right;
                dotY = top + phaseProgress * size;
            } else if ("Hold".equalsIgnoreCase(currentPhase) && currentPhaseIndex == 3) {
                // Move LEFT along the bottom edge
                dotX = right - phaseProgress * size;
                dotY = bottom;
            }
        } else if (is478) {
            if ("Inhale".equalsIgnoreCase(currentPhase)) {
                // Inhale: Move UP the left edge
                dotX = left;
                dotY = bottom - phaseProgress * size;
            } else if ("Hold".equalsIgnoreCase(currentPhase)) {
                // Hold (7s): Move RIGHT across the top edge (0 to 0.5) then DOWN the right edge (0.5 to 1)
                if (phaseProgress <= 0.5f) {
                    dotX = left + (phaseProgress * 2f) * size;
                    dotY = top;
                } else {
                    dotX = right;
                    dotY = top + ((phaseProgress - 0.5f) * 2f) * size;
                }
            } else if ("Exhale".equalsIgnoreCase(currentPhase)) {
                // Exhale (8s): Move LEFT along the bottom edge
                dotX = right - phaseProgress * size;
                dotY = bottom;
            }
        } else if (isDeep) {
            if ("Inhale".equalsIgnoreCase(currentPhase)) {
                // Inhale (4s): Move UP the left edge (0 to 0.5) then RIGHT along the top edge (0.5 to 1)
                if (phaseProgress <= 0.5f) {
                    dotX = left;
                    dotY = bottom - (phaseProgress * 2f) * size;
                } else {
                    dotX = left + ((phaseProgress - 0.5f) * 2f) * size;
                    dotY = top;
                }
            } else if ("Exhale".equalsIgnoreCase(currentPhase)) {
                // Exhale (6s): Move DOWN the right edge (0 to 0.5) then LEFT along the bottom edge (0.5 to 1)
                if (phaseProgress <= 0.5f) {
                    dotX = right;
                    dotY = top + (phaseProgress * 2f) * size;
                } else {
                    dotX = right - ((phaseProgress - 0.5f) * 2f) * size;
                    dotY = bottom;
                }
            }
        }

        // 5. Draw the glowing dot cursor
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#95DEDD"));
        paint.setShadowLayer(8 * density, 0, 0, Color.parseColor("#95DEDD"));
        canvas.drawCircle(dotX, dotY, 12 * density, paint);
    }

    private void drawLungsAnimation(Canvas canvas) {
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        // Draw radial glowing background gradient
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        
        int glowColor;
        if ("Inhale".equalsIgnoreCase(currentPhase)) {
            glowColor = Color.argb((int)(60 * phaseProgress), 46, 158, 143);
        } else if ("Exhale".equalsIgnoreCase(currentPhase)) {
            glowColor = Color.argb((int)(60 * (1 - phaseProgress)), 46, 158, 143);
        } else {
            glowColor = Color.argb(45, 252, 186, 74);
        }
        
        RadialGradient radial = new RadialGradient(cx, cy, 260, glowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP);
        paint.setShader(radial);
        canvas.drawCircle(cx, cy, 280, paint);
        paint.setShader(null);

        // Draw air particles
        paint.setStyle(Paint.Style.FILL);
        for (AirParticle p : particles) {
            paint.setColor(Color.WHITE);
            paint.setAlpha((int) (p.life * 230));
            canvas.drawCircle(p.x, p.y, p.size, paint);
        }

        // Draw lungs
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(lungsScale, lungsScale);

        int colorStart, colorEnd;
        if ("Inhale".equalsIgnoreCase(currentPhase)) {
            colorStart = primaryColor;
            colorEnd = Color.parseColor("#4A90E2");
        } else if ("Exhale".equalsIgnoreCase(currentPhase)) {
            colorStart = Color.parseColor("#2E9E8F");
            colorEnd = Color.parseColor("#81C784");
        } else {
            colorStart = accentColor;
            colorEnd = primaryColor;
        }

        LinearGradient lungGradient = new LinearGradient(-100, -100, 100, 100, colorStart, colorEnd, Shader.TileMode.CLAMP);
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(lungGradient);
        paint.setAlpha(195);

        canvas.drawPath(leftLungPath, paint);
        canvas.drawPath(rightLungPath, paint);
        paint.setShader(null);

        // Draw internal bronchi/bronchioles lines
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setAlpha(130);
        paint.setStrokeWidth(3f);
        
        canvas.drawLine(0, -60, 0, 10, paint);
        canvas.drawLine(0, 10, -35, 45, paint);
        canvas.drawLine(0, 10, 35, 45, paint);
        canvas.drawLine(-35, 45, -65, 35, paint);
        canvas.drawLine(-35, 45, -45, 75, paint);
        canvas.drawLine(35, 45, 65, 35, paint);
        canvas.drawLine(35, 45, 45, 75, paint);

        canvas.restore();
    }

    private static class AirParticle {
        float x, y;
        float vx, vy;
        float size;
        float life = 1.0f;
        boolean isInflow;

        AirParticle(float x, float y, boolean isInflow) {
            this.x = x;
            this.y = y;
            this.isInflow = isInflow;
            Random rng = new Random();
            this.size = 5 + rng.nextFloat() * 6;
            if (!isInflow) {
                float angle = rng.nextFloat() * 2 * (float)Math.PI;
                float speed = 1.5f + rng.nextFloat() * 3.5f;
                this.vx = (float)Math.cos(angle) * speed;
                this.vy = (float)Math.sin(angle) * speed;
            }
        }
    }
}
