package com.iqspark.underwriter.geo;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * The geographic eligibility / remoteness screen. Computes great-circle (haversine) distance from
 * the property to a built-in table of major Canadian cities. A property is flagged <em>remote</em>
 * only if it is &gt;100 km from <em>every</em> city — genuinely remote, not just far from one.
 *
 * <p>The 100 km rule is the vacant-home module's instance of this generic screen. If coordinates
 * are absent it falls back to a province centroid; if neither resolves it returns "unresolved" so
 * the rule refers the file for manual geo review rather than passing it silently.
 */
@Service
public class GeoService {

    public static final double REMOTE_THRESHOLD_KM = 100.0;
    private static final double EARTH_RADIUS_KM = 6371.0;

    /** A major city used as a reference point for the remoteness screen. */
    public record City(String name, String province, double latitude, double longitude) {}

    /** The outcome of the remoteness screen. */
    public record RemotenessResult(boolean resolved, boolean remote,
                                   double nearestDistanceKm, String nearestCity) {
        public static RemotenessResult unresolved() {
            return new RemotenessResult(false, false, Double.NaN, null);
        }
    }

    private static final List<City> MAJOR_CITIES = List.of(
            new City("Toronto", "ON", 43.6532, -79.3832),
            new City("Ottawa", "ON", 45.4215, -75.6972),
            new City("Hamilton", "ON", 43.2557, -79.8711),
            new City("London", "ON", 42.9849, -81.2453),
            new City("Windsor", "ON", 42.3149, -83.0364),
            new City("Montreal", "QC", 45.5019, -73.5674),
            new City("Quebec City", "QC", 46.8139, -71.2080),
            new City("Vancouver", "BC", 49.2827, -123.1207),
            new City("Victoria", "BC", 48.4284, -123.3656),
            new City("Calgary", "AB", 51.0447, -114.0719),
            new City("Edmonton", "AB", 53.5461, -113.4938),
            new City("Winnipeg", "MB", 49.8951, -97.1384),
            new City("Saskatoon", "SK", 52.1332, -106.6700),
            new City("Regina", "SK", 50.4452, -104.6189),
            new City("Halifax", "NS", 44.6488, -63.5752),
            new City("Moncton", "NB", 46.0878, -64.7782),
            new City("St. John's", "NL", 47.5615, -52.7126));

    private static final Map<String, double[]> PROVINCE_CENTROIDS = Map.ofEntries(
            Map.entry("ON", new double[]{50.0000, -85.0000}),
            Map.entry("QC", new double[]{52.0000, -72.0000}),
            Map.entry("BC", new double[]{53.7267, -127.6476}),
            Map.entry("AB", new double[]{53.9333, -116.5765}),
            Map.entry("MB", new double[]{53.7609, -98.8139}),
            Map.entry("SK", new double[]{52.9399, -106.4509}),
            Map.entry("NS", new double[]{45.0000, -63.0000}),
            Map.entry("NB", new double[]{46.5653, -66.4619}),
            Map.entry("NL", new double[]{53.1355, -57.6604}),
            Map.entry("PE", new double[]{46.5107, -63.4168}),
            Map.entry("YT", new double[]{64.2823, -135.0000}),
            Map.entry("NT", new double[]{64.8255, -124.8457}),
            Map.entry("NU", new double[]{70.2998, -83.1076}));

    /**
     * Run the screen. Uses coordinates when present, else the province centroid, else returns
     * unresolved.
     */
    public RemotenessResult assess(Double latitude, Double longitude, String province) {
        Double lat = latitude;
        Double lon = longitude;
        if (lat == null || lon == null) {
            double[] centroid = province == null ? null : PROVINCE_CENTROIDS.get(province.trim().toUpperCase());
            if (centroid == null) {
                return RemotenessResult.unresolved();
            }
            lat = centroid[0];
            lon = centroid[1];
        }

        double nearest = Double.POSITIVE_INFINITY;
        String nearestCity = null;
        for (City c : MAJOR_CITIES) {
            double d = haversineKm(lat, lon, c.latitude(), c.longitude());
            if (d < nearest) {
                nearest = d;
                nearestCity = c.name();
            }
        }
        boolean remote = nearest > REMOTE_THRESHOLD_KM;
        return new RemotenessResult(true, remote, Math.round(nearest * 10.0) / 10.0, nearestCity);
    }

    public double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
