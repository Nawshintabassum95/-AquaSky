package com.aquasky.navigation.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aquasky.navigation.R;
import com.google.android.material.button.MaterialButton;

/**
 * AquaSky Aviation Corridor Activity
 * Demonstrates RelativeLayout, aviation waypoints, stepdown altitude profile, and storm avoidance.
 */
public class AirRouteActivity extends AppCompatActivity {

    private TextView tvFlightCallsign, tvAirwayRoute, tvFlightTelemetry, tvAviationWeatherRisk, tvWaypointSummary;
    private MaterialButton btnSimulateLiveTracking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_air_route);

        tvFlightCallsign = findViewById(R.id.tvFlightCallsign);
        tvAirwayRoute = findViewById(R.id.tvAirwayRoute);
        tvFlightTelemetry = findViewById(R.id.tvFlightTelemetry);
        tvAviationWeatherRisk = findViewById(R.id.tvAviationWeatherRisk);
        tvWaypointSummary = findViewById(R.id.tvWaypointSummary);
        btnSimulateLiveTracking = findViewById(R.id.btnSimulateLiveTracking);

        btnSimulateLiveTracking.setOnClickListener(v -> {
            Toast.makeText(this, "Simulating Trans-Oceanic GPS Track: Altitude FL370, Ground Speed 510 kts", Toast.LENGTH_SHORT).show();
            tvFlightTelemetry.setText("Altitude: FL370 | Ground Speed: 510 kts (Tailwind +22 kts)");
        });
    }
}
