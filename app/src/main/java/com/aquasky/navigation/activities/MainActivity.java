package com.aquasky.navigation.activities;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import com.aquasky.navigation.network.NearbyPlacesClient;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.aquasky.navigation.R;
import com.aquasky.navigation.components.ConnectivityAndHazardReceiver;
import com.aquasky.navigation.components.ContentResolverUsageHelper;
import com.aquasky.navigation.components.HazardMonitoringService;
import com.aquasky.navigation.database.AquaSkyDatabaseHelper;
import com.aquasky.navigation.firebase.FirebaseAuthHelper;
import com.aquasky.navigation.firebase.FirestoreRoadReportSync;
import com.aquasky.navigation.firebase.FirestoreUserProfile;
import com.aquasky.navigation.firebase.RealtimeHazardSync;
import com.aquasky.navigation.models.HazardAlert;
import com.aquasky.navigation.models.VehicleProfile;
import com.aquasky.navigation.network.ApiClient;
import com.aquasky.navigation.network.WeatherApiResponse;
import com.aquasky.navigation.utils.PermissionHelper;
import com.aquasky.navigation.utils.RoadRouter;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.TilesOverlay;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * AquaSky Worldwide Navigation (First Page)
 * Features:
 * - Live OpenStreetMap (osmdroid) MapView integration - no API key needed
 * - Vehicle-specific distinct Map Route rendering (Car, Bus, Truck, Bike, CNG)
 * - Multimodal domain switching (Sea, Air, Road)
 * - Complete Activity Lifecycle instrumentation
 */
public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        View.OnLongClickListener,
        ConnectivityAndHazardReceiver.OnConnectivityChangeListener {

    private static final String TAG = "MainActivityLifecycle";
    private static final String PREFS_NAME = "AquaSkySettings";
    private static final String KEY_THEME_DARK = "isDarkMode";
    private static final int REQUEST_CODE_SELECT_ROUTE = 5001;

    // UI Widgets
    private TextView tvAppName, tvAppSlogan, tvAlertTitle, tvAlertDescription, tvAlertFreshness;
    private TextView tvWaveHeightValue, tvWindSpeedValue, tvTideDraftValue, tvRiskScoreValue, tvLifecycleTracker, tvNetworkSyncStatus;
    private TextView tvMapLayerTag, tvMapTelemetryBadge, tvMapRouteStatus, tvVehicleSpecificInfo;
    private MaterialButton btnAuthAction, btnThemeToggle, btnModeSea, btnModeAir, btnModeRoad;
    private MaterialButton btnLaunchNavigation, btnLaunchComparison, btnLaunchListView, btnSimulateOfflineToggle;
    private MaterialButton btnShareAlertImplicit, btnAcceptDetour, btnReportIssue, btnSos;
    private AutoCompleteTextView actvOrigin, actvDestination;
    private ImageButton btnSwapEndpoints;
    private ChipGroup chipGroupVehicles;
    private Chip chipCar, chipBus, chipTruck, chipBike, chipCng;
    private CardView cardLiveAlert, cardAlertAttachedImage;
    private android.widget.ImageView ivAlertAttachedImage;
    private ScrollView mainScrollView;

    // Map Components
    private MapView mapViewMain;
    private Polyline currentActivePolyline;
    private TilesOverlay seaMarkOverlay;                       // OpenSeaMap nautical layer (SEA mode only)
    private final List<org.osmdroid.views.overlay.Overlay> routeOverlays = new ArrayList<>();

    // State Variables
    private String currentMode = "SEA"; // "SEA", "AIR", "ROAD"
    private VehicleProfile.Type selectedVehicleType = VehicleProfile.Type.CAR_SUV;
    private boolean isOfflineSimulated = false;
    private ConnectivityAndHazardReceiver dynamicReceiver;

    // Worldwide Autocomplete Datasets
    private final String[] WORLD_SEA_PORTS = {
            "Port of Singapore (Jurong Deep Sea Channel)",
            "Port of Rotterdam (Europort Maasvlakte)",
            "Port of Shanghai (Yangshan Deep Water Port)",
            "Port of Dubai (Jebel Ali)",
            "Port of Los Angeles / Long Beach",
            "Port of Chattogram (Karnaphuli Channel)",
            "Port of Colombo (South Container Terminal)",
            "Port of Yokohama / Tokyo Bay",
            "Suez Canal Northern Transit (Port Said)"
    };

    private final String[] WORLD_AIRPORTS = {
            "Singapore Changi Airport (SIN)",
            "London Heathrow Airport (LHR)",
            "New York John F. Kennedy (JFK)",
            "Dubai International Airport (DXB)",
            "Tokyo Haneda Airport (HND)",
            "Hazrat Shahjalal Intl (DAC Dhaka)",
            "Suvarnabhumi Airport (BKK Bangkok)",
            "Frankfurt International (FRA)"
    };

    private final String[] WORLD_HIGHWAYS = {
            "Singapore Ayer Rajah Expressway (AYE)",
            "Rotterdam A15 Europort Highway",
            "New York I-95 North Corridor",
            "Tokyo Shuto Expressway Loop",
            "Dhaka Airport (Kawla Ramp - Elevated Expressway)",
            "Dubai Sheikh Zayed Road (E11)",
            "London M25 Orbital Motorway"
    };

    // =========================================================================
    // 1. ALL ANDROID ACTIVITY LIFECYCLE METHODS
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Lifecycle: onCreate() — Initializing AquaSky Worldwide Navigation.");

        // Apply saved theme preference
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDark = prefs.getBoolean(KEY_THEME_DARK, true);
        AppCompatDelegate.setDefaultNightMode(isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        // osmdroid MUST be configured before the MapView is inflated:
        // user-agent is mandatory (OSM servers block requests without it) and
        // the tile cache goes to app-private storage (no storage permission needed).
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        File osmBase = new File(getCacheDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(osmBase);
        Configuration.getInstance().setOsmdroidTileCache(new File(osmBase, "tiles"));

        setContentView(R.layout.activity_main);

        // Initialize UI Views
        initViews();

        // Initialize the OpenStreetMap view and draw the first route
        setupMap();

        // Nearby places (restaurants, fuel, hotels, ...)
        setupNearbyPlaces();

        // Setup Event Listeners
        setupEventListeners();

        // Request Permissions
        PermissionHelper.requestRequiredPermissions(this);

        // Start Foreground Service
        startHazardService();

        // Realtime Firebase Ingestion
        setupFirebaseRealtimeHazards();

        // Live Worldwide Weather
        fetchMarineWeatherViaHttp(1.29, 103.85);

        // ContentResolver Demonstration
        ContentResolverUsageHelper.queryActiveHazardsViaResolver(this);

        updateLifecycleTracker("onCreate() executed. MapView initialized.");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Lifecycle: onStart() — Activity becoming visible.");
        updateLifecycleTracker("onStart() — Activity visible.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapViewMain != null) mapViewMain.onResume();

        dynamicReceiver = new ConnectivityAndHazardReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(ConnectivityAndHazardReceiver.ACTION_HAZARD_ALERT);
        filter.addAction(ConnectivityAndHazardReceiver.ACTION_HAZARD_ALERT);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);   // notun
        filter.addAction(Intent.ACTION_BATTERY_LOW);             // notun
        filter.addAction(Intent.ACTION_BATTERY_OKAY);            // notun
        registerReceiver(dynamicReceiver, filter);
        registerReceiver(dynamicReceiver, filter);
        ConnectivityAndHazardReceiver.setConnectivityListener(this);

        refreshUserState();
        updateLifecycleTracker("onResume() — Interactive. MapView running.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapViewMain != null) mapViewMain.onPause();
        Log.i(TAG, "Lifecycle: onPause() — Receiver unregistering.");

        if (dynamicReceiver != null) {
            try {
                unregisterReceiver(dynamicReceiver);
            } catch (Exception ignored) {}
        }
        updateLifecycleTracker("onPause() — Partially obscured.");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "Lifecycle: onStop() — Backgrounded.");
        updateLifecycleTracker("onStop() — Backgrounded.");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "Lifecycle: onRestart() — Restoring visibility.");
        updateLifecycleTracker("onRestart() — Restoring visibility.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapViewMain != null) mapViewMain.onDetach();
        Log.i(TAG, "Lifecycle: onDestroy() — Freeing resources.");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("saved_mode", currentMode);
        outState.putString("saved_origin", actvOrigin.getText().toString());
        outState.putString("saved_destination", actvDestination.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentMode = savedInstanceState.getString("saved_mode", "SEA");
        actvOrigin.setText(savedInstanceState.getString("saved_origin", ""));
        actvDestination.setText(savedInstanceState.getString("saved_destination", ""));
        applyModeSelection(currentMode);
    }

    private static final int REQUEST_CODE_PICK_IMAGE = 5002;
    private android.net.Uri selectedReportImageUri = null;
    private android.widget.ImageView ivReportPreviewRef = null;
    private String userAddedHazardTitle = null;
    private String userAddedHazardDesc = null;
    private String userAddedHazardFreshness = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == REQUEST_CODE_SELECT_ROUTE && resultCode == RESULT_OK && data != null) {
            String routeId = data.getStringExtra("SELECTED_ROUTE_ID");
            String routeName = data.getStringExtra("SELECTED_ROUTE_NAME");
            String typeTag = data.getStringExtra("SELECTED_ROUTE_TYPE");
            int risk = data.getIntExtra("SELECTED_ROUTE_RISK", 15);
            int eta = data.getIntExtra("SELECTED_ROUTE_ETA", 180);
            double dist = data.getDoubleExtra("SELECTED_ROUTE_DIST", 300.0);
            String avoided = data.getStringExtra("SELECTED_ROUTE_AVOIDED");
            String domain = data.getStringExtra("SELECTED_ROUTE_DOMAIN");

            applySelectedRouteToMap(routeId, routeName, typeTag, risk, eta, dist, avoided, domain);
        }

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedReportImageUri = data.getData();
            if (selectedReportImageUri != null) {
                showReportDialog();
                Toast.makeText(this, "📷 Evidence Photo Attached via MediaStore!", Toast.LENGTH_SHORT).show();
            }
        }
    }
    // =========================================================================
    // 2. VIEW INITIALIZATION & EVENT LISTENERS
    // =========================================================================

    private void initViews() {
        tvAppName = findViewById(R.id.tvAppName);
        tvAppSlogan = findViewById(R.id.tvAppSlogan);
        tvAlertTitle = findViewById(R.id.tvAlertTitle);
        tvAlertDescription = findViewById(R.id.tvAlertDescription);
        tvAlertFreshness = findViewById(R.id.tvAlertFreshness);
        tvWaveHeightValue = findViewById(R.id.tvWaveHeightValue);
        tvWindSpeedValue = findViewById(R.id.tvWindSpeedValue);
        tvTideDraftValue = findViewById(R.id.tvTideDraftValue);
        tvRiskScoreValue = findViewById(R.id.tvRiskScoreValue);
        tvLifecycleTracker = findViewById(R.id.tvLifecycleTracker);
        tvNetworkSyncStatus = findViewById(R.id.tvNetworkSyncStatus);

        tvMapLayerTag = findViewById(R.id.tvMapLayerTag);
        tvMapTelemetryBadge = findViewById(R.id.tvMapTelemetryBadge);
        tvMapRouteStatus = findViewById(R.id.tvMapRouteStatus);
        tvVehicleSpecificInfo = findViewById(R.id.tvVehicleSpecificInfo);

        btnAuthAction = findViewById(R.id.btnAuthAction);
        btnThemeToggle = findViewById(R.id.btnThemeToggle);
        btnModeSea = findViewById(R.id.btnModeSea);
        btnModeAir = findViewById(R.id.btnModeAir);
        btnModeRoad = findViewById(R.id.btnModeRoad);
        btnLaunchNavigation = findViewById(R.id.btnLaunchNavigation);
        btnLaunchComparison = findViewById(R.id.btnLaunchComparison);
        btnLaunchListView = findViewById(R.id.btnLaunchListView);
        btnSimulateOfflineToggle = findViewById(R.id.btnSimulateOfflineToggle);
        btnShareAlertImplicit = findViewById(R.id.btnShareAlertImplicit);
        btnAcceptDetour = findViewById(R.id.btnAcceptDetour);
        btnReportIssue = findViewById(R.id.btnReportIssue);
        btnSos = findViewById(R.id.btnSos);

        actvOrigin = findViewById(R.id.actvOrigin);
        actvDestination = findViewById(R.id.actvDestination);
        btnSwapEndpoints = findViewById(R.id.btnSwapEndpoints);
        chipGroupVehicles = findViewById(R.id.chipGroupVehicles);
        cardLiveAlert = findViewById(R.id.cardLiveAlert);
        cardAlertAttachedImage = findViewById(R.id.cardAlertAttachedImage);
        ivAlertAttachedImage = findViewById(R.id.ivAlertAttachedImage);

        chipCar = findViewById(R.id.chipCar);
        chipBus = findViewById(R.id.chipBus);
        chipTruck = findViewById(R.id.chipTruck);
        chipBike = findViewById(R.id.chipBike);
        chipCng = findViewById(R.id.chipCng);

        mainScrollView = findViewById(R.id.mainScrollView);
        mapViewMain = findViewById(R.id.mapViewMain);

        updateAutocompleteAdapters();
    }

    private void setupEventListeners() {
        btnAuthAction.setOnClickListener(this);
        btnThemeToggle.setOnClickListener(this);
        btnModeSea.setOnClickListener(this);
        btnModeAir.setOnClickListener(this);
        btnModeRoad.setOnClickListener(this);
        btnLaunchNavigation.setOnClickListener(this);
        btnLaunchComparison.setOnClickListener(this);
        btnLaunchListView.setOnClickListener(this);
        btnSimulateOfflineToggle.setOnClickListener(this);
        btnShareAlertImplicit.setOnClickListener(this);
        btnAcceptDetour.setOnClickListener(this);
        btnReportIssue.setOnClickListener(this);
        btnSos.setOnClickListener(this);
        btnSwapEndpoints.setOnClickListener(this);

        cardLiveAlert.setOnLongClickListener(this);

        actvDestination.setOnItemClickListener((parent, view, position, id) -> {
            updateHazardAlertForDestination(actvDestination.getText().toString());
        });

        if (ivAlertAttachedImage != null) {
            ivAlertAttachedImage.setOnClickListener(v -> {
                if (selectedReportImageUri != null) {
                    android.widget.ImageView fullImg = new android.widget.ImageView(this);
                    fullImg.setImageURI(selectedReportImageUri);
                    fullImg.setAdjustViewBounds(true);
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("📷 Incident Photo Evidence (Full View)")
                            .setView(fullImg)
                            .setPositiveButton("Close", null)
                            .show();
                }
            });
        }

        // Individual click handlers for each vehicle chip to guarantee selection
        chipCar.setOnClickListener(v -> selectVehicle(VehicleProfile.Type.CAR_SUV));
        chipBus.setOnClickListener(v -> selectVehicle(VehicleProfile.Type.TRANSIT_BUS));
        chipTruck.setOnClickListener(v -> selectVehicle(VehicleProfile.Type.CARGO_TRUCK));
        chipBike.setOnClickListener(v -> selectVehicle(VehicleProfile.Type.MOTORCYCLE));
        chipCng.setOnClickListener(v -> selectVehicle(VehicleProfile.Type.CNG_AUTO));

        // Group listener fallback
        chipGroupVehicles.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipCar) selectVehicle(VehicleProfile.Type.CAR_SUV);
            else if (checkedId == R.id.chipBus) selectVehicle(VehicleProfile.Type.TRANSIT_BUS);
            else if (checkedId == R.id.chipTruck) selectVehicle(VehicleProfile.Type.CARGO_TRUCK);
            else if (checkedId == R.id.chipBike) selectVehicle(VehicleProfile.Type.MOTORCYCLE);
            else if (checkedId == R.id.chipCng) selectVehicle(VehicleProfile.Type.CNG_AUTO);
        });
    }

    // =========================================================================
    // 3. MAP RENDERING & VEHICLE-SPECIFIC MAP ROUTE UPDATES
    // =========================================================================

    // =========================================================================
    // 3. MAP RENDERING & VEHICLE-SPECIFIC MAP ROUTE UPDATES
    // =========================================================================

    private void setupMap() {
        if (mapViewMain == null) return;

        mapViewMain.setTileSource(TileSourceFactory.MAPNIK);          // normal land map (OpenStreetMap)
        mapViewMain.setMultiTouchControls(true);                      // pinch to zoom
        mapViewMain.setMinZoomLevel(2.0);
        mapViewMain.setMaxZoomLevel(18.0);
        mapViewMain.setVerticalMapRepetitionEnabled(false);
        mapViewMain.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        mapViewMain.getController().setZoom(3.0);
        mapViewMain.getController().setCenter(new GeoPoint(20.0, 80.0));

        // The MapView lives inside a ScrollView: stop the ScrollView from stealing the map's drag gestures
        mapViewMain.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });

        // OpenSeaMap seamarks (buoys, lights, shipping lanes) - drawn over the land map
        OnlineTileSourceBase seaMarks = new OnlineTileSourceBase("OpenSeaMap", 0, 18, 256, ".png",
                new String[]{"https://tiles.openseamap.org/seamark/"}) {
            @Override
            public String getTileURLString(long pMapTileIndex) {
                return getBaseUrl() + MapTileIndex.getZoom(pMapTileIndex) + "/"
                        + MapTileIndex.getX(pMapTileIndex) + "/"
                        + MapTileIndex.getY(pMapTileIndex) + mImageFilenameEnding;
            }
        };
        MapTileProviderBasic seaProvider = new MapTileProviderBasic(getApplicationContext(), seaMarks);
        seaMarkOverlay = new TilesOverlay(seaProvider, this);
        seaMarkOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
        seaMarkOverlay.setLoadingLineColor(Color.TRANSPARENT);

        updateHazardAlertForDestination(actvDestination.getText().toString());

        renderCurrentDomainAndVehicleOnMap();
    }

    private void scrollToMap() {
        if (mainScrollView == null) return;
        final View mapCard = findViewById(R.id.cardMapContainer);
        if (mapCard == null) return;
        if (actvOrigin != null) actvOrigin.clearFocus();
        if (actvDestination != null) actvDestination.clearFocus();
        mainScrollView.postDelayed(() -> {
            mainScrollView.smoothScrollTo(0, mapCard.getTop());
        }, 150);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void clearRouteOverlays() {
        if (mapViewMain == null) return;
        mapViewMain.getOverlays().removeAll(routeOverlays);
        routeOverlays.clear();
        currentActivePolyline = null;

    }

    /** Green pin "A" = origin, red pin "B" = destination. (green colour => A, anything else => B) */
    private void addMarker(GeoPoint position, String title, String snippet, int color) {
        boolean isOrigin = (color == Color.parseColor("#10B981"));

        Marker marker = new Marker(mapViewMain);
        marker.setPosition(position);
        marker.setTitle(title);
        if (snippet != null) marker.setSnippet(snippet);
        marker.setIcon(createPin(isOrigin ? "A" : "B", color));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        mapViewMain.getOverlays().add(marker);
        routeOverlays.add(marker);
    }

    private android.graphics.drawable.Drawable createPin(String label, int color) {
        int w = dp(34), h = dp(44);
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

        float r = w / 2f - dp(2);
        float cx = w / 2f, cy = r + dp(2);

        android.graphics.Path tip = new android.graphics.Path();       // pointer at the bottom
        tip.moveTo(cx - r * 0.55f, cy + r * 0.75f);
        tip.lineTo(cx, h - dp(1));
        tip.lineTo(cx + r * 0.55f, cy + r * 0.75f);
        tip.close();

        paint.setStyle(android.graphics.Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawPath(tip, paint);
        canvas.drawCircle(cx, cy, r, paint);

        paint.setStyle(android.graphics.Paint.Style.STROKE);           // white border
        paint.setStrokeWidth(dp(2));
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, r, paint);

        paint.setStyle(android.graphics.Paint.Style.FILL);             // letter
        paint.setColor(Color.WHITE);
        paint.setTextSize(dp(16));
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextAlign(android.graphics.Paint.Align.CENTER);
        android.graphics.Paint.FontMetrics fm = paint.getFontMetrics();
        canvas.drawText(label, cx, cy - (fm.ascent + fm.descent) / 2f, paint);

        return new android.graphics.drawable.BitmapDrawable(getResources(), bmp);
    }

    private Polyline addRoutePolyline(List<GeoPoint> points, int color, int widthDp) {
        Polyline line = new Polyline(mapViewMain);
        line.setPoints(points);
        line.setGeodesic(true);
        line.getOutlinePaint().setColor(color);
        line.getOutlinePaint().setStrokeWidth(dp(widthDp * 0.6f));
        line.getOutlinePaint().setStrokeCap(android.graphics.Paint.Cap.ROUND);

        mapViewMain.getOverlays().add(line);
        routeOverlays.add(line);
        return line;
    }

    private void fitRoute(final List<GeoPoint> points, final GeoPoint fallbackCenter) {
        if (mapViewMain == null) return;
        // post(): wait until the MapView has its real width/height, otherwise the zoom maths is wrong
        mapViewMain.post(() -> {
            try {
                BoundingBox box = BoundingBox.fromGeoPoints(points);
                mapViewMain.zoomToBoundingBox(box.increaseByScale(1.25f), true, dp(24));
            } catch (Exception e) {
                mapViewMain.getController().setZoom(8.5);
                mapViewMain.getController().setCenter(fallbackCenter);
            }
            mapViewMain.invalidate();
        });
    }

    // ---------- vehicle-aware road routing glue ----------

    private static class RoadUi {
        final List<GeoPoint> points = new ArrayList<>();
        int color;
        int width;
        String layer;
        String speedEta;
        String note;
    }

    private RoadUi buildRoadUi(GeoPoint start, GeoPoint end, String originText, String destText) {
        RoadRouter.Result r = RoadRouter.findRoute(selectedVehicleType,
                start.getLatitude(), start.getLongitude(), end.getLatitude(), end.getLongitude());

        RoadUi ui = new RoadUi();
        switch (selectedVehicleType) {
            case TRANSIT_BUS:  ui.color = Color.parseColor("#F97316"); ui.width = 9;  break;
            case CARGO_TRUCK:  ui.color = Color.parseColor("#EF4444"); ui.width = 11; break;
            case MOTORCYCLE:   ui.color = Color.parseColor("#10B981"); ui.width = 6;  break;
            case CNG_AUTO:     ui.color = Color.parseColor("#F59E0B"); ui.width = 7;  break;
            default:           ui.color = Color.parseColor("#0284C7"); ui.width = 10; break;
        }

        if (!r.found) {
            ui.points.add(start);
            ui.points.add(end);
            ui.layer = r.vehicleEmoji + " " + r.vehicleLabel + ": No safe route";
            ui.speedEta = "No route";
            ui.note = r.message;
            tvVehicleSpecificInfo.setText(r.vehicleEmoji + " " + r.vehicleLabel + ": " + r.message);
            return ui;
        }

        // typed start -> first junction -> ... -> last junction -> typed end
        ui.points.add(start);
        for (double[] c : r.coords) ui.points.add(new GeoPoint(c[0], c[1]));
        ui.points.add(end);

        int h = r.etaMinutes / 60, m = r.etaMinutes % 60;
        String eta = h > 0 ? h + "h " + String.format(java.util.Locale.US, "%02d", m) + "m" : m + " min";
        double avg = r.distanceKm / Math.max(r.etaMinutes / 60.0, 0.01);

        ui.layer = r.vehicleEmoji + " " + r.vehicleLabel + " Route · Risk " + r.riskScore + "/100";
        ui.speedEta = String.format(java.util.Locale.US, "%.0f km/h avg · ETA %s", avg, eta);

        StringBuilder note = new StringBuilder();
        note.append(String.format(java.util.Locale.US, "%s ➔ %s · %.0f km · Toll %d BDT",
                originText, destText, r.distanceKm, r.tollBdt));
        for (int i = 0; i < Math.min(2, r.notes.size()); i++) note.append("\n").append(r.notes.get(i));
        ui.note = note.toString();

        StringBuilder info = new StringBuilder();
        info.append(r.vehicleEmoji).append(' ').append(r.vehicleLabel).append(": ");
        info.append(String.format(java.util.Locale.US, "%.0f km, ETA %s, Toll %d BDT, Risk %d/100.", r.distanceKm, eta, r.tollBdt, r.riskScore));
        for (String n : r.notes) info.append("\n• ").append(n);
        tvVehicleSpecificInfo.setText(info.toString());

        // keep the risk tile in sync with the chosen vehicle
        String level = r.riskScore < 25 ? "Low" : (r.riskScore < 50 ? "Moderate" : "High");
        tvRiskScoreValue.setText(r.riskScore + " / 100 (" + level + ")");
        tvRiskScoreValue.setTextColor(ContextCompat.getColor(this,
                r.riskScore < 25 ? R.color.status_safe_emerald
                        : (r.riskScore < 50 ? R.color.status_warning_amber : R.color.status_critical_red)));
        return ui;
    }

    private void selectVehicle(VehicleProfile.Type type) {
        this.selectedVehicleType = type;
        updateHazardAlertForDestination(actvDestination.getText().toString());

        renderCurrentDomainAndVehicleOnMap();
        Toast.makeText(this, "Active Vehicle Mode: " + type.name(), Toast.LENGTH_SHORT).show();
    }

    private void renderCurrentDomainAndVehicleOnMap() {
        if (mapViewMain == null) return;

        clearRouteOverlays();

        List<GeoPoint> points = new ArrayList<>();
        int routeColor;
        int routeWidth;
        String layerName;
        String speedEta;
        String routeNote;

        if ("SEA".equals(currentMode)) {
            // Worldwide Marine TSS Lane: Singapore to Rotterdam via Malacca & Suez
            GeoPoint singapore = new GeoPoint(1.29, 103.85);
            GeoPoint malaccaStrait = new GeoPoint(2.50, 101.50);
            GeoPoint sriLanka = new GeoPoint(5.90, 80.50);
            GeoPoint aden = new GeoPoint(12.50, 44.00);
            GeoPoint suez = new GeoPoint(27.80, 34.00);
            GeoPoint gibraltar = new GeoPoint(36.10, -5.35);
            GeoPoint rotterdam = new GeoPoint(51.95, 4.02);

            points.add(singapore);
            points.add(malaccaStrait);
            points.add(sriLanka);
            points.add(aden);
            points.add(suez);
            points.add(gibraltar);
            points.add(rotterdam);

            routeColor = Color.parseColor("#0284C7"); // Marine Ocean Blue
            routeWidth = 8;
            layerName = "🌊 Deep-Water TSS Channel (+16.5m Draft)";
            speedEta = "21 kts · ETA 16.7 Days";
            routeNote = "Marine TSS: Singapore ➔ Malacca Strait ➔ Suez ➔ Rotterdam";

            addMarker(singapore, "Port of Singapore", "Jurong Deep Channel", Color.parseColor("#10B981"));
            addMarker(rotterdam, "Port of Rotterdam", "Europort Maasvlakte", Color.parseColor("#EF4444"));

        } else if ("AIR".equals(currentMode)) {
            // Worldwide Airway Corridor: Singapore (SIN) to London Heathrow (LHR)
            GeoPoint sin = new GeoPoint(1.36, 103.99);
            GeoPoint bayOfBengal = new GeoPoint(12.00, 90.00);
            GeoPoint india = new GeoPoint(22.00, 78.00);
            GeoPoint dxb = new GeoPoint(25.25, 55.36);
            GeoPoint turkey = new GeoPoint(39.00, 35.00);
            GeoPoint lhr = new GeoPoint(51.47, -0.45);

            points.add(sin);
            points.add(bayOfBengal);
            points.add(india);
            points.add(dxb);
            points.add(turkey);
            points.add(lhr);

            routeColor = Color.parseColor("#8B5CF6"); // Airway Purple/Magenta
            routeWidth = 6;
            layerName = "✈️ Controlled Airway L642 (FL370)";
            speedEta = "488 kts · ETA 12h 45m";
            routeNote = "Trans-Oceanic Corridor: SIN ➔ DXB ➔ LHR (Storm Buffer Active)";

            addMarker(sin, "Singapore Changi (SIN)", null, Color.parseColor("#10B981"));
            addMarker(lhr, "London Heathrow (LHR)", null, Color.parseColor("#EF4444"));

        } else {
            // Road navigation: constraint-based routing per vehicle (see RoadRouter)
            String oText = actvOrigin.getText().toString().trim();
            String dText = actvDestination.getText().toString().trim();
            GeoPoint pOrigin = oText.isEmpty() ? new GeoPoint(23.85, 90.40) : resolveCoordinatesForLocation(oText, true);
            GeoPoint pDest = dText.isEmpty() ? new GeoPoint(22.34, 91.81) : resolveCoordinatesForLocation(dText, false);

            RoadUi ui = buildRoadUi(pOrigin, pDest, oText.isEmpty() ? "Dhaka Airport" : oText, dText.isEmpty() ? "Chattogram" : dText);
            points.addAll(ui.points);
            routeColor = ui.color;
            routeWidth = ui.width;
            layerName = ui.layer;
            speedEta = ui.speedEta;
            routeNote = ui.note;

            addMarker(pOrigin, "Origin Point", null, Color.parseColor("#10B981"));
            addMarker(pDest, "Destination", null, Color.parseColor("#EF4444"));
        }

        // Draw the route polyline on the map
        currentActivePolyline = addRoutePolyline(points, routeColor, routeWidth);

        // Update Overlays
        tvMapLayerTag.setText(layerName);
        tvMapTelemetryBadge.setText(speedEta);
        tvMapRouteStatus.setText(routeNote);

        fitRoute(points, points.get(0));
    }

    // =========================================================================
    // 4. CLICK LISTENERS & USER ACTIONS
    // =========================================================================

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btnAuthAction) {
            handleAuthClick();
        } else if (id == R.id.btnThemeToggle) {
            toggleAppTheme();
        } else if (id == R.id.btnModeSea) {
            applyModeSelection("SEA");
        } else if (id == R.id.btnModeAir) {
            applyModeSelection("AIR");
        } else if (id == R.id.btnModeRoad) {
            applyModeSelection("ROAD");
        } else if (id == R.id.btnSwapEndpoints) {
            swapEndpoints();
        } else if (id == R.id.btnLaunchNavigation) {
            showCalculatedRouteOnMap();
        } else if (id == R.id.btnLaunchComparison) {
            Intent intent = new Intent(MainActivity.this, RouteComparisonActivity.class);
            intent.putExtra("EXTRA_DOMAIN", currentMode);
            intent.putExtra("EXTRA_ORIGIN", actvOrigin.getText().toString());
            intent.putExtra("EXTRA_DESTINATION", actvDestination.getText().toString());
            startActivityForResult(intent, REQUEST_CODE_SELECT_ROUTE);
        } else if (id == R.id.btnLaunchListView) {
            showOfflineSavedMapsDialog();
        } else if (id == R.id.btnSos) {
            triggerSos();
        } else if (id == R.id.btnReportIssue) {
            showReportDialog();
        } else if (id == R.id.btnShareAlertImplicit) {
            shareHazardAlertImplicit();
        } else if (id == R.id.btnAcceptDetour) {
            applySmartHazardDetour();
        } else if (id == R.id.btnSimulateOfflineToggle) {
            toggleOfflineSimulation();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == R.id.cardLiveAlert) {
            triggerSos();
            return true;
        }
        return false;
    }

    // =====================================================================
    //  NEARBY PLACES (Google-Maps style): category chips -> OpenStreetMap data
    // =====================================================================

    private static final int REQ_NEARBY_LOCATION = 3310;
    private final List<Marker> nearbyMarkers = new ArrayList<>();
    private Marker myLocationMarker;
    private Runnable pendingLocationAction;
    private boolean locationDenied = false;
    private View cardNearbyResults;
    private TextView tvNearbyTitle;
    private android.widget.LinearLayout nearbyList;

    private interface LocationResult {
        void onLocation(double lat, double lng, boolean fromGps);
    }

    private void setupNearbyPlaces() {
        cardNearbyResults = findViewById(R.id.cardNearbyResults);
        tvNearbyTitle = findViewById(R.id.tvNearbyTitle);
        nearbyList = findViewById(R.id.nearbyList);

        findViewById(R.id.chipMyLocation).setOnClickListener(v ->
                getUserLocation((lat, lng, fromGps) -> {
                    showMyLocation(lat, lng);
                    mapViewMain.getController().setZoom(15.5);
                    mapViewMain.getController().animateTo(new GeoPoint(lat, lng));
                    if (!fromGps) {
                        Toast.makeText(this, "Using the map centre (GPS not available).", Toast.LENGTH_SHORT).show();
                    }
                }));

        bindNearbyChip(R.id.chipNearRestaurant, NearbyPlacesClient.Category.RESTAURANT);
        bindNearbyChip(R.id.chipNearGas, NearbyPlacesClient.Category.GAS);
        bindNearbyChip(R.id.chipNearHotel, NearbyPlacesClient.Category.HOTEL);
        bindNearbyChip(R.id.chipNearHospital, NearbyPlacesClient.Category.HOSPITAL);
        bindNearbyChip(R.id.chipNearShopping, NearbyPlacesClient.Category.SHOPPING);
        bindNearbyChip(R.id.chipNearAtm, NearbyPlacesClient.Category.ATM);
        bindNearbyChip(R.id.chipNearCafe, NearbyPlacesClient.Category.CAFE);
    }

    private void bindNearbyChip(int chipId, final NearbyPlacesClient.Category category) {
        final Chip chip = findViewById(chipId);
        chip.setOnClickListener(v -> {
            if (!chip.isChecked()) {          // tapped again -> switch the suggestions off
                clearNearby();
                return;
            }
            searchNearby(category);
        });
    }

    private void searchNearby(final NearbyPlacesClient.Category category) {
        getUserLocation((lat, lng, fromGps) -> {
            showMyLocation(lat, lng);
            removeNearbyMarkers();
            nearbyList.removeAllViews();
            cardNearbyResults.setVisibility(View.VISIBLE);
            tvNearbyTitle.setText(category.emoji + " Searching " + category.label.toLowerCase(java.util.Locale.US) + "…");

            NearbyPlacesClient.search(category, lat, lng, 2000, new NearbyPlacesClient.Callback() {
                @Override
                public void onResult(List<NearbyPlacesClient.Place> places) {
                    if (isFinishing()) return;
                    showNearbyResults(category, places, lat, lng, fromGps);
                }

                @Override
                public void onError(String message) {
                    if (isFinishing()) return;
                    tvNearbyTitle.setText("⚠️ " + message);
                }
            });
        });
    }

    private void showNearbyResults(NearbyPlacesClient.Category category, List<NearbyPlacesClient.Place> places,
                                   double lat, double lng, boolean fromGps) {
        removeNearbyMarkers();
        nearbyList.removeAllViews();

        String where = fromGps ? "near you" : "around the map centre";
        if (places.isEmpty()) {
            tvNearbyTitle.setText(category.emoji + " No " + category.label.toLowerCase(java.util.Locale.US)
                    + " found within 2 km " + where);
            return;
        }
        tvNearbyTitle.setText(category.emoji + " " + category.label + " " + where + " · " + places.size() + " found");

        List<GeoPoint> bounds = new ArrayList<>();
        bounds.add(new GeoPoint(lat, lng));

        int index = 1;
        for (NearbyPlacesClient.Place place : places) {
            final GeoPoint point = new GeoPoint(place.lat, place.lng);
            bounds.add(point);

            String distance = place.distanceM < 1000
                    ? Math.round(place.distanceM) + " m"
                    : String.format(java.util.Locale.US, "%.1f km", place.distanceM / 1000f);
            String sub = place.details.isEmpty() ? distance : distance + " · " + place.details;

            final Marker marker = new Marker(mapViewMain);
            marker.setPosition(point);
            marker.setTitle(place.name);
            marker.setSnippet(sub);
            marker.setIcon(createPin(category.emoji, category.color));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapViewMain.getOverlays().add(marker);
            nearbyMarkers.add(marker);

            TextView row = new TextView(this);
            row.setText(index + ". " + place.name + "\n" + sub);
            row.setTextColor(Color.parseColor("#F1F5F9"));
            row.setTextSize(13);
            row.setBackgroundColor(Color.parseColor("#0F172A"));
            row.setPadding(dp(14), dp(10), dp(14), dp(10));
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(1);
            row.setLayoutParams(lp);
            row.setOnClickListener(v -> {          // tap a row -> fly to the place and open its info bubble
                mapViewMain.getController().setZoom(17.0);
                mapViewMain.getController().animateTo(point);
                marker.showInfoWindow();
            });
            nearbyList.addView(row);
            index++;
        }

        // zoom so you and every suggestion are visible
        final BoundingBox box = BoundingBox.fromGeoPoints(bounds).increaseByScale(1.3f);
        mapViewMain.post(() -> {
            try {
                mapViewMain.zoomToBoundingBox(box, true, dp(30));
            } catch (Exception ignored) { }
            mapViewMain.invalidate();
        });
    }

    private void removeNearbyMarkers() {
        if (mapViewMain == null) return;
        mapViewMain.getOverlays().removeAll(nearbyMarkers);
        nearbyMarkers.clear();
        mapViewMain.invalidate();
    }

    private void clearNearby() {
        removeNearbyMarkers();
        if (nearbyList != null) nearbyList.removeAllViews();
        if (cardNearbyResults != null) cardNearbyResults.setVisibility(View.GONE);
    }

    /** Blue dot = "you are here". */
    private void showMyLocation(double lat, double lng) {
        if (mapViewMain == null) return;
        if (myLocationMarker != null) mapViewMain.getOverlays().remove(myLocationMarker);

        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(Color.parseColor("#2563EB"));
        dot.setStroke(dp(3), Color.WHITE);
        dot.setSize(dp(20), dp(20));

        myLocationMarker = new Marker(mapViewMain);
        myLocationMarker.setPosition(new GeoPoint(lat, lng));
        myLocationMarker.setTitle("You are here");
        myLocationMarker.setIcon(dot);
        myLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        mapViewMain.getOverlays().add(myLocationMarker);
        mapViewMain.invalidate();
    }

    /**
     * Finds the user's position: recent last-known fix, else a fresh single update (8 s timeout),
     * else the centre of the map (so the feature still works on an emulator without GPS).
     */
    private void getUserLocation(final LocationResult callback) {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!fine && !coarse) {
            if (locationDenied) {                       // user already said no: use the map centre
                useMapCentre(callback, null);
                return;
            }
            pendingLocationAction = () -> getUserLocation(callback);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_NEARBY_LOCATION);
            return;
        }

        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location best = null;
        String[] providers = {LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER};
        for (String provider : providers) {
            try {
                Location l = lm.getLastKnownLocation(provider);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            } catch (SecurityException | IllegalArgumentException ignored) { }
        }
        final Location lastKnown = best;

        if (lastKnown != null && System.currentTimeMillis() - lastKnown.getTime() < 10 * 60 * 1000L) {
            callback.onLocation(lastKnown.getLatitude(), lastKnown.getLongitude(), true);
            return;
        }

        String provider = null;
        try {
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) provider = LocationManager.NETWORK_PROVIDER;
            else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) provider = LocationManager.GPS_PROVIDER;
        } catch (Exception ignored) { }
        if (provider == null) {
            useMapCentre(callback, lastKnown);
            return;
        }

        final boolean[] finished = {false};
        final Handler handler = new Handler(Looper.getMainLooper());
        final LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (finished[0]) return;
                finished[0] = true;
                callback.onLocation(location.getLatitude(), location.getLongitude(), true);
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
            @Override public void onProviderEnabled(String provider) { }
            @Override public void onProviderDisabled(String provider) { }
        };

        try {
            lm.requestSingleUpdate(provider, listener, Looper.getMainLooper());
        } catch (SecurityException e) {
            useMapCentre(callback, lastKnown);
            return;
        }
        handler.postDelayed(() -> {
            if (finished[0]) return;
            finished[0] = true;
            try { lm.removeUpdates(listener); } catch (SecurityException ignored) { }
            useMapCentre(callback, lastKnown);
        }, 8000);
    }

    private void useMapCentre(LocationResult callback, Location lastKnown) {
        if (lastKnown != null) {
            callback.onLocation(lastKnown.getLatitude(), lastKnown.getLongitude(), true);
            return;
        }
        org.osmdroid.api.IGeoPoint c = mapViewMain.getMapCenter();
        callback.onLocation(c.getLatitude(), c.getLongitude(), false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_NEARBY_LOCATION) return;

        boolean granted = false;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) granted = true;
        }
        if (!granted) {
            locationDenied = true;
            Toast.makeText(this, "Location permission denied. Using the map centre instead.", Toast.LENGTH_LONG).show();
        }
        Runnable action = pendingLocationAction;
        pendingLocationAction = null;
        if (action != null) action.run();
    }

    // ---------- SOS: the contact depends on the current mode ----------

    private static final String SOS_LAND_NUMBER = "999";    // National Emergency Service (police, fire, ambulance)
    private static final String SOS_SEA_NUMBER  = "16111";  // Bangladesh Coast Guard emergency hotline
    private static final String SOS_AIR_NUMBER  = "999";    // TODO: replace with the official Air Force / ATC emergency number

    private void triggerSos() {
        final String number;
        final String authority;
        if ("SEA".equals(currentMode)) {
            number = SOS_SEA_NUMBER;
            authority = "Bangladesh Coast Guard / Navy";
        } else if ("AIR".equals(currentMode)) {
            number = SOS_AIR_NUMBER;
            authority = "Bangladesh Air Force / Emergency";
        } else {
            number = SOS_LAND_NUMBER;
            authority = "National Emergency Service";
        }

        final String position;
        if (mapViewMain != null) {
            org.osmdroid.api.IGeoPoint c = mapViewMain.getMapCenter();
            position = String.format(java.util.Locale.US, "%.5f, %.5f", c.getLatitude(), c.getLongitude());
        } else {
            position = "unknown";
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🆘 Send SOS")
                .setMessage("Mode: " + currentMode + "\nContact: " + authority + " (" + number + ")\nMap position: " + position)
                .setPositiveButton("Call " + number, (d, w) -> {
                    Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
                    try { startActivity(dial); } catch (Exception e) {
                        Toast.makeText(this, "No dialer app found", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Share position", (d, w) -> {
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    share.putExtra(Intent.EXTRA_TEXT, "SOS from AquaSky (" + currentMode + " mode). Position: "
                            + position + ". Need help urgently.");
                    startActivity(Intent.createChooser(share, "Share SOS position"));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------- Firebase demo: report an issue (Firestore + Realtime Database + MediaStore Photo) ----------

    private void showReportDialog() {
        if (FirebaseAuthHelper.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in first to submit a report.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        final android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), dp(8));

        final android.widget.Spinner spCategory = new android.widget.Spinner(this);
        spCategory.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Road Block", "Accident", "Flooding", "Storm / Rough Sea", "Turbulence", "Other"}));
        final android.widget.EditText etTitle = new android.widget.EditText(this);
        etTitle.setHint("Title (e.g. Waterlogging near Kawla)");
        etTitle.setSingleLine(true);
        final android.widget.EditText etDesc = new android.widget.EditText(this);
        etDesc.setHint("Details");
        etDesc.setMinLines(2);

        // 📷 Attach Photo Button
        final com.google.android.material.button.MaterialButton btnAttachPhoto = new com.google.android.material.button.MaterialButton(this);
        btnAttachPhoto.setText("📷 Attach Evidence Photo (Gallery)");
        btnAttachPhoto.setBackgroundColor(Color.parseColor("#0284C7"));
        btnAttachPhoto.setTextColor(Color.WHITE);
        android.widget.LinearLayout.LayoutParams btnParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, dp(8), 0, dp(6));
        btnAttachPhoto.setLayoutParams(btnParams);

        // Photo Preview ImageView
        final android.widget.ImageView ivPreview = new android.widget.ImageView(this);
        android.widget.LinearLayout.LayoutParams imgParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(140));
        imgParams.setMargins(0, 0, 0, dp(8));
        ivPreview.setLayoutParams(imgParams);
        ivPreview.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        ivPreview.setVisibility(selectedReportImageUri != null ? View.VISIBLE : View.GONE);
        if (selectedReportImageUri != null) {
            ivPreview.setImageURI(selectedReportImageUri);
        }
        ivReportPreviewRef = ivPreview;

        box.addView(spCategory);
        box.addView(etTitle);
        box.addView(etDesc);
        box.addView(btnAttachPhoto);
        box.addView(ivPreview);

        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🚨 Report Incident with Photo Evidence")
                .setView(box)
                .setPositiveButton("Save to Firestore", null)
                .setNeutralButton("Post Live Hazard", null)
                .setNegativeButton("Cancel", null)
                .create();

        btnAttachPhoto.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK);
            pickIntent.setType("image/*");
            startActivityForResult(pickIntent, REQUEST_CODE_PICK_IMAGE);
            dialog.dismiss();
        });

        dialog.show();

        // Our own click handlers so the dialog stays open if validation fails
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) { etTitle.setError("Required"); return; }
            String desc = etDesc.getText().toString().trim();
            String cat = spCategory.getSelectedItem().toString();
            String photoStatus = (selectedReportImageUri != null) ? " [📷 Photo Attached]" : "";

            // Immediately update the Live Hazard Alert Banner on Home Screen!
            setUserAddedHazard(title + photoStatus, desc, cat);

            org.osmdroid.api.IGeoPoint c = mapViewMain.getMapCenter();
            FirestoreRoadReportSync.submitRoadReport(title + photoStatus, cat,
                    c.getLatitude(), c.getLongitude(), desc,
                    new FirestoreRoadReportSync.ReportCallback() {
                        @Override public void onSuccess(String documentId) {
                            Toast.makeText(MainActivity.this, "Saved to Firestore ✔ (" + documentId + ") with Photo", Toast.LENGTH_LONG).show();
                            selectedReportImageUri = null;
                            dialog.dismiss();
                        }
                        @Override public void onFailure(String message) {
                            Toast.makeText(MainActivity.this, "Firestore failed: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) { etTitle.setError("Required"); return; }
            String desc = etDesc.getText().toString().trim();
            String cat = spCategory.getSelectedItem().toString();
            String photoStatus = (selectedReportImageUri != null) ? " [📷 Photo Attached]" : "";

            // Immediately update the Live Hazard Alert Banner on Home Screen!
            setUserAddedHazard(title + photoStatus, desc, cat);

            org.osmdroid.api.IGeoPoint c = mapViewMain.getMapCenter();
            RealtimeHazardSync.postLiveHazard(title, desc, currentMode,
                    c.getLatitude(), c.getLongitude(), "HIGH",
                    new RealtimeHazardSync.WriteCallback() {
                        @Override public void onSuccess() {
                            Toast.makeText(MainActivity.this, "Live hazard posted ✔ (Realtime Database)", Toast.LENGTH_LONG).show();
                            selectedReportImageUri = null;
                            dialog.dismiss();
                        }
                        @Override public void onFailure(String message) {
                            Toast.makeText(MainActivity.this, "Realtime DB failed: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    // =========================================================================
    // DYNAMIC DESTINATION-AWARE & USER-REPORTED LIVE HAZARD ALERTS
    // =========================================================================

    private void setUserAddedHazard(String title, String details, String category) {
        userAddedHazardTitle = "🚨 " + title;
        userAddedHazardDesc = (!details.isEmpty() ? details + " • " : "") + "Category: " + category + " • Reported near active coordinate";
        userAddedHazardFreshness = "Live Community Alert · Just Now";
        displayLiveHazardAlert(userAddedHazardTitle, userAddedHazardDesc, userAddedHazardFreshness);
    }

    private void displayLiveHazardAlert(String title, String desc, String freshness) {
        if (tvAlertTitle != null) tvAlertTitle.setText(title);
        if (tvAlertDescription != null) tvAlertDescription.setText(desc);
        if (tvAlertFreshness != null) tvAlertFreshness.setText(freshness);
    }

    private void updateHazardAlertForDestination(String destination) {
        if (destination == null) destination = "";
        String dLower = destination.toLowerCase(java.util.Locale.US);

        // If user reported a hazard recently, prioritize displaying that user's live hazard!
        if (userAddedHazardTitle != null && !userAddedHazardTitle.isEmpty()) {
            displayLiveHazardAlert(userAddedHazardTitle, userAddedHazardDesc, userAddedHazardFreshness);
            return;
        }

        if ("SEA".equals(currentMode)) {
            if (dLower.contains("rotterdam") || dLower.contains("singapore") || dLower.contains("malacca")) {
                displayLiveHazardAlert(
                        "🌊 High Swell & Gale Warning (Malacca Strait TSS)",
                        "Severe wave heights (4.5m - 5.2m) & storm swells near TSS sector 4. Safe deep-water deviation active.",
                        "Live Sync · IMO Maritime Bureau"
                );
            } else if (dLower.contains("colombo") || dLower.contains("chattogram") || dLower.contains("chittagong")) {
                displayLiveHazardAlert(
                        "🌊 Deep Depression & Squall Advisory (Bay of Bengal)",
                        "Winds gusting up to 45 kts near Outer Anchorage. Coast guard advises draft monitoring.",
                        "Live Sync · Coast Guard & Navy"
                );
            } else {
                displayLiveHazardAlert(
                        "🌊 Global Marine TSS Warning (Open Waters)",
                        "Storm swell warning active across primary international shipping lanes. Detour available.",
                        "Live Sync · Global MetOcean"
                );
            }
        } else if ("AIR".equals(currentMode)) {
            if (dLower.contains("london") || dLower.contains("heathrow") || dLower.contains("fra")) {
                displayLiveHazardAlert(
                        "✈️ Severe Clear-Air Turbulence (Airway L642 · FL370)",
                        "Jetstream shear (>55 kts) & convective turbulence cells detected along trans-continental sector.",
                        "Live Sync · ICAO Aviation Safety"
                );
            } else if (dLower.contains("dubai") || dLower.contains("bangkok") || dLower.contains("sin")) {
                displayLiveHazardAlert(
                        "✈️ High-Altitude Thunderstorm Sigmet (Waypoints MESPO - TEPIL)",
                        "Active storm tops exceeding FL420. Vectoring detour recommended around storm core.",
                        "Live Sync · Regional ATC Centre"
                );
            } else {
                displayLiveHazardAlert(
                        "✈️ Aviation SIGMET Advisory (Cruising Levels)",
                        "Crosswind gusts (>35 kts) and localized wind shear along flight path.",
                        "Live Sync · Bangladesh CAAB"
                );
            }
        } else {
            // Road Navigation - Bangladesh Destinations
            if (dLower.contains("khulna") || dLower.contains("kuet") || dLower.contains("fulbarigate")) {
                displayLiveHazardAlert(
                        "⚠️ Waterlogging & Road Obstruction (Fulbarigate / KUET)",
                        "Flash rain causing road waterlogging near Fulbarigate bypass & KUET gate area. Heavy vehicles moving slowly.",
                        "Live Sync · Khulna Traffic Control"
                );
            } else if (dLower.contains("chattogram") || dLower.contains("chittagong") || dLower.contains("cox")) {
                displayLiveHazardAlert(
                        "⚠️ Landslide Risk & Waterlogging Alert (Sitakunda - N1 Highway)",
                        "Hilly terrain soil instability near Sitakunda. Light vehicle detour suggested via regional bypass.",
                        "Live Sync · Highway Police HQ"
                );
            } else if (dLower.contains("sylhet") || dLower.contains("sunamganj")) {
                displayLiveHazardAlert(
                        "⚠️ Submerged Regional Bridge Alert (Surma Corridor)",
                        "River water overflowed bridge approach on Sylhet access route. Passenger buses restricted.",
                        "Live Sync · Flood Forecasting Division"
                );
            } else if (dLower.contains("padma") || dLower.contains("barisal") || dLower.contains("mongla") || dLower.contains("payra")) {
                displayLiveHazardAlert(
                        "⚠️ High Wind Warning (>45 kts) on Padma Bridge Upper Deck",
                        "High crosswind gusts detected on river span. Two-wheelers advised to reduce speed to 30 km/h.",
                        "Live Sync · Bridge Safety Authority"
                );
            } else {
                displayLiveHazardAlert(
                        "⚠️ Severe Waterlogging Advisory (Airport Kawla Expressway Ramp)",
                        "Heavy rain drainage congestion near Kawla ramp. Elevated expressway upper deck remains fully clear.",
                        "Live Sync · Dhaka Transport Authority"
                );
            }
        }
    }
    // =========================================================================
    private void showOfflineSavedMapsDialog() {
        final String[] offlineTitles;
        final String[] offlineOrigins;
        final String[] offlineDests;

        if ("SEA".equals(currentMode)) {
            offlineTitles = new String[]{
                    "🌊 Bay of Bengal TSS Shipping Lane (Cached · 4.2 MB)",
                    "🌊 Chittagong to Singapore Deep Sea Corridor (Cached · 8.1 MB)",
                    "🌊 Malacca Strait Transit Chart (Cached · 5.5 MB)"
            };
            offlineOrigins = new String[]{
                    "Port of Chattogram (Karnaphuli Channel)",
                    "Port of Singapore (Jurong Deep Sea Channel)",
                    "Port of Singapore (Jurong Deep Sea Channel)"
            };
            offlineDests = new String[]{
                    "Port of Colombo (South Container Terminal)",
                    "Port of Rotterdam (Europort Maasvlakte)",
                    "Port of Dubai (Jebel Ali)"
            };
        } else if ("AIR".equals(currentMode)) {
            offlineTitles = new String[]{
                    "✈️ Dhaka (DAC) to Singapore (SIN) Airway L642 (Cached · 3.8 MB)",
                    "✈️ Dhaka to London Heathrow Trans-Corridor (Cached · 12 MB)",
                    "✈️ Regional FIR & High-Altitude Waypoints (Cached · 6.0 MB)"
            };
            offlineOrigins = new String[]{
                    "Hazrat Shahjalal Intl (DAC Dhaka)",
                    "Hazrat Shahjalal Intl (DAC Dhaka)",
                    "Singapore Changi Airport (SIN)"
            };
            offlineDests = new String[]{
                    "Singapore Changi Airport (SIN)",
                    "London Heathrow Airport (LHR)",
                    "Dubai International Airport (DXB)"
            };
        } else {
            offlineTitles = new String[]{
                    "🚗 Dhaka - Chittagong N1 National Highway (Cached · 9.4 MB)",
                    "🚗 Dhaka - Sylhet N2 Highway Vector Grid (Cached · 7.1 MB)",
                    "🚗 Padma Bridge Connecting Expressway Grid (Cached · 3.2 MB)"
            };
            offlineOrigins = new String[]{
                    "Dhaka Airport (Kawla Ramp - Elevated Expressway)",
                    "Dhaka Airport (Kawla Ramp - Elevated Expressway)",
                    "Dhaka Airport (Kawla Ramp - Elevated Expressway)"
            };
            offlineDests = new String[]{
                    "Chattogram Commercial Area",
                    "Sylhet City Center",
                    "Khulna City Center"
            };
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📦 Offline Saved Maps & Charts")
                .setIcon(android.R.drawable.ic_menu_save)
                .setItems(offlineTitles, (dialog, which) -> {
                    String selectedMap = offlineTitles[which];
                    actvOrigin.setText(offlineOrigins[which]);
                    actvDestination.setText(offlineDests[which]);

                    tvMapLayerTag.setText("📦 " + selectedMap);
                    tvMapTelemetryBadge.setText("📦 Offline Cached");
                    tvMapRouteStatus.setText("Offline Chart Active: " + selectedMap + "\nOperating with zero internet using local SQLite vector tiles.");

                    showCalculatedRouteOnMap();
                    Toast.makeText(this, "Loaded: " + selectedMap + " from SQLite Cache", Toast.LENGTH_LONG).show();
                    scrollToMap();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void handleAuthClick() {
        FirebaseUser user = FirebaseAuthHelper.getCurrentUser();
        if (user != null) {
            FirebaseAuthHelper.signOut();
            Toast.makeText(this, "Signed out successfully.", Toast.LENGTH_SHORT).show();
            refreshUserState();
        } else {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
    }

    private void toggleAppTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDark = prefs.getBoolean(KEY_THEME_DARK, true);
        boolean newTheme = !isDark;
        prefs.edit().putBoolean(KEY_THEME_DARK, newTheme).apply();

        AppCompatDelegate.setDefaultNightMode(newTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void swapEndpoints() {
        String temp = actvOrigin.getText().toString();
        actvOrigin.setText(actvDestination.getText().toString());
        actvDestination.setText(temp);
        updateHazardAlertForDestination(actvDestination.getText().toString());

        renderCurrentDomainAndVehicleOnMap();
    }

    private void applyModeSelection(String mode) {
        this.currentMode = mode;

        btnModeSea.setBackgroundResource(mode.equals("SEA") ? R.drawable.bg_mode_pill_selected : R.drawable.bg_mode_pill_unselected);
        btnModeAir.setBackgroundResource(mode.equals("AIR") ? R.drawable.bg_mode_pill_selected : R.drawable.bg_mode_pill_unselected);
        btnModeRoad.setBackgroundResource(mode.equals("ROAD") ? R.drawable.bg_mode_pill_selected : R.drawable.bg_mode_pill_unselected);

        // Keep text readable on both pill backgrounds (white on selected, light grey on unselected)
        int selectedText = Color.WHITE;
        int unselectedText = Color.parseColor("#CBD5E1");
        btnModeSea.setTextColor(mode.equals("SEA") ? selectedText : unselectedText);
        btnModeAir.setTextColor(mode.equals("AIR") ? selectedText : unselectedText);
        btnModeRoad.setTextColor(mode.equals("ROAD") ? selectedText : unselectedText);

        updateAutocompleteAdapters();

        if ("SEA".equals(mode)) {
            actvOrigin.setText("Port of Singapore (Jurong Deep Sea Channel)");
            actvDestination.setText("Port of Rotterdam (Europort Maasvlakte)");
            tvRiskScoreValue.setText("14 / 100 (Safe)");
            tvWaveHeightValue.setText("2.1 m (Safe Swell)");
            tvTideDraftValue.setText("+16.5 m Deep");
        } else if ("AIR".equals(mode)) {
            actvOrigin.setText("Singapore Changi Airport (SIN)");
            actvDestination.setText("London Heathrow Airport (LHR)");
            tvRiskScoreValue.setText("11 / 100 (Low)");
            tvWaveHeightValue.setText("FL370 Cruise");
            tvTideDraftValue.setText("Jetstream +45 kts");
        } else {
            actvOrigin.setText("Dhaka Airport (Kawla Ramp - Elevated Expressway)");
            actvDestination.setText("Chattogram Commercial Area");
            tvRiskScoreValue.setText("22 / 100 (Low)");
            tvWaveHeightValue.setText("Highways Clear");
            tvTideDraftValue.setText("Toll Verified");
        }

        if ("AIR".equals(mode)) {
            actvOrigin.setText("Singapore Changi Airport (SIN)");
            actvDestination.setText("London Heathrow Airport (LHR)");
            tvRiskScoreValue.setText("11 / 100 (Low)");
            tvWaveHeightValue.setText("FL370 Cruise");
            tvTideDraftValue.setText("Jetstream +45 kts");


            boolean isAirplaneModeOn = android.provider.Settings.Global.getInt(
                    getContentResolver(),
                    android.provider.Settings.Global.AIRPLANE_MODE_ON, 0) != 0;

            if (!isAirplaneModeOn) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("✈️ Flight Safety Mode")
                        .setMessage("You have selected Air Route Navigation. Would you like to enable Airplane Mode for in-flight mode?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Yes, Turn On", (dialog, which) -> {

                            Intent intent = new Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS);
                            try {
                                startActivity(intent);
                            } catch (Exception e) {
                                Toast.makeText(MainActivity.this, "Open Settings to toggle Airplane Mode", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Not Now", null)
                        .show();
            }
        }

        updateHazardAlertForDestination(actvDestination.getText().toString());

        renderCurrentDomainAndVehicleOnMap();
    }

    private void updateAutocompleteAdapters() {
        String[] dataset;
        if ("SEA".equals(currentMode)) {
            dataset = WORLD_SEA_PORTS;
        } else if ("AIR".equals(currentMode)) {
            dataset = WORLD_AIRPORTS;
        } else {
            dataset = WORLD_HIGHWAYS;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, dataset);
        actvOrigin.setAdapter(adapter);
        actvDestination.setAdapter(adapter);
    }

    private void showCalculatedRouteOnMap() {
        if (mapViewMain == null) {
            Toast.makeText(this, "Map is initializing, please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        String originText = actvOrigin.getText().toString().trim();
        String destText = actvDestination.getText().toString().trim();

        updateHazardAlertForDestination(destText);

        if (originText.isEmpty() || destText.isEmpty()) {
            Toast.makeText(this, "Please enter both Origin and Destination", Toast.LENGTH_SHORT).show();
            return;
        }

        clearRouteOverlays();

        GeoPoint startPt = resolveCoordinatesForLocation(originText, true);
        GeoPoint endPt = resolveCoordinatesForLocation(destText, false);

        List<GeoPoint> points = new ArrayList<>();
        points.add(startPt);

        int routeColor;
        int routeWidth;
        String layerName;
        String speedEta;
        String routeNote;

        if ("SEA".equals(currentMode)) {
            double midLat = (startPt.getLatitude() + endPt.getLatitude()) / 2.0;
            double midLng = (startPt.getLongitude() + endPt.getLongitude()) / 2.0;
            points.add(new GeoPoint(midLat - 0.5, midLng + 0.3));
            points.add(endPt);

            routeColor = Color.parseColor("#0284C7");
            routeWidth = 8;
            layerName = "🌊 Deep-Water TSS Sea Lane (+16.5m Draft)";
            speedEta = "21.4 kts · ETA 3.8 Days";
            routeNote = "Marine Transit: " + originText + " ➔ " + destText;

        } else if ("AIR".equals(currentMode)) {
            double midLat = (startPt.getLatitude() + endPt.getLatitude()) / 2.0 + 0.2;
            double midLng = (startPt.getLongitude() + endPt.getLongitude()) / 2.0;
            points.add(new GeoPoint(midLat, midLng));
            points.add(endPt);

            routeColor = Color.parseColor("#8B5CF6");
            routeWidth = 6;
            layerName = "✈️ Controlled Airway Corridor (FL370)";
            speedEta = "480 kts · ETA 1h 45m";
            routeNote = "Aviation Route: " + originText + " ➔ " + destText;

        } else {
            RoadUi ui = buildRoadUi(startPt, endPt, originText, destText);
            points.clear();
            points.addAll(ui.points);
            routeColor = ui.color;
            routeWidth = ui.width;
            layerName = ui.layer;
            speedEta = ui.speedEta;
            routeNote = ui.note;
        }

        addMarker(startPt, "Origin: " + originText, "Departure Point", Color.parseColor("#10B981"));

        addMarker(endPt, "Destination: " + destText, "Arrival Point", Color.parseColor("#EF4444"));

        currentActivePolyline = addRoutePolyline(points, routeColor, routeWidth);

        tvMapLayerTag.setText(layerName);
        tvMapTelemetryBadge.setText(speedEta);
        tvMapRouteStatus.setText(routeNote);

        fitRoute(points, startPt);

        if (mainScrollView != null) {
            scrollToMap();
        }

        Toast.makeText(this, "🗺️ Route displayed on Map: " + originText + " ➔ " + destText, Toast.LENGTH_SHORT).show();
    }

    private GeoPoint resolveCoordinatesForLocation(String locationName, boolean isOrigin) {
        String query = locationName.toLowerCase();

        if (query.contains("chattogram") || query.contains("chittagong")) {
            return query.contains("airport") ? new GeoPoint(22.25, 91.81) : new GeoPoint(22.31, 91.80);
        } else if (query.contains("mongla")) {
            return new GeoPoint(22.49, 89.59);
        } else if (query.contains("payra")) {
            return new GeoPoint(21.99, 90.28);
        } else if (query.contains("matarbari")) {
            return new GeoPoint(21.71, 91.87);
        } else if (query.contains("sadarghat")) {
            return new GeoPoint(23.70, 90.41);
        } else if (query.contains("barisal")) {
            return new GeoPoint(22.70, 90.37);
        } else if (query.contains("chandpur")) {
            return new GeoPoint(23.23, 90.64);
        } else if (query.contains("singapore")) {
            return "AIR".equals(currentMode) ? new GeoPoint(1.36, 103.99) : new GeoPoint(1.29, 103.85);
        } else if (query.contains("rotterdam")) {
            return new GeoPoint(51.95, 4.02);
        } else if (query.contains("colombo")) {
            return new GeoPoint(6.94, 79.84);
        } else if (query.contains("dubai")) {
            return new GeoPoint(25.25, 55.36);
        } else if (query.contains("london") || query.contains("heathrow")) {
            return new GeoPoint(51.47, -0.45);
        } else if (query.contains("bangkok") || query.contains("suvarnabhumi")) {
            return new GeoPoint(13.69, 100.75);
        } else if (query.contains("new york") || query.contains("jfk")) {
            return new GeoPoint(40.64, -73.78);
        } else if (query.contains("shanghai")) {
            return new GeoPoint(30.63, 122.06);
        } else if (query.contains("tokyo") || query.contains("haneda")) {
            return new GeoPoint(35.54, 139.78);
        }

        if (query.contains("airport") || query.contains("kawla")) {
            return new GeoPoint(23.85, 90.40);
        } else if (query.contains("farmgate")) {
            return new GeoPoint(23.75, 90.39);
        } else if (query.contains("motijheel")) {
            return new GeoPoint(23.73, 90.41);
        } else if (query.contains("old dhaka") || query.contains("lalbagh") || query.contains("armanitola")) {
            return new GeoPoint(23.71, 90.39);
        } else if (query.contains("dhanmondi")) {
            return new GeoPoint(23.75, 90.37);
        } else if (query.contains("uttara")) {
            return new GeoPoint(23.87, 90.39);
        } else if (query.contains("gulshan")) {
            return new GeoPoint(23.79, 90.41);
        } else if (query.contains("dhaka")) {
            return new GeoPoint(23.81, 90.41);
        } else if (query.contains("cox") || query.contains("bazar")) {
            return new GeoPoint(21.43, 91.98);
        } else if (query.contains("sylhet")) {
            return new GeoPoint(24.89, 91.87);
        } else if (query.contains("padma")) {
            return new GeoPoint(23.47, 90.26);
        } else if (query.contains("khulna")) {
            return new GeoPoint(22.84, 89.54);
        } else if (query.contains("rajshahi")) {
            return new GeoPoint(24.36, 88.60);
        }

        int hash = Math.abs(locationName.hashCode());
        double offsetLat = ((hash % 100) / 100.0) * 0.6;
        double offsetLng = (((hash / 100) % 100) / 100.0) * 0.6;

        if (isOrigin) {
            return new GeoPoint(23.80 + offsetLat, 90.38 + offsetLng);
        } else {
            return new GeoPoint(22.35 + offsetLat, 91.80 + offsetLng);
        }
    }

    private void applySelectedRouteToMap(String routeId, String routeName, String typeTag,
                                        int risk, int etaMinutes, double dist,
                                        String avoidedHazards, String domain) {
        if (mapViewMain == null) return;

        clearRouteOverlays();

        String originText = actvOrigin.getText().toString().trim();
        String destText = actvDestination.getText().toString().trim();

        updateHazardAlertForDestination(destText);
        GeoPoint startPt = resolveCoordinatesForLocation(originText, true);
        GeoPoint endPt = resolveCoordinatesForLocation(destText, false);

        List<GeoPoint> points = new ArrayList<>();
        points.add(startPt);

        int routeColor;
        int routeWidth = 9;
        String speedEta;

        if (etaMinutes >= 1440) {
            speedEta = String.format("%.1f Days · %.0f NM/KM", (etaMinutes / 1440.0), dist);
        } else if (etaMinutes >= 60) {
            speedEta = String.format("%dh %02dm · %.0f KM", (etaMinutes / 60), (etaMinutes % 60), dist);
        } else {
            speedEta = String.format("%d mins · %.0f KM", etaMinutes, dist);
        }

        if ("r1".equals(routeId) || (typeTag != null && typeTag.contains("SAFEST"))) {
            double midLat = (startPt.getLatitude() + endPt.getLatitude()) / 2.0;
            double midLng = (startPt.getLongitude() + endPt.getLongitude()) / 2.0;
            points.add(new GeoPoint(midLat - 1.2, midLng - 0.8));
            points.add(new GeoPoint(midLat - 0.4, midLng + 0.2));
            points.add(endPt);

            routeColor = Color.parseColor("#10B981");
        } else if ("r2".equals(routeId) || (typeTag != null && typeTag.contains("FASTEST"))) {
            double midLat = (startPt.getLatitude() + endPt.getLatitude()) / 2.0;
            double midLng = (startPt.getLongitude() + endPt.getLongitude()) / 2.0;
            points.add(new GeoPoint(midLat, midLng));
            points.add(endPt);

            routeColor = Color.parseColor("#F59E0B");
        } else if ("r5".equals(routeId) || (typeTag != null && typeTag.contains("ELEVATED"))) {
            double dLat = (endPt.getLatitude() - startPt.getLatitude());
            double dLng = (endPt.getLongitude() - startPt.getLongitude());
            points.add(new GeoPoint(startPt.getLatitude() + dLat * 0.3, startPt.getLongitude() + dLng * 0.25));
            points.add(new GeoPoint(startPt.getLatitude() + dLat * 0.7, startPt.getLongitude() + dLng * 0.65));
            points.add(endPt);

            routeColor = Color.parseColor("#0284C7");
        } else {
            double dLat = (endPt.getLatitude() - startPt.getLatitude());
            double dLng = (endPt.getLongitude() - startPt.getLongitude());
            points.add(new GeoPoint(startPt.getLatitude() + dLat * 0.25, startPt.getLongitude() + dLng * 0.4));
            points.add(new GeoPoint(startPt.getLatitude() + dLat * 0.6, startPt.getLongitude() + dLng * 0.7));
            points.add(endPt);

            routeColor = Color.parseColor("#EF4444");
        }

        addMarker(startPt, "Origin: " + originText, "Departure", Color.parseColor("#10B981"));

        addMarker(endPt, "Destination: " + destText, "Arrival", Color.parseColor("#EF4444"));

        currentActivePolyline = addRoutePolyline(points, routeColor, routeWidth);

        tvMapLayerTag.setText(typeTag + " · " + routeName);
        tvMapTelemetryBadge.setText(speedEta);
        tvMapRouteStatus.setText("Selected: " + routeName + "\n" + (avoidedHazards != null ? avoidedHazards : ""));

        String riskLevel = risk < 25 ? "Safe" : (risk < 50 ? "Moderate" : "High");
        tvRiskScoreValue.setText(risk + " / 100 (" + riskLevel + ")");
        if (risk < 25) {
            tvRiskScoreValue.setTextColor(ContextCompat.getColor(this, R.color.status_safe_emerald));
        } else if (risk < 50) {
            tvRiskScoreValue.setTextColor(ContextCompat.getColor(this, R.color.status_warning_amber));
        } else {
            tvRiskScoreValue.setTextColor(ContextCompat.getColor(this, R.color.status_critical_red));
        }

        fitRoute(points, startPt);

        if (mainScrollView != null) {
            scrollToMap();
        }

        Toast.makeText(this, "✅ Map updated with: " + routeName, Toast.LENGTH_LONG).show();
    }

    private void launchDomainNavigationActivity() {
        if ("AIR".equals(currentMode)) {
            Intent intent = new Intent(this, AirRouteActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, RouteComparisonActivity.class);
            intent.putExtra("EXTRA_DOMAIN", currentMode);
            intent.putExtra("EXTRA_ORIGIN", actvOrigin.getText().toString());
            intent.putExtra("EXTRA_DESTINATION", actvDestination.getText().toString());
            startActivity(intent);
        }
    }

    private void shareHazardAlertImplicit() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "AquaSky Global Emergency Hazard Advisory");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "⚠️ " + tvAlertTitle.getText().toString() + "\n" +
                tvAlertDescription.getText().toString() + "\nLive Tracking via AquaSky Multimodal Engine.");

        startActivity(Intent.createChooser(shareIntent, "Share Global Hazard Via"));
    }

    private void applySmartHazardDetour() {
        String dest = actvDestination != null ? actvDestination.getText().toString().trim() : "";
        String label = dest.isEmpty() ? "active destination" : dest;
        tvAlertDescription.setText("✅ Safe Smart Detour Applied! Bypasses active hazard zone on route to " + label + ".");
        tvRiskScoreValue.setText("10 / 100 (Safe)");
        tvRiskScoreValue.setTextColor(ContextCompat.getColor(this, R.color.status_safe_emerald));
        Toast.makeText(this, "Safe Detour Applied to Navigation Plan", Toast.LENGTH_SHORT).show();
    }

    private void startHazardService() {
        try {
            Intent serviceIntent = new Intent(this, HazardMonitoringService.class);
            serviceIntent.setAction(HazardMonitoringService.ACTION_START_MONITORING);
            startService(serviceIntent);
        } catch (Exception e) {
            Log.w(TAG, "Service start note: " + e.getMessage());
        }
    }

    private void setupFirebaseRealtimeHazards() {
        RealtimeHazardSync.subscribeToLiveHazards(new RealtimeHazardSync.HazardListener() {
            @Override
            public void onHazardsUpdated(List<HazardAlert> hazards) {
                if (hazards != null && !hazards.isEmpty()) {
                    HazardAlert alert = hazards.get(0);
                    for (HazardAlert h : hazards) {          // show the newest hazard
                        if (h.getTimestampMillis() > alert.getTimestampMillis()) alert = h;
                    }
                    if (userAddedHazardTitle == null) {
                        displayLiveHazardAlert(alert.getTitle(), alert.getDescription(), "Live Sync · " + alert.getSourceAgency());
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Realtime Hazard error: " + error);
                Toast.makeText(MainActivity.this, "Realtime DB: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchMarineWeatherViaHttp(double lat, double lng) {
        Call<WeatherApiResponse> call = ApiClient.getWeatherService().getCurrentWeather(lat, lng, true, "temperature_2m,windspeed_10m");
        call.enqueue(new Callback<WeatherApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherApiResponse> call, @NonNull Response<WeatherApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getCurrentWeather() != null) {
                    WeatherApiResponse.CurrentWeather current = response.body().getCurrentWeather();
                    tvWindSpeedValue.setText(String.format("%.1f kts · NE", current.getWindspeed()));
                    tvWaveHeightValue.setText(String.format("%.1f°C · Safe Swell", current.getTemperature()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherApiResponse> call, @NonNull Throwable t) {
                Log.w(TAG, "HTTP Request fallback to sensor: " + t.getMessage());
            }
        });
    }

    private void toggleOfflineSimulation() {
        isOfflineSimulated = !isOfflineSimulated;
        if (isOfflineSimulated) {
            tvNetworkSyncStatus.setText("📦 Offline Mode Active · Local Nav Chart Cached (Last Synced 10m ago)");
            tvNetworkSyncStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warning_amber));
            btnSimulateOfflineToggle.setText("Go Online");
            Toast.makeText(this, "Operating in Offline Mode using Cached Navigation Charts", Toast.LENGTH_SHORT).show();
            // Pre-warm local database cache
            AquaSkyDatabaseHelper.getInstance(this).getOfflineRoutes(currentMode);
        } else {
            tvNetworkSyncStatus.setText("🟢 Live API Connected · Open-Meteo & NOAA");
            tvNetworkSyncStatus.setTextColor(ContextCompat.getColor(this, R.color.status_safe_emerald));
            btnSimulateOfflineToggle.setText("Simulate Offline");
            Toast.makeText(this, "Reconnected to Global Weather & Satellite APIs", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshUserState() {
        final FirebaseUser user = FirebaseAuthHelper.getCurrentUser();
        if (user == null) {
            btnAuthAction.setText(R.string.btn_sign_in);
            tvAppSlogan.setText(R.string.app_tagline);
            return;
        }

        final String email = user.getEmail() != null ? user.getEmail() : "";
        final String shortName = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;

        // show the account immediately, then replace with name + role from Firestore
        btnAuthAction.setText(shortName + " (Sign Out)");
        tvAppSlogan.setText("👤 " + email);

        FirestoreUserProfile.loadProfile(user.getUid(), (name, role) -> {
            if (FirebaseAuthHelper.getCurrentUser() == null) return;   // signed out meanwhile
            String shown = name.isEmpty() ? shortName : name;
            btnAuthAction.setText(shown + " (Sign Out)");
            tvAppSlogan.setText("👤 " + shown + " · " + role + "\n" + email);
        });
    }

    private void updateLifecycleTracker(String status) {
        if (tvLifecycleTracker != null) {
            tvLifecycleTracker.setText(status);
        }
    }

    @Override
    public void onNetworkChanged(boolean isOnline) {
        if (!isOfflineSimulated) {
            tvNetworkSyncStatus.setText(isOnline ? "🟢 Live API Connected · Open-Meteo & NOAA" : "📦 Offline Mode Active · Local Nav Chart Cached");
        }
    }
}
