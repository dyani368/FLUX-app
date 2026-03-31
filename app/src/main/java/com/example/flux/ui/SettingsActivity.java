package com.example.flux.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.flux.MainActivity;
import com.example.flux.R;
import com.example.flux.data.repository.ExportHelper;
import com.example.flux.data.repository.HabitRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("flux_prefs", MODE_PRIVATE);

        setupThemeSelector();
        setupToggles();
        setupExport();
        setupSignOut();
    }

    private void setupThemeSelector() {
        RadioGroup group = findViewById(R.id.themeGroup);
        String current = ThemeManager.getTheme(this);

        if (current.equals(ThemeManager.WARM_NEUTRAL)) {
            group.check(R.id.themeWarmNeutral);
        } else if (current.equals(ThemeManager.COOL_CALM)) {
            group.check(R.id.themeCoolCalm);
        } else if (current.equals(ThemeManager.FOREST)) {
            group.check(R.id.themeForest);
        } else {
            group.check(R.id.themeDarkMinimal);
        }

        group.setOnCheckedChangeListener((g, id) -> {
            String selected;
            if (id == R.id.themeWarmNeutral)     selected = ThemeManager.WARM_NEUTRAL;
            else if (id == R.id.themeCoolCalm)   selected = ThemeManager.COOL_CALM;
            else if (id == R.id.themeForest)     selected = ThemeManager.FOREST;
            else                                  selected = ThemeManager.DARK_MINIMAL;

            ThemeManager.saveTheme(this, selected);
            // restart to apply
            recreate();
        });
    }

    private void setupToggles() {
        Switch switchAnim = findViewById(R.id.switchAnimations);
        Switch switchMomentum = findViewById(R.id.switchMomentum);
        Switch switchAnnoying = findViewById(R.id.switchAnnoyingMode);
        Switch switchReminders = findViewById(R.id.switchReminders);
        Switch switchMinimal = findViewById(R.id.switchMinimalMode);

        switchAnim.setChecked(prefs.getBoolean("animations_on", true));
        switchMomentum.setChecked(prefs.getBoolean("momentum_on", true));
        switchAnnoying.setChecked(prefs.getBoolean("annoying_mode", false));
        switchReminders.setChecked(prefs.getBoolean("reminders_on", true));
        switchMinimal.setChecked(prefs.getBoolean("minimal_mode", false));

        switchAnim.setOnCheckedChangeListener((b, v) ->
            prefs.edit().putBoolean("animations_on", v).apply());
        switchMomentum.setOnCheckedChangeListener((b, v) ->
            prefs.edit().putBoolean("momentum_on", v).apply());
        switchAnnoying.setOnCheckedChangeListener((b, v) -> {
            prefs.edit().putBoolean("annoying_mode", v).apply();
        });
        switchReminders.setOnCheckedChangeListener((b, v) ->
            prefs.edit().putBoolean("reminders_on", v).apply());

        switchMinimal.setOnCheckedChangeListener((b, v) -> {
            prefs.edit().putBoolean("minimal_mode", v).apply();
            // restart main activity to apply
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }

    private void setupExport() {
        HabitRepository repo = new HabitRepository(getApplication());
        findViewById(R.id.btnExportRow).setOnClickListener(v -> {
            Toast.makeText(this, "Preparing export...", Toast.LENGTH_SHORT).show();
            repo.exportData((habits, logs, sleep) -> {
                File csv = ExportHelper.exportHabitsCSV(this, habits, logs, sleep);
                if (csv != null) {
                    ExportHelper.shareCSV(this, csv);
                } else {
                    Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setupSignOut() {
        findViewById(R.id.btnSignOutRow).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            prefs.edit().putBoolean("onboarding_done", false).apply();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });
    }
}
