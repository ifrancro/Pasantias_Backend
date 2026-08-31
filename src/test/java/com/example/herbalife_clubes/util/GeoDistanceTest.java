package com.example.herbalife_clubes.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoDistanceTest {

    private static final double CLUB_LAT = -17.3935;
    private static final double CLUB_LNG = -66.1570;

    @Test
    void mismaCoordenadaDistanciaCero() {
        assertEquals(0.0, GeoDistance.distanceMeters(CLUB_LAT, CLUB_LNG, CLUB_LAT, CLUB_LNG), 0.001);
    }

    @Test
    void distanciaSimetrica() {
        double offsetLat = 0.0009;
        double d1 = GeoDistance.distanceMeters(CLUB_LAT, CLUB_LNG, CLUB_LAT + offsetLat, CLUB_LNG);
        double d2 = GeoDistance.distanceMeters(CLUB_LAT + offsetLat, CLUB_LNG, CLUB_LAT, CLUB_LNG);
        assertEquals(d1, d2, 0.01);
        assertTrue(d1 > 90 && d1 < 110);
    }
}
