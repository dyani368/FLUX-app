package com.example.flux.ui.dashboard;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleView extends View {

    private final List<Particle> particles = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private ValueAnimator animator;

    private static final int[] COLORS = {
            0xFF7C4DFF, 0xFF4CAF50, 0xFFFFB300, 0xFFE91E63, 0xFF00BCD4
    };

    public ParticleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void burst(float cx, float cy) {
        particles.clear();
        for (int i = 0; i < 20; i++) {
            Particle p = new Particle();
            p.x = cx;
            p.y = cy;
            p.vx = (random.nextFloat() - 0.5f) * 20;
            p.vy = (random.nextFloat() - 1f) * 20;
            p.radius = 6 + random.nextFloat() * 8;
            p.color = COLORS[random.nextInt(COLORS.length)];
            p.alpha = 255;
            particles.add(p);
        }

        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(800);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            for (Particle p : particles) {
                p.x += p.vx;
                p.y += p.vy;
                p.vy += 0.8f; // gravity
                p.alpha = (int)(255 * (1 - t));
            }
            invalidate();
        });
        animator.start();
        setVisibility(VISIBLE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (Particle p : particles) {
            paint.setColor(p.color);
            paint.setAlpha(p.alpha);
            canvas.drawCircle(p.x, p.y, p.radius, paint);
        }
    }

    static class Particle {
        float x, y, vx, vy, radius;
        int color, alpha;
    }
}