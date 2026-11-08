package com.aquasky.navigation.models;

import java.io.Serializable;

/**
 * Model defining Vehicle-wise constraints: speed, width, height, alley access, tolls
 */
public class VehicleProfile implements Serializable {
    public enum Type {
        CAR_SUV,
        TRANSIT_BUS,
        CARGO_TRUCK,
        MOTORCYCLE,
        CNG_AUTO
    }

    private Type type;
    private String name;
    private double maxSpeedKmH;
    private double maxWidthMeters;
    private double maxHeightMeters;
    private boolean flyoverAllowed;
    private boolean narrowAlleyAllowed;
    private int baseTollBdt;

    public VehicleProfile(Type type, String name, double maxSpeedKmH, double maxWidthMeters,
                          double maxHeightMeters, boolean flyoverAllowed, boolean narrowAlleyAllowed, int baseTollBdt) {
        this.type = type;
        this.name = name;
        this.maxSpeedKmH = maxSpeedKmH;
        this.maxWidthMeters = maxWidthMeters;
        this.maxHeightMeters = maxHeightMeters;
        this.flyoverAllowed = flyoverAllowed;
        this.narrowAlleyAllowed = narrowAlleyAllowed;
        this.baseTollBdt = baseTollBdt;
    }

    public Type getType() { return type; }
    public String getName() { return name; }
    public double getMaxSpeedKmH() { return maxSpeedKmH; }
    public double getMaxWidthMeters() { return maxWidthMeters; }
    public double getMaxHeightMeters() { return maxHeightMeters; }
    public boolean isFlyoverAllowed() { return flyoverAllowed; }
    public boolean isNarrowAlleyAllowed() { return narrowAlleyAllowed; }
    public int getBaseTollBdt() { return baseTollBdt; }
}
