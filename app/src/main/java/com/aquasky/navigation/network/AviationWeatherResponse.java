package com.aquasky.navigation.network;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 * AviationWeatherResponse — Model classes for NOAA Aviation Weather Center API responses.
 * Encapsulates SIGMET, METAR, TAF, and PIREP collections.
 */
public class AviationWeatherResponse {

    /**
     * Significant Meteorological Information (SIGMET) Collection
     */
    public static class SigmetCollection {
        @SerializedName("features")
        private List<SigmetItem> items = new ArrayList<>();

        public List<SigmetItem> getItems() {
            return items != null ? items : new ArrayList<>();
        }

        public void setItems(List<SigmetItem> items) {
            this.items = items;
        }
    }

    public static class SigmetItem {
        @SerializedName("hazard")
        private String hazard; // e.g. "CONVECTIVE", "TURBULENCE", "ICING"

        @SerializedName("severity")
        private String severity; // e.g. "SEVERE", "MODERATE"

        @SerializedName("rawText")
        private String rawText;

        @SerializedName("altitudeLow")
        private int altitudeLow;

        @SerializedName("altitudeHigh")
        private int altitudeHigh;

        public String getHazard() { return hazard; }
        public String getSeverity() { return severity; }
        public String getRawText() { return rawText; }
        public int getAltitudeLow() { return altitudeLow; }
        public int getAltitudeHigh() { return altitudeHigh; }
    }

    /**
     * Meteorological Aerodrome Report (METAR) Collection
     */
    public static class MetarCollection {
        @SerializedName("data")
        private List<MetarItem> items = new ArrayList<>();

        public List<MetarItem> getItems() {
            return items != null ? items : new ArrayList<>();
        }

        public void setItems(List<MetarItem> items) {
            this.items = items;
        }
    }

    public static class MetarItem {
        @SerializedName("icaoId")
        private String icaoId;

        @SerializedName("temp")
        private double temperature;

        @SerializedName("wdir")
        private int windDirection;

        @SerializedName("wspd")
        private int windSpeed;

        @SerializedName("visib")
        private double visibility;

        @SerializedName("rawText")
        private String rawText;

        public String getIcaoId() { return icaoId; }
        public double getTemperature() { return temperature; }
        public int getWindDirection() { return windDirection; }
        public int getWindSpeed() { return windSpeed; }
        public double getVisibility() { return visibility; }
        public String getRawText() { return rawText; }
    }

    /**
     * Terminal Aerodrome Forecast (TAF) Collection
     */
    public static class TafCollection {
        @SerializedName("data")
        private List<TafItem> items = new ArrayList<>();

        public List<TafItem> getItems() {
            return items != null ? items : new ArrayList<>();
        }

        public void setItems(List<TafItem> items) {
            this.items = items;
        }
    }

    public static class TafItem {
        @SerializedName("icaoId")
        private String icaoId;

        @SerializedName("rawText")
        private String rawText;

        @SerializedName("validTimeFrom")
        private String validTimeFrom;

        @SerializedName("validTimeTo")
        private String validTimeTo;

        public String getIcaoId() { return icaoId; }
        public String getRawText() { return rawText; }
        public String getValidTimeFrom() { return validTimeFrom; }
        public String getValidTimeTo() { return validTimeTo; }
    }

    /**
     * Pilot Reports (PIREP) Collection
     */
    public static class PirepCollection {
        @SerializedName("data")
        private List<PirepItem> items = new ArrayList<>();

        public List<PirepItem> getItems() {
            return items != null ? items : new ArrayList<>();
        }

        public void setItems(List<PirepItem> items) {
            this.items = items;
        }
    }

    public static class PirepItem {
        @SerializedName("aircraftType")
        private String aircraftType;

        @SerializedName("altitude")
        private int altitude;

        @SerializedName("turbSeverity")
        private String turbSeverity;

        @SerializedName("iceSeverity")
        private String iceSeverity;

        @SerializedName("rawText")
        private String rawText;

        public String getAircraftType() { return aircraftType; }
        public int getAltitude() { return altitude; }
        public String getTurbSeverity() { return turbSeverity; }
        public String getIceSeverity() { return iceSeverity; }
        public String getRawText() { return rawText; }
    }
}
