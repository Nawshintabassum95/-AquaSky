package com.aquasky.navigation.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aquasky.navigation.R;
import com.aquasky.navigation.models.NavigationRoute;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Modern RecyclerView Adapter demonstrating ViewHolders, ViewBinding, and Click Listeners.
 * Shows Fastest vs. Safest route trade-offs.
 */
public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {

    public interface OnRouteSelectedListener {
        void onRouteSelected(NavigationRoute route);
    }

    private final Context context;
    private final List<NavigationRoute> routeList;
    private final OnRouteSelectedListener listener;

    public RouteAdapter(Context context, List<NavigationRoute> routeList, OnRouteSelectedListener listener) {
        this.context = context;
        this.routeList = routeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_route_card, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        NavigationRoute route = routeList.get(position);

        holder.tvRouteBadge.setText(route.getTypeTag());
        holder.tvRouteName.setText(route.getName());
        holder.tvRouteStats.setText(String.format("⏱️ %.1f NM/KM · %d mins | Channel: %s",
                route.getDistanceKmOrNm(), route.getEstimatedMinutes(), route.getElevationOrChannelType()));
        holder.tvRiskScorePill.setText(String.format("Risk: %d/100 (%s)", route.getModeledRiskScore(), route.getRiskLevel()));
        holder.tvAvoidedHazards.setText(route.getAvoidedHazardsDescription());

        // Dynamic Color Coding based on Risk Score
        if (route.getModeledRiskScore() < 25) {
            holder.tvRiskScorePill.setTextColor(Color.parseColor("#10B981")); // Emerald
        } else if (route.getModeledRiskScore() < 50) {
            holder.tvRiskScorePill.setTextColor(Color.parseColor("#F59E0B")); // Amber
        } else {
            holder.tvRiskScorePill.setTextColor(Color.parseColor("#EF4444")); // Red
        }

        // Click Listener for Route Selection
        holder.btnSelectRoute.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRouteSelected(route);
            } else {
                Toast.makeText(context, "Activated: " + route.getName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return routeList != null ? routeList.size() : 0;
    }

    public static class RouteViewHolder extends RecyclerView.ViewHolder {
        TextView tvRouteBadge;
        TextView tvRiskScorePill;
        TextView tvRouteName;
        TextView tvRouteStats;
        TextView tvAvoidedHazards;
        MaterialButton btnSelectRoute;

        public RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRouteBadge = itemView.findViewById(R.id.tvRouteBadge);
            tvRiskScorePill = itemView.findViewById(R.id.tvRiskScorePill);
            tvRouteName = itemView.findViewById(R.id.tvRouteName);
            tvRouteStats = itemView.findViewById(R.id.tvRouteStats);
            tvAvoidedHazards = itemView.findViewById(R.id.tvAvoidedHazards);
            btnSelectRoute = itemView.findViewById(R.id.btnSelectRoute);
        }
    }
}
