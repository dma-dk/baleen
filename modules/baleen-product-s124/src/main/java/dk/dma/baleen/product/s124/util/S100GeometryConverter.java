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
package dk.dma.baleen.product.s124.util;

import dk.dma.baleen.s100.xmlbindings.s100.gml.base._5_0.*;
import dk.dma.baleen.s100.xmlbindings.s100.gml.base._5_0.CurveType;
import dk.dma.baleen.s100.xmlbindings.s100.gml.base._5_0.PointType;
import dk.dma.baleen.s100.xmlbindings.s100.gml.base._5_0.SurfaceType;
import dk.dma.baleen.s100.xmlbindings.s100.gml.base._5_0.impl.*;
import dk.dma.baleen.s100.xmlbindings.s100.gml.base._5_0.impl.CurveTypeImpl;
import dk.dma.baleen.s100.xmlbindings.s100.gml.base._5_0.impl.PointTypeImpl;
import dk.dma.baleen.s100.xmlbindings.s100.gml.base._5_0.impl.SurfaceTypeImpl;
import dk.dma.baleen.s100.xmlbindings.s100.gml.profiles._5_0.*;
import dk.dma.baleen.s100.xmlbindings.s100.gml.profiles._5_0.ObjectFactory;
import dk.dma.baleen.s100.xmlbindings.s100.gml.profiles._5_0.impl.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Lineal;
import org.locationtech.jts.geom.Polygonal;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.Puntal;

import jakarta.xml.bind.JAXBElement;

/**
 * A geometry converter to and from JTS Geometry and S-100 Point/Curve/Surface attributes type.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public final class S100GeometryConverter {
    private S100GeometryConverter() {}

    /**
     * Translates the generic JTS geometry to a list of generic S-100 point/curve/surface geometries that can be understood
     * and handled by services.
     *
     * @param geometry
     *            The JTS geometry object
     * @return a list S-100 point/curve/surface geometries
     */
    public static List<S100SpatialAttributeType> convertFromGeometry(Geometry geometry) {
        // Return the populated property
        return populatePointCurveSurfaceToGeometry(geometry, new ArrayList<>());
    }

    /**
     * Translates the generic point/curve/surface property of the list of feature type into a JTS geometry (most likely a
     * geometry collection) that can be understood and handled by the services.
     *
     * @param list
     *            a list of S-100 point/curve/surface properties
     * @return the respective geometry
     */
    public static Geometry convertToGeometry(List<S100SpatialAttributeType> list) {
        return convertToGeometry(list, new GeometryFactory(new PrecisionModel(), 4326));
    }

    public static Geometry convertToGeometry(List<S100SpatialAttributeType> list, GeometryFactory geometryFactory) {

        // Handle empty list case
        if (list == null || list.isEmpty()) {
            return geometryFactory.createEmpty(-1);
        }

        // Result collection to store processed geometries
        List<Geometry> resultGeometries = new ArrayList<>();

        // Process each spatial attribute
        for (S100SpatialAttributeType attribute : list) {
            Geometry geometry = null;

            if (attribute instanceof PointProperty pointProperty) {
                // Handle Point Property
                PointType point = pointProperty.getPoint();
                if (point != null && point.getPos() != null) {
                    Pos pos = point.getPos();
                    Double[] values = pos.getValue();
                    if (values != null && values.length >= 2) {
                        // JTS uses (lon,lat), GML uses (lat,lon)
                        Coordinate coord = new Coordinate(values[1], values[0]);
                        geometry = geometryFactory.createPoint(coord);
                    }
                }
            } else if (attribute instanceof CurveProperty curveProperty) {
                // Handle Curve Property
                List<Geometry> curveGeometries = new ArrayList<>();

                CurveType curve = curveProperty.getCurve();
                if (curve != null && curve.getSegments() != null) {
                    Segments segments = curve.getSegments();
                    List<JAXBElement<? extends AbstractCurveSegmentType>> curveSegments = segments.getAbstractCurveSegments();

                    for (JAXBElement<?> element : curveSegments) {
                        Object value = element.getValue();
                        if (value instanceof LineStringSegmentType lineString) {
                            PosList posList = lineString.getPosList();
                            if (posList != null) {
                                Coordinate[] coords = gmlPosListToCoordinates(posList);
                                if (coords.length == 1) {
                                    curveGeometries.add(geometryFactory.createPoint(coords[0]));
                                } else {
                                    curveGeometries.add(geometryFactory.createLineString(coords));
                                }
                            }
                        }
                    }
                }

                if (!curveGeometries.isEmpty()) {
                    geometry = geometryFactory.createGeometryCollection(curveGeometries.toArray(new Geometry[0]));
                }
            } else if (attribute instanceof SurfaceProperty surfaceProperty) {
                // Handle Surface Property
                List<Geometry> surfaceGeometries = new ArrayList<>();

                SurfaceType surface = surfaceProperty.getSurface();
                if (surface != null && surface.getPatches() != null) {
                    Patches patches = surface.getPatches();
                    List<JAXBElement<? extends AbstractSurfacePatchType>> surfacePatches = patches.getAbstractSurfacePatches();

                    for (JAXBElement<?> element : surfacePatches) {
                        Object value = element.getValue();
                        if (value instanceof PolygonPatchType polygonPatch) {
                            AbstractRingPropertyType exterior = polygonPatch.getExterior();
                            if (exterior != null && exterior.getAbstractRing() != null) {
                                JAXBElement<?> ring = exterior.getAbstractRing();
                                Object ringValue = ring.getValue();
                                if (ringValue instanceof LinearRingType linearRing) {
                                    PosList posList = linearRing.getPosList();
                                    if (posList != null) {
                                        Coordinate[] coords = gmlPosListToCoordinates(posList);
                                        if (coords.length == 1) {
                                            surfaceGeometries.add(geometryFactory.createPoint(coords[0]));
                                        } else {
                                            surfaceGeometries.add(geometryFactory.createPolygon(coords));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!surfaceGeometries.isEmpty()) {
                    geometry = geometryFactory.createGeometryCollection(surfaceGeometries.toArray(new Geometry[0]));
                }
            }

            if (geometry != null) {
                resultGeometries.add(geometry);
            }
        }

        // Combine all geometries using union
        Geometry result = geometryFactory.createEmpty(-1);
        for (Geometry geom : resultGeometries) {
            if (result.isEmpty()) {
                result = geom;
            } else {
                result = result.union(geom);
            }
        }

        return result;
    }

    /**
     * A simple utility function that receives JTS geometry coordinates and constructs a position list object.
     *
     * @param coordinates
     *            the provided coordinates
     * @return the respective position list
     */
    private static PosList coordinatesToGmlPosList(Coordinate[] coordinates) {
        // Translate the coordinates to a simple list of doubles (Y, X)
        // JTS uses (lon,lat), GML uses (lat,lon)
        List<Double> coords = Optional.ofNullable(coordinates).map(Arrays::asList).orElse(Collections.emptyList()).stream()
                .map(c -> Arrays.asList(c.getY(), c.getX())).flatMap(List::stream).toList();

        // Then create the list and return
        PosList posList = new PosListImpl();
        posList.setValue(coords.toArray(Double[]::new));
        return posList;
    }

    /**
     * Populates and return an S-100 curve property based on the provided line segment geometry coordinates.
     *
     * @param coords
     *            The coordinates of the element to be generated
     * @return The populated point property
     */
    private static LineStringSegmentType generateCurvePropertySegment(Double[] coords) {
        // Generate the elements
        LineStringSegmentType lineStringSegmentType = new LineStringSegmentTypeImpl();
        PosList posList = new PosListImpl();

        // Populate with the geometry data
        posList.setValue(coords);
        lineStringSegmentType.setPosList(posList);

        // And return the output
        return lineStringSegmentType;
    }

    /**
     * Populates and return an S-100 point property based on the provided point geometry coordinates.
     *
     * @param coords
     *            The coordinates of the element to be generated
     * @return The populated point property
     */
    private static Pos generatePointPropertyPosition(Double[] coords) {
        // Generate the elements
        Pos pos = new PosImpl();

        // Populate with the geometry data
        pos.setValue(coords);

        // And return the output
        return pos;
    }

    /**
     * Populates and return an S-100 surface property based on the provided surface geometry coordinates.
     *
     * @param coords
     *            The coordinates of the element to be generated
     * @return The populated point property
     */
    private static PolygonPatchType generateSurfacePropertyPatch(Double[] coords) {
        // Create an OpenGIS GML factory
        ObjectFactory opengisGMLFactory = new ObjectFactory();

        // Generate the elements
        PolygonPatchType polygonPatchType = new PolygonPatchTypeImpl();
        AbstractRingPropertyType abstractRingPropertyType = new AbstractRingPropertyTypeImpl();
        LinearRingType linearRingType = new LinearRingTypeImpl();
        PosList posList = new PosListImpl();

        // Populate with the geometry data
        posList.setValue(coords);

        // Populate the elements
        linearRingType.setPosList(posList);
        abstractRingPropertyType.setAbstractRing(opengisGMLFactory.createLinearRing(linearRingType));
        polygonPatchType.setExterior(abstractRingPropertyType);

        // And return the output
        return polygonPatchType;
    }

    /**
     * A simple utility function that splits the position list values by two and generates JTS geometry coordinates by them.
     *
     * @param posList
     *            the provided position list
     * @return the respective coordinates
     */
    private static Coordinate[] gmlPosListToCoordinates(PosList posList) {
        final List<Coordinate> result = new ArrayList<>();
        for (int i = 0; i < posList.getValue().length; i = i + 2) {
            result.add(new Coordinate(posList.getValue()[i + 1], posList.getValue()[i]));
        }
        return result.toArray(new Coordinate[] {});
    }

    /**
     * Initialise the S-100 Curve Property object
     *
     * @return the initialised S-100 Curve Property object
     */
    private static CurveProperty initialiseCurveProperty() {
        // Generate the elements
        CurveProperty curveProperty = new CurvePropertyImpl();
        CurveType curveType = new CurveTypeImpl();
        Segments segments = new SegmentsImpl();

        // Populate the elements
        curveType.setSegments(segments);
        curveProperty.setCurve(curveType);

        // And return the output
        return curveProperty;
    }

    /**
     * Initialise the S-100 Surface Property object
     *
     * @return the initialised S-100 Surface Property object
     */
    private static SurfaceProperty initialiseSurfaceProperty() {

        // Generate the elements
        SurfaceProperty surfaceProperty = new SurfacePropertyImpl();
        SurfaceType surfaceType = new SurfaceTypeImpl();
        Patches patches = new PatchesImpl();

        // Populate the elements
        surfaceType.setPatches(patches);
        surfaceProperty.setSurface(surfaceType);

        // And return the output
        return surfaceProperty;
    }

    /**
     * Initialise the S-100 Point Property object
     *
     * @return the initialised S-100 Point Property object
     */
    private static PointProperty initPointProperty() {
        // Generate the elements
        PointProperty pointProperty = new PointPropertyImpl();
        PointType pointType = new PointTypeImpl();

        // Populate the elements
        pointProperty.setPoint(pointType);

        // And return the output
        return pointProperty;
    }

    /**
     * A iterative helper function that examines the provided geometry and dives deeper into collection to pick up the basic
     * JTS geometry types such as the points, lines and polygons.
     *
     * @param geometry
     *            The geometry to be examined
     * @param s100SpatialAttributeTypes
     *            The S-100 geometry object to be populated
     */
    private static List<S100SpatialAttributeType> populatePointCurveSurfaceToGeometry(Geometry geometry,
            List<S100SpatialAttributeType> s100SpatialAttributeTypes) {
        // Create an OpenGIS GML factory
        ObjectFactory opengisGMLFactory = new ObjectFactory();

        if (geometry instanceof Puntal) {
            // Initialise the point property if not already initialised
            PointProperty pointProperty = initPointProperty();

            // And append the point
            pointProperty.getPoint().setPos(generatePointPropertyPosition(coordinatesToGmlPosList(geometry.getCoordinates()).getValue()));
            s100SpatialAttributeTypes.add(pointProperty);
        } else if (geometry instanceof Lineal) {
            // Initialise the curve property if not already initialised
            CurveProperty curveProperty = initialiseCurveProperty();

            // And append the line string
            curveProperty.getCurve().getSegments().getAbstractCurveSegments().add(
                    opengisGMLFactory.createLineStringSegment(generateCurvePropertySegment(coordinatesToGmlPosList(geometry.getCoordinates()).getValue())));
            s100SpatialAttributeTypes.add(curveProperty);
        } else if (geometry instanceof Polygonal) {
            // Initialise the curve property if not already initialised
            SurfaceProperty surfaceProperty = initialiseSurfaceProperty();

            // And append the surface patch
            surfaceProperty.getSurface().getPatches().getAbstractSurfacePatches()
                    .add(opengisGMLFactory.createPolygonPatch(generateSurfacePropertyPatch(coordinatesToGmlPosList(geometry.getCoordinates()).getValue())));
            s100SpatialAttributeTypes.add(surfaceProperty);
        } else if (geometry instanceof GeometryCollection && geometry.getNumGeometries() > 0) {
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                populatePointCurveSurfaceToGeometry(geometry.getGeometryN(i), s100SpatialAttributeTypes);
            }
        }

        // And return the property
        return s100SpatialAttributeTypes;
    }
}
