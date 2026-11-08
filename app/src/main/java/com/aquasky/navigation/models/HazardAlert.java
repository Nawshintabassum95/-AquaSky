package com.aquasky.navigation.models;

import java.io.Serializable;

/**
 * Model representing live weather, marine cyclone, or road flooding hazard.
 */
public class HazardAlert implements Serializable {
    private String id;
    private String title;
    private String description;
    private String severity; // "CRITICAL", "HIGH", "MODERATE"
    private String domain;   // "SEA", "AIR", "ROAD"
    private double latitude;
    private double longitude;
    private String sourceAgency;
    private long timestampMillis;

    public HazardAlert() {
        // Required for serialization
    }

    public HazardAlert(String id, String title, String description, String severity, String domain,
                       double latitude, double longitude, String sourceAgency, long timestampMillis) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.domain = domain;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sourceAgency = sourceAgency;
        this.timestampMillis = timestampMillis;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getSeverity() { return severity; }
    public String getDomain() { return domain; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getSourceAgency() { return sourceAgency; }
    public long getTimestampMillis() { return timestampMillis; }

    // Setters: Firebase Realtime Database needs them to rebuild the object from JSON
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setDomain(String domain) { this.domain = domain; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setSourceAgency(String sourceAgency) { this.sourceAgency = sourceAgency; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }
}
