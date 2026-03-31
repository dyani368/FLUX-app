package com.example.flux.ui.sleep;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.flux.R;
import com.example.flux.data.local.SleepLog;
import com.example.flux.ui.ThemeManager;
import com.example.flux.ui.dashboard.HabitViewModel;

import java.util.Locale;

public class SleepActivity extends AppCompatActivity {

    private String currentTheme;
    private float currentHours = 7.5f;
    private int selectedEnergy = 3;

    // Energy button views
    private LinearLayout[] energyButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        currentTheme = ThemeManager.getTheme(this);
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep);

        HabitViewModel viewModel = new ViewModelProvider(this).get(HabitViewModel.class);

        // --- Hours picker ---
        TextView tvHoursDisplay = findViewById(R.id.tvHoursDisplay);
        android.widget.EditText etHours = findViewById(R.id.etHours);

        // sync initial display and hidden EditText
        tvHoursDisplay.setText(formatHours(currentHours));
        if (etHours != null) etHours.setText(String.valueOf(currentHours));

        findViewById(R.id.btnHoursMinus).setOnClickListener(v -> {
            if (currentHours > 0.5f) {
                currentHours -= 0.5f;
                tvHoursDisplay.setText(formatHours(currentHours));
                if (etHours != null) etHours.setText(String.valueOf(currentHours));
            }
        });

        findViewById(R.id.btnHoursPlus).setOnClickListener(v -> {
            if (currentHours < 14f) {
                currentHours += 0.5f;
                tvHoursDisplay.setText(formatHours(currentHours));
                if (etHours != null) etHours.setText(String.valueOf(currentHours));
            }
        });

        // --- Energy emoji buttons ---
        energyButtons = new LinearLayout[]{
            findViewById(R.id.btnEnergy1),
            findViewById(R.id.btnEnergy2),
            findViewById(R.id.btnEnergy3),
            findViewById(R.id.btnEnergy4),
            findViewById(R.id.btnEnergy5)
        };

        // keep seekbar in sync (hidden but used by save logic in original code)
        SeekBar seekEnergy = findViewById(R.id.seekEnergy);

        for (int i = 0; i < energyButtons.length; i++) {
            final int level = i + 1;
            energyButtons[i].setOnClickListener(v -> {
                selectedEnergy = level;
                if (seekEnergy != null) seekEnergy.setProgress(level - 1);
                updateEnergySelection();
            });
        }

        // default highlight energy 3
        updateEnergySelection();

        // --- Save ---
        findViewById(R.id.btnSaveSleep).setOnClickListener(v -> {
            SleepLog log = new SleepLog();
            log.hours = currentHours;
            log.energyLevel = selectedEnergy;
            log.date = System.currentTimeMillis();
            viewModel.insertSleepLog(log);

            Toast.makeText(this, "Sleep logged! 🌙", Toast.LENGTH_SHORT).show();
            finish();
        });

        // --- Load last 7 nights ---
        viewModel.getLastSevenDays().observe(this, sleepLogs -> {
            LinearLayout llHistory = findViewById(R.id.llSleepHistory);
            if (llHistory == null) return;
            llHistory.removeAllViews();

            TextView tvTip = findViewById(R.id.tvSleepTip);
            TextView tvAvg = findViewById(R.id.tvAvgSleep);

            if (sleepLogs != null && !sleepLogs.isEmpty()) {
                float avg = 0;
                for (SleepLog s : sleepLogs) avg += s.hours;
                avg /= sleepLogs.size();

                if (tvAvg != null) {
                    tvAvg.setText(String.format(Locale.getDefault(), "avg %.1fh", avg));
                }

                if (tvTip != null) {
                    if (avg >= 7.5f)
                        tvTip.setText("Great sleep average! Your habits will reflect this energy.");
                    else if (avg >= 6f)
                        tvTip.setText("You're close — even 30 more minutes boosts habit completion by 20%.");
                    else
                        tvTip.setText("Low sleep detected. An earlier bedtime tonight will make tomorrow easier.");
                }

                float maxHours = 10f;
                int maxBarHeightDp = 64;

                for (int i = Math.min(sleepLogs.size(), 7) - 1; i >= 0; i--) {
                    SleepLog log = sleepLogs.get(i);

                    LinearLayout barContainer = new LinearLayout(this);
                    barContainer.setOrientation(LinearLayout.VERTICAL);
                    barContainer.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                    params.setMargins(4, 0, 4, 0);
                    barContainer.setLayoutParams(params);

                    // colored bar
                    View bar = new View(this);
                    float ratio = Math.min(log.hours / maxHours, 1f);
                    int barHeightDp = Math.max((int)(ratio * maxBarHeightDp), 6);
                    int barHeightPx = (int)(barHeightDp * getResources().getDisplayMetrics().density);

                    LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                        (int)(14 * getResources().getDisplayMetrics().density), barHeightPx);
                    barParams.setMargins(0, 0, 0, 4);
                    bar.setLayoutParams(barParams);

                    // color by quality
                    int barColor;
                    if (log.hours >= 7f) {
                        barColor = 0xFF4CAF50;   // green — good
                    } else if (log.hours >= 5f) {
                        barColor = 0xFFFF9800;   // amber — okay
                    } else {
                        barColor = 0xFFF44336;   // red — low
                    }

                    // rounded bar using GradientDrawable
                    android.graphics.drawable.GradientDrawable barDrawable =
                        new android.graphics.drawable.GradientDrawable();
                    barDrawable.setColor(barColor);
                    barDrawable.setCornerRadius(8 * getResources().getDisplayMetrics().density);
                    bar.setBackground(barDrawable);

                    // hour label below bar
                    TextView label = new TextView(this);
                    label.setText(String.format(Locale.getDefault(), "%.0fh", log.hours));
                    label.setTextSize(9);
                    label.setTextColor(0xFF888888);
                    label.setGravity(android.view.Gravity.CENTER);

                    barContainer.addView(bar);
                    barContainer.addView(label);
                    llHistory.addView(barContainer);
                }
            } else {
                if (tvTip != null) tvTip.setText("Log your first sleep to start tracking your patterns.");
                if (tvAvg != null) tvAvg.setText("");
            }
        });
    }

    private String formatHours(float hours) {
        if (hours == (int) hours) {
            return String.valueOf((int) hours);
        }
        return String.format(Locale.getDefault(), "%.1f", hours);
    }

    private void updateEnergySelection() {
        if (energyButtons == null) return;
        for (int i = 0; i < energyButtons.length; i++) {
            if (energyButtons[i] == null) continue;
            boolean selected = (i + 1) == selectedEnergy;
            energyButtons[i].setAlpha(selected ? 1.0f : 0.4f);

            // scale up selected button slightly
            energyButtons[i].animate()
                .scaleX(selected ? 1.1f : 1.0f)
                .scaleY(selected ? 1.1f : 1.0f)
                .setDuration(150)
                .start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String theme = ThemeManager.getTheme(this);
        if (!theme.equals(currentTheme)) {
            recreate();
        }
    }
}
