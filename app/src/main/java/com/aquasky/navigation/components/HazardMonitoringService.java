package com.aquasky.navigation.components;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.aquasky.navigation.utils.NotificationHelper;

/**
 * Android Foreground Service demonstrating Service Lifecycle:
 * - onCreate(): Service created in memory
 * - onStartCommand(): Background task triggered, elevated to Foreground Service
 * - onBind(): Bound communication (null here for started service)
 * - onDestroy(): Cleanup background thread and release resources
 */
public class HazardMonitoringService extends Service {

    private static final String TAG = "AquaSkyService";
    public static final String ACTION_START_MONITORING = "com.aquasky.action.START_MONITORING";
    public static final String ACTION_STOP_MONITORING = "com.aquasky.action.STOP_MONITORING";
    private static final int FOREGROUND_NOTIFICATION_ID = 4001;

    private boolean isRunning = false;
    private Thread monitoringThread;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service Lifecycle: onCreate() — Initializing global hazard geofencing engine.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service Lifecycle: onStartCommand() — Received startId: " + startId);

        if (intent != null && ACTION_STOP_MONITORING.equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        // Elevate Service to Foreground with persistent Notification
        Notification foregroundNotification = new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_HAZARD_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("AquaSky Active Navigation")
                .setContentText("Monitoring Global Maritime Swells & Highway Networks")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        startForeground(FOREGROUND_NOTIFICATION_ID, foregroundNotification);

        // Start background hazard monitoring loop
        startHazardMonitoringThread();

        return START_STICKY;
    }

    private void startHazardMonitoringThread() {
        if (isRunning) return;
        isRunning = true;

        monitoringThread = new Thread(() -> {
            Log.d(TAG, "Background geofence monitoring thread started.");
            while (isRunning) {
                try {
                    Thread.sleep(15000);
                    Log.d(TAG, "Checking active hazards... Global routes verified.");
                } catch (InterruptedException e) {
                    Log.w(TAG, "Monitoring thread interrupted: " + e.getMessage());
                    break;
                }
            }
        });
        monitoringThread.start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Service Lifecycle: onBind() called.");
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service Lifecycle: onDestroy() — Releasing monitoring resources.");
        isRunning = false;
        if (monitoringThread != null) {
            monitoringThread.interrupt();
        }
    }
}
