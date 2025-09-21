/*
 * Copyright (c) 2024 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dk.dma.baleen.s124.service;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.util.GeometricShapeFactory;

class GeometryHelper {

    static final Coordinate centerPoint = new Coordinate(10.6362500, 54.6586333);

    static Geometry pointIncluded() {
        // Create first area - a circle with 50m radius that includes the point
        GeometricShapeFactory shapeFactory1 = new GeometricShapeFactory();
        shapeFactory1.setNumPoints(32);
        shapeFactory1.setCentre(centerPoint);
        // Converting 50 meters to degrees (approximate at this latitude)
        double radiusInDegrees1 = 50.0 / 111320.0;
        shapeFactory1.setSize(radiusInDegrees1 * 2);
        Geometry geo = shapeFactory1.createCircle();
        geo.setSRID(4326);
        return geo;
    }

    static Geometry pointExcluded() {
        // Create second area - a circle with 30m radius, centered 80m east of the point
        GeometricShapeFactory shapeFactory2 = new GeometricShapeFactory();
        shapeFactory2.setNumPoints(32);
        // Moving the center 80m east (approximate at this latitude)
        double offsetInDegrees = 80.0 / (111320.0 * Math.cos(Math.toRadians(54.6586333)));
        Coordinate center2 = new Coordinate(10.6362500 + offsetInDegrees, 54.6586333);
        shapeFactory2.setCentre(center2);
        // 30m radius in degrees
        double radiusInDegrees2 = 30.0 / 111320.0;
        shapeFactory2.setSize(radiusInDegrees2 * 2);
        Geometry geo = shapeFactory2.createCircle();
        geo.setSRID(4326);
        return geo;
    }
}