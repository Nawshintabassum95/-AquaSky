package com.aquasky.navigation;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.aquasky.navigation.utils.NotificationHelper;

/**
 * AquaSky Application Base
 * Initializes application context and notification channels.
 */
public class AquaSkyApplication extends Application {

    private static final String TAG = "AquaSkyApp";
    private static AquaSkyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.i(TAG, "AquaSky Global Navigation initialized.");

        // Initialize High-Priority Notification Channels for Hazards
        NotificationHelper.createNotificationChannels(this);
    }

    public static AquaSkyApplication getInstance() {
        return instance;
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
}
