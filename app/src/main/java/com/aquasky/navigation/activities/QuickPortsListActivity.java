package com.aquasky.navigation.activities;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aquasky.navigation.R;
import com.aquasky.navigation.adapters.PortListViewAdapter;
import com.aquasky.navigation.models.PortWaypoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates classic Android ListView architecture with custom BaseAdapter & ViewHolder recycling.
 */
public class QuickPortsListActivity extends AppCompatActivity {

    private ListView listViewPorts;
    private PortListViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_ports_list);

        listViewPorts = findViewById(R.id.listViewPorts);

        List<PortWaypoint> ports = getSamplePorts();
        adapter = new PortListViewAdapter(this, ports);
        listViewPorts.setAdapter(adapter);

        // AdapterView.OnItemClickListener demonstration
        listViewPorts.setOnItemClickListener((parent, view, position, id) -> {
            PortWaypoint selected = ports.get(position);
            Toast.makeText(this, "Selected Station: " + selected.getName(), Toast.LENGTH_SHORT).show();
        });
    }

    private List<PortWaypoint> getSamplePorts() {
        List<PortWaypoint> list = new ArrayList<>();
        list.add(new PortWaypoint("p1", "Port of Singapore (Jurong Deep Sea Channel)", "SEA", 1.29, 103.85, 16.5, "International Transshipment Mega-Hub"));
        list.add(new PortWaypoint("p2", "Port of Rotterdam (Europort Maasvlakte)", "SEA", 51.95, 4.02, 24.0, "Gateway to Europe · Deepest Basin"));
        list.add(new PortWaypoint("p3", "Port of Shanghai (Yangshan Deep Water)", "SEA", 30.63, 122.06, 17.5, "World's Busiest Automated Container Terminal"));
        list.add(new PortWaypoint("p4", "Port of Chattogram (Karnaphuli Channel)", "SEA", 22.31, 91.80, 9.5, "Bay of Bengal Commercial Gateway"));
        list.add(new PortWaypoint("p5", "Singapore Changi Airport (SIN)", "AIR", 1.36, 103.99, 0, "Runways 02L/20R · World Best Aviation Hub"));
        list.add(new PortWaypoint("p6", "London Heathrow Airport (LHR)", "AIR", 51.47, -0.45, 0, "Trans-Atlantic & Eurasian Gateway"));
        list.add(new PortWaypoint("p7", "New York John F. Kennedy (JFK)", "AIR", 40.64, -73.78, 0, "Major International North American Gateway"));
        return list;
    }
}
