package io.github.mmilk23.screenduo.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GeoMathTest {

    @Test
    void calculatesDistanceAndBearing() {
        GeoPoint rioDeJaneiro = new GeoPoint(-22.9068, -43.1729);
        GeoPoint saoPaulo = new GeoPoint(-23.5505, -46.6333);

        assertEquals(357.7, GeoMath.distanceKm(rioDeJaneiro, saoPaulo), 2.0);
        assertEquals(266.3, GeoMath.bearingDegrees(rioDeJaneiro, saoPaulo), 2.0);
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
