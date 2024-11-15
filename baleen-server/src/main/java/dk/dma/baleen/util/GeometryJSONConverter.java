package dk.dma.baleen.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

import java.io.IOException;

/**
 * The type Geometry JSON converter.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class GeometryJSONConverter {

    /**
     * Convert from geometry to a JSON node.
     *
     * @param geometry the geometry
     * @return the json node
     */
    public static JsonNode convertFromGeometry(Geometry geometry) {
        if (geometry == null) {
            return null;
        }

        ObjectMapper om = new ObjectMapper();
        try {
            JsonNode node = om.readTree(new GeoJsonWriter().write(geometry));
            return node;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Convert from a JSON node to geometry.
     *
     * @param jsonNode the json node
     * @return the geometry
     */
    public static Geometry convertToGeometry(JsonNode jsonNode) {
        if (jsonNode == null  || jsonNode.toString() == "null" || jsonNode.asText() == "null") {
            return null;
        }

        try {
            return new GeoJsonReader().read(jsonNode.toString());
        } catch (ParseException e) {
            return null;
        }
    }

}
