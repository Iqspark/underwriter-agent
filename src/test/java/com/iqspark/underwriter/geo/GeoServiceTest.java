package com.iqspark.underwriter.geo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeoServiceTest {

    private final GeoService geo = new GeoService();

    @Test
    void torontoIsWithinRange() {
        GeoService.RemotenessResult r = geo.assess(43.6532, -79.3832, "ON");
        assertThat(r.resolved()).isTrue();
        assertThat(r.remote()).isFalse();
        assertThat(r.nearestDistanceKm()).isLessThan(GeoService.REMOTE_THRESHOLD_KM);
    }

    @Test
    void farNorthIsRemote() {
        GeoService.RemotenessResult r = geo.assess(60.0, -100.0, "MB");
        assertThat(r.resolved()).isTrue();
        assertThat(r.remote()).isTrue();
        assertThat(r.nearestDistanceKm()).isGreaterThan(GeoService.REMOTE_THRESHOLD_KM);
    }

    @Test
    void fallsBackToProvinceCentroidWhenNoCoordinates() {
        GeoService.RemotenessResult r = geo.assess(null, null, "ON");
        assertThat(r.resolved()).isTrue();
    }

    @Test
    void unresolvedWhenNeitherCoordinatesNorProvince() {
        assertThat(geo.assess(null, null, null).resolved()).isFalse();
        assertThat(geo.assess(null, null, "ZZ").resolved()).isFalse();
    }

    @Test
    void haversineMatchesKnownDistance() {
        // Toronto -> Montreal is ~504 km.
        double d = geo.haversineKm(43.6532, -79.3832, 45.5019, -73.5674);
        assertThat(d).isBetween(450.0, 560.0);
    }
}
