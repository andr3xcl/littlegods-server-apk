package com.example.statsweb;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WaveView extends View implements SensorEventListener {

    private Paint wavePaint;
    private Paint bubblePaint;
    private Path wavePath;
    
    private float waveHeight = 60f;
    private float waveFrequency = 0.012f;
    private float shift = 0f;
    private float speed = 0.025f;

    private List<Bubble> bubbles;
    private List<Star> stars;
    private List<Cloud> clouds;
    private List<Planet> planets;
    private List<ShootingStar> shootingStars;
    private Random random = new Random();
    
    // Gravity / Tilt variables
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float tiltX = 0f;
    private float smoothedTiltX = 0f;

    private boolean isNightMode = true; // Defaulting to night for now
    private Paint bgElementPaint;

    public WaveView(Context context) {
        super(context);
        init(context);
    }

    public WaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        wavePaint = new Paint();
        wavePaint.setStyle(Paint.Style.FILL);
        wavePaint.setAntiAlias(true);
        
        bubblePaint = new Paint();
        bubblePaint.setColor(0x44FFFFFF);
        bubblePaint.setStyle(Paint.Style.FILL);
        bubblePaint.setAntiAlias(true);

        bgElementPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        wavePath = new Path();
        bubbles = new ArrayList<>();
        stars = new ArrayList<>();
        clouds = new ArrayList<>();
        planets = new ArrayList<>();
        shootingStars = new ArrayList<>();

        // Initialize Sensors
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    public void setNightMode(boolean night) {
        this.isNightMode = night;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            tiltX = -event.values[0]; 
            smoothedTiltX = smoothedTiltX + (tiltX - smoothedTiltX) * 0.1f;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (bubbles.isEmpty()) {
            for (int i = 0; i < 20; i++) {
                bubbles.add(new Bubble(w, h));
            }
        }
        if (stars.isEmpty()) {
            for (int i = 0; i < 50; i++) {
                stars.add(new Star(w, h));
            }
        }
        if (clouds.isEmpty()) {
            for (int i = 0; i < 5; i++) {
                clouds.add(new Cloud(w, h));
            }
        }
        if (planets.isEmpty()) {
            for (int i = 0; i < 2; i++) {
                planets.add(new Planet(w, h));
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int w = getWidth();
        int h = getHeight();

        // Draw background base color
        canvas.drawColor(isNightMode ? 0xFF0F0F0F : 0xFFE0F2F7);

        // Draw Background Elements (Behind Waves)
        if (isNightMode) {
            for (Star star : stars) {
                star.draw(canvas, bgElementPaint, w, h, smoothedTiltX);
            }
            for (Planet planet : planets) {
                planet.draw(canvas, bgElementPaint, w, h, smoothedTiltX);
            }
            
            // Randomly spawn shooting star
            if (random.nextInt(300) == 0 && shootingStars.size() < 2) {
                shootingStars.add(new ShootingStar(w, h));
            }
            
            for (int i = shootingStars.size() - 1; i >= 0; i--) {
                ShootingStar ss = shootingStars.get(i);
                if (ss.isDead()) {
                    shootingStars.remove(i);
                } else {
                    ss.draw(canvas, bgElementPaint);
                }
            }
        } else {
            for (Cloud cloud : clouds) {
                cloud.draw(canvas, bgElementPaint, w, h, smoothedTiltX);
            }
        }

        // Colors based on theme
        int c1s = isNightMode ? 0xFF001144 : 0xFF0055AA;
        int c1e = isNightMode ? 0xFF002266 : 0xFF0088CC;
        
        int c2s = isNightMode ? 0xAA001166 : 0xAA0077CC;
        int c2e = isNightMode ? 0xAA002288 : 0xAA00AACC;
        
        int c3s = isNightMode ? 0x88002288 : 0x880099DD;
        int c3e = isNightMode ? 0x880033AA : 0x8800BBEE;
        
        int c4s = isNightMode ? 0xCC003399 : 0xCC00AAFF;
        int c4e = isNightMode ? 0xCC0044CC : 0xCC00DDFF;

        // Layer 1: Deepest Wave
        drawWave(canvas, 0.4f, 1.5f, shift * 0.5f, 0.88f, c1s, c1e, smoothedTiltX * 12f);
        
        // Bubbles
        bubblePaint.setColor(isNightMode ? 0x33FFFFFF : 0x33000000);
        for (Bubble bubble : bubbles) {
            bubble.move(w, h);
            bubble.draw(canvas, bubblePaint);
        }

        // Layer 2
        drawWave(canvas, 0.7f, 1.2f, shift * 0.7f, 0.90f, c2s, c2e, smoothedTiltX * 18f);

        // Layer 3
        drawWave(canvas, 1.0f, 0.9f, shift * 0.9f, 0.92f, c3s, c3e, smoothedTiltX * 24f);
        
        // Layer 4
        drawWave(canvas, 1.2f, 1.0f, shift, 0.94f, c4s, c4e, smoothedTiltX * 30f);

        // Shimmer Layer
        drawWaveHighlight(canvas, 1.2f, 1.0f, shift, 0.94f, isNightMode ? 0x22FFFFFF : 0x44FFFFFF, smoothedTiltX * 30f);
        
        shift += speed;
        // Optimized 60fps update
        postOnAnimation(this::invalidate);
    }

    private void drawWave(Canvas canvas, float heightMultiplier, float freqMultiplier, float offset, float baseHeightPercent, int colorStart, int colorEnd, float tiltOffset) {
        wavePath.reset();
        float h = getHeight();
        float w = getWidth();
        float topY = h * baseHeightPercent;

        // Apply Gradient
        wavePaint.setShader(new LinearGradient(0, topY - waveHeight, 0, h, colorStart, colorEnd, Shader.TileMode.CLAMP));

        // Start drawing from way outside the left to handle tilt (overscan)
        wavePath.moveTo(-300, h + 300);
        
        for (float x = -300; x <= w + 300; x += 25) {
            float tiltFactor = (x - w/2) * (smoothedTiltX * 0.06f);
            float y = (float) (Math.sin((x * waveFrequency * freqMultiplier) + offset) * (waveHeight * heightMultiplier)) 
                    + topY + tiltFactor;
            wavePath.lineTo(x + tiltOffset, y);
        }
        
        wavePath.lineTo(w + 300, h + 300);
        wavePath.close();
        canvas.drawPath(wavePath, wavePaint);
    }

    private void drawWaveHighlight(Canvas canvas, float heightMultiplier, float freqMultiplier, float offset, float baseHeightPercent, int color, float tiltOffset) {
        wavePath.reset();
        float h = getHeight();
        float w = getWidth();
        float topY = h * baseHeightPercent;

        wavePaint.setShader(null);
        wavePaint.setColor(color);
        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeWidth(5f);

        boolean first = true;
        for (float x = -300; x <= w + 300; x += 25) {
            float tiltFactor = (x - w/2) * (smoothedTiltX * 0.06f);
            float y = (float) (Math.sin((x * waveFrequency * freqMultiplier) + offset) * (waveHeight * heightMultiplier)) 
                    + topY + tiltFactor;
            if (first) {
                wavePath.moveTo(x + tiltOffset, y);
                first = false;
            } else {
                wavePath.lineTo(x + tiltOffset, y);
            }
        }
        canvas.drawPath(wavePath, wavePaint);
        wavePaint.setStyle(Paint.Style.FILL); // reset
    }

    private class Bubble {
        float x, y;
        float radius;
        float speedY;

        Bubble(int w, int h) {
            reset(w, h);
            y = random.nextInt(h);
        }

        void reset(int w, int h) {
            x = random.nextInt(w + 200) - 100;
            y = h + 100;
            radius = random.nextFloat() * 7 + 3;
            speedY = random.nextFloat() * 1.5f + 0.8f;
        }

        void move(int w, int h) {
            y -= speedY;
            x += (float) Math.sin(y * 0.05f) * 0.4f + (smoothedTiltX * 0.1f);
            if (y < h * 0.80f) {
                reset(w, h);
            }
        }

        void draw(Canvas canvas, Paint paint) {
            canvas.drawCircle(x, y, radius, paint);
        }
    }

    private class Star {
        float x, y, size;
        int baseAlpha;
        float twinkleOffset;

        Star(int w, int h) {
            x = random.nextInt(w);
            y = random.nextInt((int)(h * 0.8f));
            size = random.nextFloat() * 3 + 1;
            baseAlpha = random.nextInt(150) + 100;
            twinkleOffset = random.nextFloat() * 6.28f;
        }

        void draw(Canvas canvas, Paint paint, int w, int h, float tilt) {
            paint.setColor(0xFFFFFFFF);
            // Twinkle effect
            float factor = (float) (Math.sin(System.currentTimeMillis() * 0.003f + twinkleOffset) + 1f) / 2f;
            int alpha = (int) (baseAlpha * (0.5f + 0.5f * factor));
            paint.setAlpha(alpha);

            float tx = x + (tilt * 10f);
            canvas.drawCircle(tx, y, size, paint);
        }
    }

    private class Planet {
        float x, y, size;
        int color;
        Planet(int w, int h) {
            x = random.nextInt(w);
            y = random.nextInt((int)(h * 0.5f));
            size = random.nextFloat() * 20 + 15;
            color = random.nextBoolean() ? 0xFF996633 : 0xFF6699CC;
        }
        void draw(Canvas canvas, Paint paint, int w, int h, float tilt) {
            float tx = x + (tilt * 20f);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawCircle(tx, y, size, paint);
            
            // Draw Ring
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0x88FFFFFF);
            paint.setStrokeWidth(4f);
            canvas.drawOval(tx - size*2, y - size*0.5f, tx + size*2, y + size*0.5f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private class Cloud {
        float x, y, w, h;
        float speed;
        Cloud(int viewW, int viewH) {
            reset(viewW, viewH, true);
        }
        void reset(int viewW, int viewH, boolean randomX) {
            x = randomX ? random.nextInt(viewW) : -300;
            y = random.nextInt((int)(viewH * 0.4f));
            w = random.nextInt(200) + 150;
            h = w * 0.5f;
            speed = random.nextFloat() * 0.5f + 0.2f;
        }
        void draw(Canvas canvas, Paint paint, int viewW, int viewH, float tilt) {
            x += speed + (tilt * 0.05f);
            if (x > viewW + 300) reset(viewW, viewH, false);
            
            paint.setColor(0x88FFFFFF); // Increased alpha for visibility
            float tx = x + (tilt * 15f);
            canvas.drawRoundRect(tx, y, tx + w, y + h, h/2, h/2, paint);
        }
    }

    private class ShootingStar {
        float x, y, speedX, speedY;
        float length;
        int alpha = 255;

        ShootingStar(int w, int h) {
            x = random.nextInt(w);
            y = random.nextInt(h / 3);
            speedX = 15f + random.nextFloat() * 10f;
            speedY = 8f + random.nextFloat() * 5f;
            length = 100f + random.nextFloat() * 100f;
        }

        void draw(Canvas canvas, Paint paint) {
            x += speedX;
            y += speedY;
            alpha -= 5;
            if (alpha < 0) alpha = 0;

            paint.setStrokeWidth(4f);
            paint.setAlpha(alpha);
            paint.setColor(0xFFFFFFFF);
            canvas.drawLine(x, y, x - speedX * 2, y - speedY * 2, paint);
        }

        boolean isDead() {
            return alpha <= 0 || x > getWidth() + 200 || y > getHeight() + 200;
        }
    }
}
