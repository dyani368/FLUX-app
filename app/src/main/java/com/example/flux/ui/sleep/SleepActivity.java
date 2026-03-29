package com.example.flux.ui.sleep;

import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        currentTheme = ThemeManager.getTheme(this);
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep);

        HabitViewModel viewModel = new ViewModelProvider(this).get(HabitViewModel.class);

        SeekBar seekEnergy = findViewById(R.id.seekEnergy);
        TextView tvEnergyLabel = findViewById(R.id.tvEnergyLabel);

        seekEnergy.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                tvEnergyLabel.setText("Energy: " + (p + 1));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        findViewById(R.id.btnSaveSleep).setOnClickListener(v -> {
            String hoursStr = ((android.widget.EditText)
                    findViewById(R.id.etHours)).getText().toString().trim();
            if (hoursStr.isEmpty()) {
                Toast.makeText(this, "Enter hours slept", Toast.LENGTH_SHORT).show();
                return;
            }

            SleepLog log = new SleepLog();
            log.hours = Float.parseFloat(hoursStr);
            log.energyLevel = seekEnergy.getProgress() + 1;
            log.date = System.currentTimeMillis();
            viewModel.insertSleepLog(log);

            Toast.makeText(this, "Sleep logged!", Toast.LENGTH_SHORT).show();
            finish();
        });

        // load last 7 days sleep bars
        viewModel.getLastSevenDays().observe(this, sleepLogs -> {
            LinearLayout llHistory = findViewById(R.id.llSleepHistory);
            llHistory.removeAllViews();

            // motivational tip based on avg sleep
            TextView tvTip = findViewById(R.id.tvSleepTip);
            if (sleepLogs != null && !sleepLogs.isEmpty()) {
                float avg = 0;
                for (SleepLog s : sleepLogs) avg += s.hours;
                avg /= sleepLogs.size();

                if (avg >= 7.5f)
                    tvTip.setText("🌟 Great sleep average! Your habits will reflect this.");
                else if (avg >= 6f)
                    tvTip.setText("💡 You're close — even 30 more minutes of sleep boosts habit completion by 20%.");
                else
                    tvTip.setText("⚠️ Low sleep detected. Consider an earlier bedtime tonight.");

                // draw mini bars for last 7 days
                for (int i = Math.min(sleepLogs.size(), 7) - 1; i >= 0; i--) {
                    SleepLog log = sleepLogs.get(i);
                    LinearLayout barContainer = new LinearLayout(this);
                    barContainer.setOrientation(LinearLayout.VERTICAL);
                    barContainer.setGravity(android.view.Gravity.CENTER);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    params.setMargins(4, 0, 4, 0);
                    barContainer.setLayoutParams(params);

                    // bar
                    android.view.View bar = new android.view.View(this);
                    int barHeight = (int)(log.hours / 10f * 60); // max 10hrs = 60dp
                    barHeight = Math.max(barHeight, 8);
                    LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                        20, (int)(barHeight * getResources().getDisplayMetrics().density));
                    bar.setLayoutParams(barParams);
                    bar.setBackgroundColor(log.hours >= 7 ? 0xFFB8A2D1 : 0xFF555555);

                    // hour label
                    TextView label = new TextView(this);
                    label.setText(String.format(Locale.getDefault(), "%.0fh", log.hours));
                    label.setTextSize(9);
                    label.setTextColor(0xFF888888);
                    label.setGravity(android.view.Gravity.CENTER);

                    barContainer.addView(bar);
                    barContainer.addView(label);
                    llHistory.addView(barContainer);
                }
            }
        });
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
