package com.aquasky.navigation.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aquasky.navigation.R;
import com.aquasky.navigation.adapters.RouteAdapter;
import com.aquasky.navigation.database.AquaSkyDatabaseHelper;
import com.aquasky.navigation.models.NavigationRoute;

import java.util.ArrayList;
import java.util.List;

/**
 * Fastest vs Safest Worldwide Route Comparison Activity
 * Demonstrates RecyclerView, LayoutManager, RouteAdapter, and Offline SQLite caching.
 */
public class RouteComparisonActivity extends AppCompatActivity {

    private RecyclerView recyclerViewRoutes;
    private TextView tvComparisonHeader, tvTradeoffExplanation;
    private RouteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_comparison);

        tvComparisonHeader = findViewById(R.id.tvComparisonHeader);
        tvTradeoffExplanation = findViewById(R.id.tvTradeoffExplanation);
        recyclerViewRoutes = findViewById(R.id.recyclerViewRoutes);

        String domain = getIntent().getStringExtra("EXTRA_DOMAIN");
        if (domain == null) domain = "SEA";

        recyclerViewRoutes.setLayoutManager(new LinearLayoutManager(this));

        List<NavigationRoute> routes = generateRoutesForDomain(domain);
        
        // Also query offline SQLite database to ensure offline availability
        try {
            List<NavigationRoute> offlineRoutes = AquaSkyDatabaseHelper.getInstance(this).getOfflineRoutes(domain);
            if (offlineRoutes != null && !offlineRoutes.isEmpty() && routes.isEmpty()) {
                routes.addAll(offlineRoutes);
            }
        } catch (Exception ignored) {}

        adapter = new RouteAdapter(this, routes, route -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("SELECTED_ROUTE_ID", route.getId());
            resultIntent.putExtra("SELECTED_ROUTE_NAME", route.getName());
            resultIntent.putExtra("SELECTED_ROUTE_TYPE", route.getTypeTag());
            resultIntent.putExtra("SELECTED_ROUTE_RISK", route.getModeledRiskScore());
            resultIntent.putExtra("SELECTED_ROUTE_ETA", route.getEstimatedMinutes());
            resultIntent.putExtra("SELECTED_ROUTE_DIST", route.getDistanceKmOrNm());
            resultIntent.putExtra("SELECTED_ROUTE_AVOIDED", route.getAvoidedHazardsDescription());
            //resultIntent.putExtra("SELECTED_ROUTE_DOMAIN", domain);
            setResult(RESULT_OK, resultIntent);

            Toast.makeText(this, "Selected: " + route.getName(), Toast.LENGTH_SHORT).show();
            finish();
        });
        recyclerViewRoutes.setAdapter(adapter);
    }

    private List<NavigationRoute> generateRoutesForDomain(String domain) {
        List<NavigationRoute> list = new ArrayList<>();
        if ("SEA".equals(domain)) {
            list.add(new NavigationRoute(
                    "r1",
                    "Deep-Water TSS Western Deviation (Recommended)",
                    "SAFEST ROUTE",
                    8420.0,
                    24120, // ~16.7 days
                    14,
                    "Low",
                    "🛡️ Avoids 4.8m cyclone swell in Bay of Bengal & shallow sandbanks.",
                    "Deep-Sea TSS Lane"
            ));
            list.add(new NavigationRoute(
                    "r2",
                    "Direct Rhumb-Line Channel",
                    "FASTEST ROUTE",
                    8150.0,
                    23340,
                    65,
                    "High",
                    "⚠️ Exposed to Force 8 gale winds near Malacca & Sunda narrows.",
                    "Direct Ocean Transit"
            ));
        } else if ("AIR".equals(domain)) {
            list.add(new NavigationRoute(
                    "r3",
                    "Airway L642 via Central Asia Corridor",
                    "SAFEST ROUTE",
                    5940.0,
                    780,
                    11,
                    "Low",
                    "🛡️ Grants 30 NM buffer around convective thunderstorms.",
                    "FL370 Controlled Airway"
            ));
            list.add(new NavigationRoute(
                    "r4",
                    "Direct Great Circle Polar Track",
                    "FASTEST ROUTE",
                    5820.0,
                    745,
                    52,
                    "Moderate",
                    "⚠️ High clear-air turbulence (CAT) and jetstream headwind shear.",
                    "Direct Great Circle"
            ));
        } else {
            list.add(new NavigationRoute(
                    "r5",
                    "Trans-Continental Elevated Expressway Link",
                    "FASTEST & SAFEST",
                    310.0,
                    210,
                    15,
                    "Low",
                    "🛡️ High-speed multilane highway with zero ground intersections.",
                    "Elevated Highway"
            ));
            list.add(new NavigationRoute(
                    "r6",
                    "Secondary Arterial Surface Road",
                    "GROUND ARTERIAL",
                    305.0,
                    360,
                    58,
                    "High",
                    "⚠️ High urban congestion and bridge weight restrictions.",
                    "Surface Highway"
            ));
        }
        return list;
    }
}
