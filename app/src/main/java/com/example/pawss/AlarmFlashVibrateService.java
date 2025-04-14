package com.example.pawss;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent; // Importación añadida
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.os.Handler;
import android.util.Log;

public class AlarmFlashVibrateService extends Service {

    private Handler handler = new Handler(Looper.getMainLooper());
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashOn = false;
    private Runnable flashRunnable;
    private Vibrator vibrator;
    private int reminderId;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, buildBasicNotification());


        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        vibrator = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                ? ((VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator()
                : (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        try {
            String[] ids = cameraManager.getCameraIdList();
            if (ids.length > 0) {
                cameraId = ids[0];
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (cameraId != null && cameraManager != null) {
            try {
                startFlashLoop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.d("AlarmService", "onCreate iniciado");
        Log.d("AlarmService", "cameraId: " + cameraId);

        startVibration();
        Log.e("AlarmService", "Error usando el flash");

    }

    // Método para notificación básica (usado en onCreate)
    private Notification buildBasicNotification() {
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(
                    "alarm_channel",
                    "Recordatorio activo",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, "alarm_channel")
                .setContentTitle("Actividad pendiente")
                .setContentText("Sube una foto para completar el recordatorio")
                .setSmallIcon(R.drawable.pawprint)
                .setOngoing(true)
                .build();
    }

    // Método para notificación con detalles (usado en onStartCommand)
    private Notification buildDetailedNotification(String title, String description) {
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(
                    "alarm_channel",
                    "Recordatorio activo",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        // Intent para abrir la app al tocar la notificación
        Intent intent = new Intent(this, recordatorios.class);
        intent.putExtra("reminderId", reminderId);
        intent.setAction("COMPLETE_REMINDER");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                reminderId,
                intent,
                flags
        );

        return new NotificationCompat.Builder(this, "alarm_channel")
                .setContentTitle(title != null ? title : "Actividad pendiente")
                .setContentText(description != null ? description : "Sube una foto para completar el recordatorio")
                .setSmallIcon(R.drawable.pawprint)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.logomiau, "Completar", pendingIntent)
                .build();
    }

    private void startFlashLoop() {
        flashRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    isFlashOn = !isFlashOn;
                    if (cameraId != null) {
                        cameraManager.setTorchMode(cameraId, isFlashOn);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    stopSelf();
                }
                handler.postDelayed(this, 400);
            }
        };
        handler.post(flashRunnable);
    }


    private void startVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            long[] pattern = {0, 1000, 1000};
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)); // repetir
        } else {
            vibrator.vibrate(new long[]{0, 1000, 1000}, 0);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            reminderId = intent.getIntExtra("reminderId", -1);
            String title = intent.getStringExtra("title");
            String description = intent.getStringExtra("description");

            // Crear notificación persistente con detalles
            Notification notification = buildDetailedNotification(title, description);
            startForeground(reminderId, notification);
        }

        startFlashLoop();
        startVibration();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(flashRunnable);
        try {
            cameraManager.setTorchMode(cameraId, false);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        vibrator.cancel();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void stopService() {
        handler.removeCallbacks(flashRunnable);
        try {
            cameraManager.setTorchMode(cameraId, false);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        vibrator.cancel();
        stopSelf();
    }
}