package com.example.statsweb;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

public class DayNightToggle extends View {

    private Paint bgPaint;
    private Paint circlePaint;
    private Paint craterPaint;
    
    private boolean isNight = false;
    private float animationProgress = 0f; // 0 for Day, 1 for Night
    private ValueAnimator animator;
    
    private OnThemeChangeListener listener;

    public interface OnThemeChangeListener {
        void onThemeChanged(boolean isNight);
    }

    public DayNightToggle(Context context) {
        super(context);
        init();
    }

    public DayNightToggle(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DayNightToggle(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        craterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        setOnClickListener(v -> toggle());
    }

    public void setOnThemeChangeListener(OnThemeChangeListener listener) {
        this.listener = listener;
    }

    public void setNight(boolean night, boolean animate) {
        if (this.isNight == night) return;
        this.isNight = night;
        
        float end = isNight ? 1f : 0f;
        
        if (animate) {
            if (animator != null && animator.isRunning()) animator.cancel();
            float start = animationProgress;
            animator = ValueAnimator.ofFloat(start, end);
            animator.setDuration(500);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                animationProgress = (float) animation.getAnimatedValue();
                invalidate();
            });
            animator.start();
        } else {
            animationProgress = end;
            invalidate();
        }
    }

    private void toggle() {
        if (animator != null && animator.isRunning()) return;
        
        float start = isNight ? 1f : 0f;
        float end = isNight ? 0f : 1f;
        
        isNight = !isNight;
        
        animator = ValueAnimator.ofFloat(start, end);
        animator.setDuration(500);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
        
        if (listener != null) {
            listener.onThemeChanged(isNight);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(w, w / 2); // 2:1 ratio
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        float w = getWidth();
        float h = getHeight();
        float r = h * 0.8f / 2f;
        
        // Colors
        ArgbEvaluator evaluator = new ArgbEvaluator();
        int bgColor = (int) evaluator.evaluate(animationProgress, 0xFF87CEEB, 0xFF191970);
        int circleColor = (int) evaluator.evaluate(animationProgress, 0xFFFFD700, 0xFFDDDDDD);
        
        // Draw Capsule Background
        bgPaint.setColor(bgColor);
        canvas.drawRoundRect(0, 0, w, h, h/2, h/2, bgPaint);
        
        // Draw Sun/Moon Circle
        circlePaint.setColor(circleColor);
        float centerX = h/2 + (w - h) * animationProgress;
        float centerY = h/2;
        canvas.drawCircle(centerX, centerY, r, circlePaint);
        
        // Draw Moon Craters (only if night)
        if (animationProgress > 0.5f) {
            float craterAlpha = (animationProgress - 0.5f) * 2f;
            craterPaint.setColor(0xFFBBBBBB);
            craterPaint.setAlpha((int) (craterAlpha * 255));
            
            canvas.drawCircle(centerX - r*0.3f, centerY - r*0.2f, r*0.15f, craterPaint);
            canvas.drawCircle(centerX + r*0.2f, centerY + r*0.3f, r*0.1f, craterPaint);
            canvas.drawCircle(centerX + r*0.1f, centerY - r*0.4f, r*0.08f, craterPaint);
        }
    }
}
