package com.itercraft.api.application.meteo;

public interface MeteoService {

    byte[] getMapImage(String layer, double lat, double lon, int width, int height);
}
