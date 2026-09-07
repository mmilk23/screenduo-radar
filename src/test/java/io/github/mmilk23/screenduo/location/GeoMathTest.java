package io.github.mmilk23.screenduo.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GeoMathTest {

    @Test
    void calculatesDistanceAndBearing() {
        GeoPoint origin = new GeoPoint(0.0, 0.0);
        GeoPoint oneDegreeEastOnEquator = new GeoPoint(0.0, 1.0);

        assertEquals(111.195, GeoMath.distanceKm(origin, oneDegreeEastOnEquator), 0.001);
        assertEquals(90.0, GeoMath.bearingDegrees(origin, oneDegreeEastOnEquator), 0.001);
    }

    @Test
    void boundingBoxContainsCardinalPointsAtRequestedRadius() {
        GeoPoint center = new GeoPoint(-22.9068, -43.1729);
        BoundingBox box = GeoMath.boundingBox(center, 50.0);

        assertTrue(box.minimumLatitude() < center.latitude());
        assertTrue(box.maximumLatitude() > center.latitude());
        assertTrue(box.minimumLongitude() < center.longitude());
        assertTrue(box.maximumLongitude() > center.longitude());
    }
}
