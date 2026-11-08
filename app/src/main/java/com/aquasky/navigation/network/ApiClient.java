package com.aquasky.navigation.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Android HTTP Networking Client via Retrofit 2 & OkHttp.
 */
public class ApiClient {

    private static final String BASE_URL_WEATHER = "https://api.open-meteo.com/";
    private static final String BASE_URL_AVIATION = "https://aviationweather.gov/api/data/";
    
    private static Retrofit weatherRetrofit = null;
    private static Retrofit aviationRetrofit = null;

    private static OkHttpClient createHttpClient() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .build();
    }

    public static Retrofit getWeatherClient() {
        if (weatherRetrofit == null) {
            weatherRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL_WEATHER)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(createHttpClient())
                    .build();
        }
        return weatherRetrofit;
    }

    public static Retrofit getAviationClient() {
        if (aviationRetrofit == null) {
            aviationRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL_AVIATION)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(createHttpClient())
                    .build();
        }
        return aviationRetrofit;
    }

    public static WeatherApiService getWeatherService() {
        return getWeatherClient().create(WeatherApiService.class);
    }

    public static AviationApiService getAviationService() {
        return getAviationClient().create(AviationApiService.class);
    }
}
