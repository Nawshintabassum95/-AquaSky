package com.aquasky.navigation.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.aquasky.navigation.R;
import com.aquasky.navigation.models.PortWaypoint;

import java.util.List;

/**
 * Classic Android ListView Adapter extending BaseAdapter.
 * Demonstrates the classic View recycling pattern with static ViewHolder.
 */
public class PortListViewAdapter extends BaseAdapter {

    private final Context context;
    private final List<PortWaypoint> portList;
    private final LayoutInflater inflater;

    public PortListViewAdapter(Context context, List<PortWaypoint> portList) {
        this.context = context;
        this.portList = portList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return portList != null ? portList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return portList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // Classic Android ViewHolder for ListView
    static class ViewHolder {
        TextView tvPortIcon;
        TextView tvPortName;
        TextView tvPortDetails;
        TextView tvPortCoordinates;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_port_row, parent, false);
            holder = new ViewHolder();
            holder.tvPortIcon = convertView.findViewById(R.id.tvPortIcon);
            holder.tvPortName = convertView.findViewById(R.id.tvPortName);
            holder.tvPortDetails = convertView.findViewById(R.id.tvPortDetails);
            holder.tvPortCoordinates = convertView.findViewById(R.id.tvPortCoordinates);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        PortWaypoint port = portList.get(position);
        holder.tvPortName.setText(port.getName());
        holder.tvPortDetails.setText(port.getDetails() + " · Draft: " + port.getMaxDraftMeters() + "m");
        holder.tvPortCoordinates.setText(String.format("%.2f°N, %.2f°E", port.getLatitude(), port.getLongitude()));

        if ("SEA".equals(port.getCategory())) {
            holder.tvPortIcon.setText("⚓");
        } else if ("AIR".equals(port.getCategory())) {
            holder.tvPortIcon.setText("✈️");
        } else {
            holder.tvPortIcon.setText("🚗");
        }

        return convertView;
    }
}
