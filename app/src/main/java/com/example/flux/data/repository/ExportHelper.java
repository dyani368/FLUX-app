package com.example.flux.data.repository;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import androidx.core.content.FileProvider;

import com.example.flux.data.local.Habit;
import com.example.flux.data.local.HabitLog;
import com.example.flux.data.local.SleepLog;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportHelper {

    public static File exportHabitsCSV(Context context, List<Habit> habits,
                                        List<HabitLog> logs, List<SleepLog> sleepLogs) {
        try {
            File exportDir = new File(context.getCacheDir(), "exports");
            exportDir.mkdirs();

            String fileName = "FLUX_export_" +
                new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                    .format(new Date()) + ".csv";
            File file = new File(exportDir, fileName);

            FileWriter writer = new FileWriter(file);

            // ---- HABITS SECTION ----
            writer.append("FLUX Habit Data Export\n");
            writer.append("Generated: ")
                  .append(new SimpleDateFormat("dd MMM yyyy HH:mm",
                      Locale.getDefault()).format(new Date()))
                  .append("\n\n");

            writer.append("=== HABITS ===\n");
            writer.append("Name,Category,Frequency,Difficulty,Created,Paused\n");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            for (Habit h : habits) {
                writer.append(escape(h.name)).append(",")
                      .append(escape(h.category)).append(",")
                      .append(escape(h.frequency)).append(",")
                      .append(String.valueOf(h.difficulty)).append(",")
                      .append(sdf.format(new Date(h.createdAt))).append(",")
                      .append(h.isPaused ? "Yes" : "No").append("\n");
            }

            // ---- HABIT LOGS SECTION ----
            writer.append("\n=== HABIT LOGS ===\n");
            writer.append("Habit ID,Date,Time\n");
            SimpleDateFormat logDate = new SimpleDateFormat("dd/MM/yyyy HH:mm",
                Locale.getDefault());
            for (HabitLog log : logs) {
                if (!log.undone) {
                    writer.append(String.valueOf(log.habitId)).append(",")
                          .append(logDate.format(new Date(log.timestamp))).append("\n");
                }
            }

            // ---- SLEEP SECTION ----
            writer.append("\n=== SLEEP LOGS ===\n");
            writer.append("Date,Hours,Energy Level\n");
            for (SleepLog s : sleepLogs) {
                writer.append(sdf.format(new Date(s.date))).append(",")
                      .append(String.valueOf(s.hours)).append(",")
                      .append(String.valueOf(s.energyLevel)).append("\n");
            }

            // ---- SUMMARY SECTION ----
            writer.append("\n=== SUMMARY ===\n");
            writer.append("Total Habits," + habits.size() + "\n");
            writer.append("Total Completions," + logs.stream()
                .filter(l -> !l.undone).count() + "\n");
            writer.append("Total Sleep Logs," + sleepLogs.size() + "\n");
            if (!sleepLogs.isEmpty()) {
                float avgSleep = 0;
                for (SleepLog s : sleepLogs) avgSleep += s.hours;
                avgSleep /= sleepLogs.size();
                writer.append(String.format(Locale.getDefault(),
                    "Average Sleep,%.1f hrs\n", avgSleep));
            }

            writer.flush();
            writer.close();
            return file;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public static void shareCSV(Context context, File file) {
        Uri uri = FileProvider.getUriForFile(context,
            context.getPackageName() + ".provider", file);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/csv");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.putExtra(Intent.EXTRA_SUBJECT, "FLUX Habit Export");
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(share, "Export via"));
    }
}
