/*
 * Copyright (c) 2024 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dk.dma.baleen.util;

import java.util.Optional;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;

import jakarta.xml.bind.ValidationException;

/**
 * The Geometry Utils Class.
 *
 * This utility class contains various methods that can be used to easily manage geometries and deal their relevant
 * operations.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class GeometryUtils {

    public static Geometry parse(String geometry, String unlocode) throws ValidationException {
        Geometry jtsGeometry = null;
        if (geometry != null) {
            try {
                jtsGeometry = WKTUtils.convertWKTtoGeometry(geometry);
            } catch (ParseException ex) {
                throw new ValidationException(ex.getMessage());
            }
        }

        if (unlocode != null) {
            Optional<Geometry> unlo = UnLoCode.get(unlocode).map(UnLoCode::toGeometry);
            jtsGeometry = GeometryUtils.joinGeometries(jtsGeometry, unlo.orElse(null));
        }
        return jtsGeometry;
    }

    public static GeometryFactory geometryFactory() {
        // Thread-safe but not immutable...
        return new GeometryFactory(new PrecisionModel(), 4326);
    }

    /**
     * A helper function to simplify the joining of geometries without troubling ourselves for the null checking... which is
     * a pain.
     *
     * @param geometries
     *            the geometries variable argument
     * @return the resulting joined geometry
     */
    public static Geometry joinGeometries(Geometry... geometries) {
        Geometry result = null;
        for (Geometry geometry : geometries) {
            if (result == null) {
                result = geometry;
            } else if (geometry != null) {
                result = result.union(geometry);
            }
        }
        return result;
    }
}
