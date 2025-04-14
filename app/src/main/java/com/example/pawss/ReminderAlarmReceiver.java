package com.example.pawss;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class ReminderAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String description = intent.getStringExtra("description");
        int reminderId = intent.getIntExtra("reminderId", -1);
        int assignedToId = intent.getIntExtra("assignedToId", -1);

        AuthManager authManager = new AuthManager(context);

        // Verificar si la notificación es para este usuario
        if (assignedToId != -1 && assignedToId != authManager.getUserId()) {
            return; // No es para este usuario
        }

        // Mostrar notificación
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "reminder_channel",
                    "Recordatorios",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(context, recordatorios.class);
        notificationIntent.putExtra("reminderId", reminderId);
        notificationIntent.setAction("COMPLETE_REMINDER");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                reminderId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "reminder_channel")
                .setSmallIcon(R.drawable.logomiau)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(reminderId, builder.build());
    }
}