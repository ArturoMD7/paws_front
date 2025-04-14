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

        // Iniciar servicio de notificación persistente
        Intent serviceIntent = new Intent(context, AlarmFlashVibrateService.class);
        serviceIntent.putExtra("reminderId", reminderId);
        serviceIntent.putExtra("title", title);
        serviceIntent.putExtra("description", description);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        // Mostrar notificación
        showNotification(context, title, description, reminderId);
    }

    private void showNotification(Context context, String title, String content, int reminderId) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "reminder_channel",
                    "Recordatorios",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        // Intent para abrir la app al tocar la notificación
        Intent activityIntent = new Intent(context, recordatorios.class);
        activityIntent.putExtra("reminderId", reminderId);
        activityIntent.setAction("COMPLETE_REMINDER");
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                reminderId,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );


        // Intent específico para el botón "Completar"
        Intent completeIntent = new Intent(context, recordatorios.class);
        completeIntent.putExtra("reminderId", reminderId);
        completeIntent.setAction("COMPLETE_REMINDER");
        completeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent completePendingIntent = PendingIntent.getActivity(
                context,
                reminderId + 1000, // Diferente ID para evitar colisión
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Crear la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "reminder_channel")
                .setSmallIcon(R.drawable.logomiau)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent) // Acción al tocar la notificación
                .setAutoCancel(true)
                .addAction(R.drawable.pawprint, "Completar", completePendingIntent); // Acción del botón

        manager.notify(reminderId, builder.build());
    }
}