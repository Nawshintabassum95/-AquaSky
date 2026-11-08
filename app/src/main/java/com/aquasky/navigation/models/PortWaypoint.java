package com.aquasky.navigation.models;

import java.io.Serializable;

/**
 * Model representing a Global Marine Port, Airport Waypoint, or Highway Hub
 */
public class PortWaypoint implements Serializable {
    private String id;
    private String name;
    private String category; // "SEA", "AIR", "ROAD"
    private double latitude;
    private double longitude;
    private double maxDraftMeters;
    private String details;

    public PortWaypoint(String id, String name, String category, double latitude, double longitude, double maxDraftMeters, String details) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.maxDraftMeters = maxDraftMeters;
        this.details = details;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getMaxDraftMeters() { return maxDraftMeters; }
    public String getDetails() { return details; }

    @Override
    public String toString() {
        return name;
    }
}
