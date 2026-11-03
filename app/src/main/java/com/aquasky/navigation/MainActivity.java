
package com.aquasky.navigation;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    Button btnSea, btnAir, btnLand, btnPlanRoute;

    EditText edtOrigin, edtDestination;

    TextView txtConnection, txtHazard, txtOffline;

    String selectedMode = "SEA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Connect Java with XML layout
        setContentView(R.layout.activity_main);

        // Connect buttons
        btnSea = findViewById(R.id.btnSea);
        btnAir = findViewById(R.id.btnAir);
        btnLand = findViewById(R.id.btnLand);
        btnPlanRoute = findViewById(R.id.btnPlanRoute);

        // Connect input fields
        edtOrigin = findViewById(R.id.edtOrigin);
        edtDestination = findViewById(R.id.edtDestination);

        // Connect status text views
        txtConnection = findViewById(R.id.txtConnection);
        txtHazard = findViewById(R.id.txtHazard);
        txtOffline = findViewById(R.id.txtOffline);

        txtConnection.setText(
                "DEMO MODE | Live data not connected"
        );

        // Default mode
        selectMode("SEA");

        // Sea button
        btnSea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectMode("SEA");
            }
        });

        // Air button
        btnAir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectMode("AIR");
            }
        });

        // Land button
        btnLand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectMode("LAND");
            }
        });

        // Explore route button
        btnPlanRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String origin = edtOrigin.getText()
                        .toString().trim();

                String destination = edtDestination.getText()
                        .toString().trim();

                if (origin.isEmpty()) {
                    edtOrigin.setError("Enter origin");
                    edtOrigin.requestFocus();
                    return;
                }

                if (destination.isEmpty()) {
                    edtDestination.setError("Enter destination");
                    edtDestination.requestFocus();
                    return;
                }

                String message = selectedMode
                        + " mode: " + origin
                        + " -> " + destination
                        + ". Routing will be added next.";

                Toast.makeText(
                        MainActivity.this,
                        message,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    // Update the selected navigation mode
    private void selectMode(String mode) {

        selectedMode = mode;

        if (mode.equals("SEA")) {

            txtHazard.setText(
                    "Marine weather and sea hazards are not "
                            + "connected yet."
            );

            txtOffline.setText(
                    "SEA MODE | Offline marine charts "
                            + "not configured."
            );

        } else if (mode.equals("AIR")) {

            txtHazard.setText(
                    "Aviation weather and flight data are "
                            + "not connected yet."
            );

            txtOffline.setText(
                    "AIR MODE | Downloaded flight data "
                            + "not configured."
            );

        } else {

            txtHazard.setText(
                    "Road closures and local hazard data "
                            + "are not connected yet."
            );

            txtOffline.setText(
                    "LAND MODE | Offline road graph "
                            + "not configured."
            );
        }

        Toast.makeText(
                MainActivity.this,
                mode + " navigation selected",
                Toast.LENGTH_SHORT
        ).show();
    }
}
