package com.aquasky.navigation.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * AviationApiService — Retrofit interface for live aviation weather data.
 *
 * Data sources used (all free, no API key required):
 *   1. NOAA Aviation Weather Center — https://aviationweather.gov/api/data/
 *      - SIGMET  : Significant Meteorological Information (thunderstorms, CAT zones, etc.)
 *      - METAR   : Meteorological Aerodrome Reports (surface wx at airports)
 *      - TAF     : Terminal Aerodrome Forecasts
 *      - PIREP   : Pilot Reports (turbulence, icing, etc.)
 *
 *   2. These endpoints are queried by AirRouteActivity for live aviation hazard overlays.
 *
 * Base URL is configured in AviationApiClient: https://aviationweather.gov/api/data/
 */
public interface AviationApiService {

    /**
     * Fetch active SIGMETs (Significant Meteorological Information).
     * Returns active hazard areas — thunderstorm cells, CAT, volcanic ash, tropical cyclones.
     *
     * Example URL:
     *   GET https://aviationweather.gov/api/data/sigmet?format=json
     *
     * @param format  "json" for JSON response (default: xml)
     * @param hazard  Optional filter e.g. "CONVECTIVE", "TURBULENCE", "ICING", "VA" (volcanic ash)
     * @return        Call wrapping AviationSigmetResponse
     */
    @GET("sigmet")
    Call<AviationWeatherResponse.SigmetCollection> getActiveSigmets(
            @Query("format") String format,
            @Query("hazard") String hazard
    );

    /**
     * Fetch METAR reports for one or more airports by ICAO code.
     * METAR = Meteorological Aerodrome Report — official wx observation at airport.
     *
     * Example URL:
     *   GET https://aviationweather.gov/api/data/metar?ids=WSSS,EGLL,OMDB&format=json
     *
     * @param ids     Comma-separated ICAO station codes e.g. "WSSS,EGLL,OMDB"
     * @param format  "json"
     * @return        Call wrapping list of METAR observations
     */
    @GET("metar")
    Call<AviationWeatherResponse.MetarCollection> getMetarReports(
            @Query("ids") String ids,
            @Query("format") String format
    );

    /**
     * Fetch TAF (Terminal Aerodrome Forecast) for given airport(s).
     * TAF = 24–30 hour forecast valid for a 5NM radius around the airport.
     *
     * Example URL:
     *   GET https://aviationweather.gov/api/data/taf?ids=WSSS,EGLL&format=json
     *
     * @param ids     Comma-separated ICAO codes
     * @param format  "json"
     * @return        Call wrapping list of TAF forecasts
     */
    @GET("taf")
    Call<AviationWeatherResponse.TafCollection> getTafForecasts(
            @Query("ids") String ids,
            @Query("format") String format
    );

    /**
     * Fetch PIREP (Pilot Reports) for a geographic bounding box or altitude band.
     * PIREPs report turbulence, icing, and weather encountered by actual flight crew.
     *
     * Example URL:
     *   GET https://aviationweather.gov/api/data/pirep?format=json&age=2
     *
     * @param format  "json"
     * @param ageHours  Max age of reports in hours (1–24)
     * @return        Call wrapping list of PIREP reports
     */
    @GET("pirep")
    Call<AviationWeatherResponse.PirepCollection> getActivePireps(
            @Query("format") String format,
            @Query("age") int ageHours
    );
}
