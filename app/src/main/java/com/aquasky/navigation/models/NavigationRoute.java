package com.aquasky.navigation.models;

import java.io.Serializable;

/**
 * Model representing a calculated route option (Fastest vs Safest)
 */
public class NavigationRoute implements Serializable {
    private String id;
    private String name;
    private String typeTag; // "FASTEST ROUTE", "SAFEST ROUTE", "RECOMMENDED"
    private double distanceKmOrNm;
    private int estimatedMinutes;
    private int modeledRiskScore; // 0 to 100
    private String riskLevel;     // "Low", "Moderate", "High"
    private String avoidedHazardsDescription;
    private String elevationOrChannelType;

    public NavigationRoute(String id, String name, String typeTag, double distanceKmOrNm,
                           int estimatedMinutes, int modeledRiskScore, String riskLevel,
                           String avoidedHazardsDescription, String elevationOrChannelType) {
        this.id = id;
        this.name = name;
        this.typeTag = typeTag;
        this.distanceKmOrNm = distanceKmOrNm;
        this.estimatedMinutes = estimatedMinutes;
        this.modeledRiskScore = modeledRiskScore;
        this.riskLevel = riskLevel;
        this.avoidedHazardsDescription = avoidedHazardsDescription;
        this.elevationOrChannelType = elevationOrChannelType;
    }

    public NavigationRoute(String id, String name, String domain, double distanceKmOrNm,
                           int estimatedMinutes, int modeledRiskScore, boolean isOffline,
                           String avoidedHazardsDescription) {
        this.id = id;
        this.name = name;
        this.typeTag = isOffline ? "OFFLINE CACHED" : "RECOMMENDED ROUTE";
        this.distanceKmOrNm = distanceKmOrNm;
        this.estimatedMinutes = estimatedMinutes;
        this.modeledRiskScore = modeledRiskScore;
        this.riskLevel = modeledRiskScore < 25 ? "Low" : (modeledRiskScore < 50 ? "Moderate" : "High");
        this.avoidedHazardsDescription = avoidedHazardsDescription;
        this.elevationOrChannelType = domain != null ? domain : "Standard";
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getTypeTag() { return typeTag; }
    public double getDistanceKmOrNm() { return distanceKmOrNm; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public int getModeledRiskScore() { return modeledRiskScore; }
    public String getRiskLevel() { return riskLevel; }
    public String getAvoidedHazardsDescription() { return avoidedHazardsDescription; }
    public String getElevationOrChannelType() { return elevationOrChannelType; }
}
