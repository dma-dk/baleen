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
package dk.dma.baleen.connector.secom.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.util.GeometricShapeFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An UN LoCode.
 */
public record UnLoCode(double latitude, double longitude, String status) {

    /** The list to load. */
    private static final String RESOURCE_PATH = "UnLoCodes.json";

    /** All loaded entries. */
    private static final Map<String, UnLoCode> MAP;

    static {
        InputStream s = UnLoCode.class.getClassLoader().getResourceAsStream(RESOURCE_PATH);
        if (s == null) {
            throw new ExceptionInInitializerError("UnLoCode file " + RESOURCE_PATH + " could not be found on the classpath");
        }

        try {
            MAP = load(s);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Provides access to the UN/LOCODE entry for the provided UN/LOCODE string.
     *
     * @param unLoCode
     *            the provided UN/LOCODE string
     * @return the corresponding UN/LOCODE map entry
     */
    public static Optional<UnLoCode> get(String unLoCode) {
        return Optional.ofNullable(MAP.get(unLoCode));
    }

    /**
     * Fetch lat/lon from json mapping file and populate coverage geometry with it as point
     *
     * @throws Exception
     *             if the unLoCode mapping file could not be found
     */
    private static Map<String, UnLoCode> load(InputStream inStream) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, UnLoCode> result = new HashMap<>();

        List<UnLoCodeEntry> entries = objectMapper.readValue(inStream, new TypeReference<List<UnLoCodeEntry>>() {});
        for (UnLoCodeEntry entry : entries) {
            if (StringUtils.isNotBlank(entry.coordinates)) {
                String[] coords = entry.coordinates.trim().split("\\s");
                if (coords.length == 2) {
                    double latitude = parseCoordinate(coords[0], "S");
                    double longitude = parseCoordinate(coords[1], "W");

                    UnLoCode unLoCode = new UnLoCode(latitude, longitude, entry.status);
                    result.put(entry.country + entry.location, unLoCode);
                }
            }
        }
        return Map.copyOf(result);
    }

    private static double parseCoordinate(String coord, String negativeDirection) {
        String degrees = coord.substring(0, coord.length() - 3);
        String minutes = coord.substring(coord.length() - 3, coord.length() - 1);
        String direction = coord.substring(coord.length() - 1);

        double value = Double.parseDouble(degrees + "." + minutes);
        return direction.equals(negativeDirection) ? -value : value;
    }

    /** {@return a JTS geometry for UnLoCode} */
    public Geometry toGeometry() {
        double diameterInMeters = 1000d; // 1km
        GeometricShapeFactory geometricShapeFactory = new GeometricShapeFactory(new GeometryFactory(new PrecisionModel(), 4326));
        geometricShapeFactory.setNumPoints(64);
        geometricShapeFactory.setCentre(new Coordinate(this.longitude, this.latitude));
        // Length in meters of 1° of latitude = always 111.32 km
        geometricShapeFactory.setWidth(diameterInMeters / 111320d);
        // Length in meters of 1° of longitude = 40075 km * cos( latitude ) / 360
        geometricShapeFactory.setHeight(diameterInMeters / (40075000 * Math.cos(Math.toRadians(latitude)) / 360));
        return geometricShapeFactory.createEllipse();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class UnLoCodeEntry {
        @JsonProperty("Coordinates")
        public String coordinates;

        @JsonProperty("Country")
        public String country;

        @JsonProperty("Location")
        public String location;

        @JsonProperty("Status")
        public String status;
    }
}
