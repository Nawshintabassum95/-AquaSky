package com.aquasky.navigation.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit Service Interface defining HTTP requests for live marine swells and atmospheric weather.
 */
public interface WeatherApiService {

    @GET("v1/forecast")
    Call<WeatherApiResponse> getCurrentWeather(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("current_weather") boolean currentWeather,
            @Query("hourly") String hourlyParams
    );
}
