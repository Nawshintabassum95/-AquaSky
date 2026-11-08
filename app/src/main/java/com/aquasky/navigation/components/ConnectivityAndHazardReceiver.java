package com.aquasky.navigation.components;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;
import android.provider.Settings;

import com.aquasky.navigation.utils.NotificationHelper;

public class ConnectivityAndHazardReceiver extends BroadcastReceiver {

    private static final String TAG = "AquaSkyReceiver";
    public static final String ACTION_HAZARD_ALERT = "com.aquasky.action.SEVERE_HAZARD_ALERT";

    public interface OnConnectivityChangeListener {
        void onNetworkChanged(boolean isOnline);
    }

    private static OnConnectivityChangeListener connectivityListener;

    public static void setConnectivityListener(OnConnectivityChangeListener listener) {
        connectivityListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "Broadcast received: " + action);

        if (action == null) return;


        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm != null ? cm.getActiveNetworkInfo() : null;
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            if (connectivityListener != null) {
                connectivityListener.onNetworkChanged(isConnected);
            }


        } else if (Intent.ACTION_BATTERY_LOW.equals(action)) {
            NotificationHelper.showHeadsUpHazardNotification(context,
                    "⚠️ Low Battery Warning",
                    "Battery is low! AquaSky has reduced GPS refresh rate and dimmed map telemetry to conserve battery.");
            Toast.makeText(context, "🔋 Low Battery: Power Saving Navigation Active", Toast.LENGTH_LONG).show();


        } else if (Intent.ACTION_BATTERY_OKAY.equals(action)) {
            Toast.makeText(context, "⚡ Battery Level Restored to Normal", Toast.LENGTH_SHORT).show();


        } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
            boolean isAirplaneModeOn = intent.getBooleanExtra("state", false);
            if (isAirplaneModeOn) {
                Toast.makeText(context, "✈️ Airplane Mode ON: Switched to Offline Marine/River Charts", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "🌐 Airplane Mode OFF: Live Satellite & Weather Sync Restored", Toast.LENGTH_SHORT).show();
            }


        } else if (ACTION_HAZARD_ALERT.equals(action)) {
            String title = intent.getStringExtra("hazard_title");
            String desc = intent.getStringExtra("hazard_desc");
            if (title == null) title = "Severe Navigation Hazard Detected";
            if (desc == null) desc = "A severe meteorological warning is active.";

            NotificationHelper.showHeadsUpHazardNotification(context, title, desc);
            Toast.makeText(context, "⚠️ " + title, Toast.LENGTH_LONG).show();
        } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
            boolean on = intent.getBooleanExtra("state", false);
            String title = on ? "Airplane Mode ON" : "Airplane Mode OFF";
            String desc = on
                    ? "Live weather and hazard updates paused. Showing cached routes."
                    : "Connection restored. Live updates resuming.";
            Log.i(TAG, title);
            Toast.makeText(context, "✈️ " + title, Toast.LENGTH_LONG).show();
            NotificationHelper.showHeadsUpHazardNotification(context, title, desc);
            if (connectivityListener != null) {
                connectivityListener.onNetworkChanged(!on);   // MainActivity er offline banner update hobe
            }

        } else if (Intent.ACTION_BATTERY_LOW.equals(action)) {
            Log.w(TAG, "Battery low");
            Toast.makeText(context, "🔋 Battery low! Power saving mode on.", Toast.LENGTH_LONG).show();
            NotificationHelper.showHeadsUpHazardNotification(context,
                    "Battery Low",
                    "Reduced map refresh and live monitoring to save power.");

        } else if (Intent.ACTION_BATTERY_OKAY.equals(action)) {
            Toast.makeText(context, "🔋 Battery OK. Normal mode restored.", Toast.LENGTH_SHORT).show();
        }

    }
}