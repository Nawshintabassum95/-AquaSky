package com.aquasky.navigation.components;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates the use of Android ContentResolver to query data from AquaSkyHazardContentProvider.
 */
public class ContentResolverUsageHelper {

    private static final String TAG = "ContentResolverHelper";

    public static List<String> queryActiveHazardsViaResolver(Context context) {
        List<String> results = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();

        try (Cursor cursor = resolver.query(
                AquaSkyHazardContentProvider.CONTENT_URI,
                new String[]{AquaSkyHazardContentProvider.COLUMN_TITLE, AquaSkyHazardContentProvider.COLUMN_SEVERITY},
                null,
                null,
                null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int titleIndex = cursor.getColumnIndexOrThrow(AquaSkyHazardContentProvider.COLUMN_TITLE);
                int severityIndex = cursor.getColumnIndexOrThrow(AquaSkyHazardContentProvider.COLUMN_SEVERITY);

                do {
                    String title = cursor.getString(titleIndex);
                    String severity = cursor.getString(severityIndex);
                    results.add("[" + severity + "] " + title);
                    Log.d(TAG, "ContentResolver retrieved: " + title + " (" + severity + ")");
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying ContentResolver: " + e.getMessage());
        }

        return results;
    }
}
