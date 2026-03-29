package com.example.flux.ui.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class HeatmapView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Map<Integer, Integer> completionMap = new HashMap<>(); // day -> count
    private Map<Integer, Float> sleepMap = new HashMap<>();        // day -> hours
    private int daysInMonth = 30;
    private int cellSize = 0;
    private int gap = 6;

    public HeatmapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        textPaint.setColor(Color.parseColor("#888888"));
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(Map<Integer, Integer> completions,
                        Map<Integer, Float> sleep, int daysInMonth) {
        this.completionMap = completions;
        this.sleepMap = sleep;
        this.daysInMonth = daysInMonth;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int cols = 7;
        int rows = (int) Math.ceil(daysInMonth / 7.0);
        cellSize = (getWidth() - gap * (cols + 1)) / cols;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;

        for (int day = 1; day <= daysInMonth; day++) {
            int index = day - 1 + startDayOfWeek;
            int col = index % cols;
            int row = index / cols;

            float left = gap + col * (cellSize + gap);
            float top = gap + row * (cellSize + gap);
            RectF rect = new RectF(left, top, left + cellSize, top + cellSize);

            int count = completionMap.getOrDefault(day, 0);
            float sleepHours = sleepMap.getOrDefault(day, 8f);

            // base color intensity from habit completion
            int baseAlpha = Math.min(255, 60 + count * 60);
            // dim if low sleep (< 6 hours)
            float sleepAlpha = sleepHours < 6f ? 0.5f : 1f;
            int finalAlpha = (int)(baseAlpha * sleepAlpha);

            if (count == 0) {
                paint.setColor(Color.parseColor("#1A1A1A"));
            } else {
                paint.setColor(Color.argb(finalAlpha, 124, 77, 255));
            }

            canvas.drawRoundRect(rect, 8, 8, paint);

            // day number
            float textX = left + cellSize / 2f;
            float textY = top + cellSize / 2f + 9f;
            canvas.drawText(String.valueOf(day), textX, textY, textPaint);
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        int cols = 7;
        int rows = (int) Math.ceil(daysInMonth / 7.0);
        int width = MeasureSpec.getSize(widthSpec);
        int cs = (width - gap * (cols + 1)) / cols;
        int height = rows * (cs + gap) + gap;
        setMeasuredDimension(width, height);
    }
}