package com.mindease.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.mindease.app.databinding.ActivityZenDoodleBinding;

// Free-form drawing canvas for mindful doodling. Users can draw with
// touch gestures, switch pen colors, and clear the canvas. Contains
// an inner DoodleView custom View that tracks touch paths.
public class ZenDoodleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityZenDoodleBinding binding = ActivityZenDoodleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnClear.setOnClickListener(v -> binding.doodleView.clear());
    }

    public static class DoodleView extends View {
        private final android.graphics.Path path = new android.graphics.Path();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float lastX, lastY;
        private int currentColor = Color.parseColor("#2E9E8F");
        private final int[] palette = {
            Color.parseColor("#2E9E8F"), Color.parseColor("#FCBA4A"),
            Color.parseColor("#ED4545"), Color.parseColor("#4F46E5"),
            Color.parseColor("#1C7366")
        };
        private int colorIdx = 0;

        public DoodleView(Context ctx, AttributeSet attrs) {
            super(ctx, attrs);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(currentColor);
        }

        public void clear() { path.reset(); invalidate(); }

        @Override protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.parseColor("#F7FAFF"));
            paint.setColor(currentColor);
            canvas.drawPath(path, paint);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(e.getX(), e.getY());
                    lastX = e.getX(); lastY = e.getY();
                    // Cycle color on new stroke
                    colorIdx = (colorIdx + 1) % palette.length;
                    currentColor = palette[colorIdx];
                    break;
                case MotionEvent.ACTION_MOVE:
                    path.quadTo(lastX, lastY, (e.getX() + lastX) / 2, (e.getY() + lastY) / 2);
                    lastX = e.getX(); lastY = e.getY();
                    break;
            }
            invalidate();
            return true;
        }
    }
}
