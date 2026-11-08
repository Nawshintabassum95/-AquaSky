package com.aquasky.navigation.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.aquasky.navigation.models.HazardAlert;
import com.aquasky.navigation.models.NavigationRoute;

import java.util.ArrayList;
import java.util.List;

/**
 * Offline-First SQLite Database:
 * Provides persistent local storage for routes, marine bathymetry,
 * and hazard bulletins so AquaSky navigates seamlessly without an internet connection.
 */
public class AquaSkyDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "AquaSkyDatabase";
    private static final String DATABASE_NAME = "aquasky_navigation.db";
    private static final int DATABASE_VERSION = 1;

    // Table: Saved Routes
    public static final String TABLE_ROUTES = "offline_routes";
    public static final String COL_ROUTE_ID = "id";
    public static final String COL_ROUTE_TITLE = "title";
    public static final String COL_ROUTE_DOMAIN = "domain";
    public static final String COL_ROUTE_ORIGIN = "origin";
    public static final String COL_ROUTE_DEST = "destination";
    public static final String COL_ROUTE_DISTANCE = "distance_km";
    public static final String COL_ROUTE_ETA = "eta_minutes";
    public static final String COL_ROUTE_RISK = "risk_score";
    public static final String COL_ROUTE_AVOIDED = "avoided_hazard";

    // Table: Offline Hazards
    public static final String TABLE_HAZARDS = "offline_hazards";
    public static final String COL_HAZARD_ID = "id";
    public static final String COL_HAZARD_TITLE = "title";
    public static final String COL_HAZARD_DESC = "description";
    public static final String COL_HAZARD_SEVERITY = "severity";
    public static final String COL_HAZARD_DOMAIN = "domain";
    public static final String COL_HAZARD_LAT = "latitude";
    public static final String COL_HAZARD_LNG = "longitude";
    public static final String COL_HAZARD_AGENCY = "source_agency";

    private static AquaSkyDatabaseHelper instance;

    public static synchronized AquaSkyDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new AquaSkyDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private AquaSkyDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createRoutesTable = "CREATE TABLE " + TABLE_ROUTES + " (" +
                COL_ROUTE_ID + " TEXT PRIMARY KEY, " +
                COL_ROUTE_TITLE + " TEXT, " +
                COL_ROUTE_DOMAIN + " TEXT, " +
                COL_ROUTE_ORIGIN + " TEXT, " +
                COL_ROUTE_DEST + " TEXT, " +
                COL_ROUTE_DISTANCE + " REAL, " +
                COL_ROUTE_ETA + " INTEGER, " +
                COL_ROUTE_RISK + " INTEGER, " +
                COL_ROUTE_AVOIDED + " TEXT);";

        String createHazardsTable = "CREATE TABLE " + TABLE_HAZARDS + " (" +
                COL_HAZARD_ID + " TEXT PRIMARY KEY, " +
                COL_HAZARD_TITLE + " TEXT, " +
                COL_HAZARD_DESC + " TEXT, " +
                COL_HAZARD_SEVERITY + " TEXT, " +
                COL_HAZARD_DOMAIN + " TEXT, " +
                COL_HAZARD_LAT + " REAL, " +
                COL_HAZARD_LNG + " REAL, " +
                COL_HAZARD_AGENCY + " TEXT);";

        db.execSQL(createRoutesTable);
        db.execSQL(createHazardsTable);
        Log.i(TAG, "Offline SQLite schema created successfully.");

        // Insert Default Offline Seed Data for Bangladesh & Global Marine Corridors
        seedOfflineData(db);
    }

    private void seedOfflineData(SQLiteDatabase db) {
        // Seed 1: Sea Route
        ContentValues sea = new ContentValues();
        sea.put(COL_ROUTE_ID, "offline_sea_1");
        sea.put(COL_ROUTE_TITLE, "Chattogram Port ➔ Singapore TSS (Deep Water)");
        sea.put(COL_ROUTE_DOMAIN, "SEA");
        sea.put(COL_ROUTE_ORIGIN, "Port of Chattogram (Karnaphuli Channel)");
        sea.put(COL_ROUTE_DEST, "Port of Singapore (Jurong Deep Sea Channel)");
        sea.put(COL_ROUTE_DISTANCE, 2740.0);
        sea.put(COL_ROUTE_ETA, 4200);
        sea.put(COL_ROUTE_RISK, 12);
        sea.put(COL_ROUTE_AVOIDED, "Bay of Bengal Cyclone High Swell (+4.5m)");
        db.insert(TABLE_ROUTES, null, sea);

        // Seed 2: Land Route
        ContentValues road = new ContentValues();
        road.put(COL_ROUTE_ID, "offline_road_1");
        road.put(COL_ROUTE_TITLE, "Dhaka Kawla Ramp ➔ Chattogram Agrabad C/A");
        road.put(COL_ROUTE_DOMAIN, "ROAD");
        road.put(COL_ROUTE_ORIGIN, "Dhaka Airport (Kawla Ramp)");
        road.put(COL_ROUTE_DEST, "Chattogram Agrabad C/A");
        road.put(COL_ROUTE_DISTANCE, 248.5);
        road.put(COL_ROUTE_ETA, 255);
        road.put(COL_ROUTE_RISK, 18);
        road.put(COL_ROUTE_AVOIDED, "Sayedabad ground-level flood zone & narrow Old Dhaka alleys");
        db.insert(TABLE_ROUTES, null, road);

        // Seed 3: Air Route
        ContentValues air = new ContentValues();
        air.put(COL_ROUTE_ID, "offline_air_1");
        air.put(COL_ROUTE_TITLE, "Airway W112 (DAC Dhaka ➔ CGP Chattogram)");
        air.put(COL_ROUTE_DOMAIN, "AIR");
        air.put(COL_ROUTE_ORIGIN, "Hazrat Shahjalal Intl (DAC Dhaka)");
        air.put(COL_ROUTE_DEST, "Shah Amanat Intl (CGP Chattogram)");
        air.put(COL_ROUTE_DISTANCE, 227.0);
        air.put(COL_ROUTE_ETA, 45);
        air.put(COL_ROUTE_RISK, 8);
        air.put(COL_ROUTE_AVOIDED, "Convective Thunderstorm Cell buffer maintained (+25 NM)");
        db.insert(TABLE_ROUTES, null, air);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HAZARDS);
        onCreate(db);
    }

    public List<NavigationRoute> getOfflineRoutes(String domain) {
        List<NavigationRoute> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_ROUTES, null, COL_ROUTE_DOMAIN + "=?", new String[]{domain}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ROUTE_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_ROUTE_TITLE));
                String dom = cursor.getString(cursor.getColumnIndexOrThrow(COL_ROUTE_DOMAIN));
                double distance = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_ROUTE_DISTANCE));
                int eta = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ROUTE_ETA));
                int risk = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ROUTE_RISK));
                String avoided = cursor.getString(cursor.getColumnIndexOrThrow(COL_ROUTE_AVOIDED));

                list.add(new NavigationRoute(id, title, dom, distance, eta, risk, false, avoided));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }
}
