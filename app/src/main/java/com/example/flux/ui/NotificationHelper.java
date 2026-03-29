package com.example.flux.ui;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.example.flux.MainActivity;
import com.example.flux.R;

import java.util.Calendar;

public class NotificationHelper {

    private static final String CHANNEL_ID = "flux_reminders";
    private static final int NOTIF_ID = 1001;

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "FLUX Reminders",
                NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Daily habit reminders");
            context.getSystemService(NotificationManager.class)
                   .createNotificationChannel(channel);
        }
    }

    // schedule a daily smart reminder at a specific hour
    public static void scheduleDailyReminder(Context context, int hour, int minute) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("annoying", false);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        if (cal.getTimeInMillis() < System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (alarm != null) {
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pi);
        }
    }

    // annoying mode: every 30 mins from 8am to 10pm
    public static void scheduleAnnoyingReminders(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("annoying", true);
        PendingIntent pi = PendingIntent.getBroadcast(context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarm != null) {
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
                30 * 60 * 1000L, pi); // every 30 mins
        }
    }

    public static void cancelReminders(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarm != null) {
            alarm.cancel(pi);
        }
    }

    public static class ReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean annoying = intent.getBooleanExtra("annoying", false);
            String message = annoying
                ? "⏰ Your habits are waiting. Don't break the chain."
                : "Time to check in on your habits today.";

            Intent openApp = new Intent(context, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(context, 0, openApp,
                PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_today)
                    .setContentTitle("FLUX")
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setContentIntent(pi);

            if (annoying) builder.setPriority(NotificationCompat.PRIORITY_HIGH);

            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.notify(NOTIF_ID, builder.build());
            }
        }
    }
}
