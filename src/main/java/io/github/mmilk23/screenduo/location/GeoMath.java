package io.github.mmilk23.screenduo.location;

public final class GeoMath {

    private static final double EARTH_RADIUS_KM = 6_371.0088;

    private GeoMath() {
    }

    public static BoundingBox boundingBox(GeoPoint center, double radiusKm) {
        requireRadius(radiusKm);
        double angularDistance = radiusKm / EARTH_RADIUS_KM;
        double latitude = Math.toRadians(center.latitude());
        double longitudeDelta = Math.asin(Math.min(1.0,
                Math.sin(angularDistance) / Math.max(0.000001, Math.cos(latitude))));

        return new BoundingBox(
                Math.max(-90.0, center.latitude() - Math.toDegrees(angularDistance)),
                Math.max(-180.0, center.longitude() - Math.toDegrees(longitudeDelta)),
                Math.min(90.0, center.latitude() + Math.toDegrees(angularDistance)),
                Math.min(180.0, center.longitude() + Math.toDegrees(longitudeDelta)));
    }

    public static double distanceKm(GeoPoint from, GeoPoint to) {
        double latitudeDelta = Math.toRadians(to.latitude() - from.latitude());
        double longitudeDelta = Math.toRadians(to.longitude() - from.longitude());
        double fromLatitude = Math.toRadians(from.latitude());
        double toLatitude = Math.toRadians(to.latitude());
        double haversine = Math.pow(Math.sin(latitudeDelta / 2.0), 2)
                + Math.cos(fromLatitude) * Math.cos(toLatitude)
                * Math.pow(Math.sin(longitudeDelta / 2.0), 2);
        return 2.0 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(haversine));
    }

    public static double bearingDegrees(GeoPoint from, GeoPoint to) {
        double fromLatitude = Math.toRadians(from.latitude());
        double toLatitude = Math.toRadians(to.latitude());
        double longitudeDelta = Math.toRadians(to.longitude() - from.longitude());
        double y = Math.sin(longitudeDelta) * Math.cos(toLatitude);
        double x = Math.cos(fromLatitude) * Math.sin(toLatitude)
                - Math.sin(fromLatitude) * Math.cos(toLatitude) * Math.cos(longitudeDelta);
        return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0;
    }

    private static void requireRadius(double radiusKm) {
        if (!Double.isFinite(radiusKm) || radiusKm <= 0.0 || radiusKm > 1_000.0) {
            throw new IllegalArgumentException("Radius must be greater than 0 and at most 1000 km.");
        }
    }
}
