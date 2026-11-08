package com.aquasky.navigation.network;

import com.google.gson.annotations.SerializedName;

/**
 * POJO for parsing JSON response from Open-Meteo Weather API
 */
public class WeatherApiResponse {

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("current_weather")
    private CurrentWeather currentWeather;

    public static class CurrentWeather {
        @SerializedName("temperature")
        private double temperature;

        @SerializedName("windspeed")
        private double windspeed;

        @SerializedName("winddirection")
        private double winddirection;

        public double getTemperature() { return temperature; }
        public double getWindspeed() { return windspeed; }
        public double getWinddirection() { return winddirection; }
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public CurrentWeather getCurrentWeather() { return currentWeather; }
}
