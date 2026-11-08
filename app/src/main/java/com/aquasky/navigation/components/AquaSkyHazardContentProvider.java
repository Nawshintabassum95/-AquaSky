package com.aquasky.navigation.components;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Android ContentProvider demonstrating:
 * - ContentProvider Lifecycle: onCreate()
 * - CRUD operations: query(), insert(), update(), delete(), getType()
 * - UriMatcher matching patterns for hazards collection and individual items
 */
public class AquaSkyHazardContentProvider extends ContentProvider {

    private static final String TAG = "HazardProvider";
    public static final String AUTHORITY = "com.aquasky.navigation.provider";
    public static final String TABLE_HAZARDS = "hazards";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_HAZARDS);

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_SEVERITY = "severity";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";

    private static final int CODE_HAZARDS = 100;
    private static final int CODE_HAZARD_ID = 101;
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, TABLE_HAZARDS, CODE_HAZARDS);
        uriMatcher.addURI(AUTHORITY, TABLE_HAZARDS + "/#", CODE_HAZARD_ID);
    }

    @Override
    public boolean onCreate() {
        Log.i(TAG, "ContentProvider Lifecycle: onCreate() — Worldwide Hazard Provider ready.");
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        String[] columns = {COLUMN_ID, COLUMN_TITLE, COLUMN_SEVERITY, COLUMN_LATITUDE, COLUMN_LONGITUDE};
        MatrixCursor cursor = new MatrixCursor(columns);

        switch (uriMatcher.match(uri)) {
            case CODE_HAZARDS:
                cursor.addRow(new Object[]{1, "Malacca Strait Gale Warning Force 8", "HIGH", 2.50, 101.50});
                cursor.addRow(new Object[]{2, "Rotterdam Europort Storm Surge +2.8m", "MODERATE", 51.95, 4.02});
                cursor.addRow(new Object[]{3, "North Atlantic Jetstream Turbulence FL360", "CRITICAL", 45.20, -35.10});
                break;
            case CODE_HAZARD_ID:
                cursor.addRow(new Object[]{1, "Malacca Strait Gale Warning Force 8", "HIGH", 2.50, 101.50});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case CODE_HAZARDS:
                return "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + TABLE_HAZARDS;
            case CODE_HAZARD_ID:
                return "vnd.android.cursor.item/vnd." + AUTHORITY + "." + TABLE_HAZARDS;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return Uri.withAppendedPath(CONTENT_URI, "4");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        return 0;
    }
}
