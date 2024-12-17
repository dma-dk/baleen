/*
 * Copyright (c) 2008 Kasper Nielsen.
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
package dk.dma.baleen.product.s124.util;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Geometry;

import dk.dma.baleen.s100.xmlbindings.s100.gml.base._5_0.S100SpatialAttributeType;
import dk.dma.baleen.s100.xmlbindings.s100.gml.profiles._5_0.AbstractGMLType;
import dk.dma.baleen.s100.xmlbindings.s124.v1_0_0.Dataset;
import dk.dma.baleen.s100.xmlbindings.s124.v1_0_0.MessageSeriesIdentifierType;
import dk.dma.baleen.s100.xmlbindings.s124.v1_0_0.NAVWARNAreaAffected;
import dk.dma.baleen.s100.xmlbindings.s124.v1_0_0.NAVWARNPart;
import dk.dma.baleen.s100.xmlbindings.s124.v1_0_0.NAVWARNPreamble;
import dk.dma.baleen.s100.xmlbindings.s124.v1_0_0.References;

/**
 *
 */
public class S124DatasetReader {

    public static String toMRN(MessageSeriesIdentifierType identifier) {
        StringBuilder b = new StringBuilder();
        b.append("urn:mrn:dk:baleen:s-124");

        return b.toString();
    }

    private static <T extends AbstractGMLType> List<T> findAll(Class<T> gmlType, Dataset ds) {
        List<T> result = new ArrayList<>();
        if (ds.getMembers() != null) {
            for (AbstractGMLType t : ds.getMembers().getNAVWARNPreamblesAndReferencesAndNAVWARNParts()) {
                if (gmlType.isInstance(t)) {
                    result.add(gmlType.cast(t));
                }
            }
        }
        return List.copyOf(result);
    }

    public static NAVWARNPreamble findPreamble(Dataset ds) {
        List<NAVWARNPreamble> list = findAll(NAVWARNPreamble.class, ds);
        if (list.size() == 1) {
            return list.get(0);
        } else {
            throw new IllegalArgumentException("Expected exactly 1 Preamble, but found " + list.size());
        }
    }

    public static List<MessageSeriesIdentifierType> findAllReferences(Dataset ds) {
        List<References> list = findAll(References.class, ds);
        if (list.size() == 1) {
            List<MessageSeriesIdentifierType> mt = list.get(0).getMessageSeriesIdentifiers();
            if (mt != null) {
                return List.copyOf(mt);
            }
        } else if (list.size() > 1) {
            throw new IllegalArgumentException("Multiple reference types in dataset");
        }
        return List.of();
    }

    // Used in subscriotion, and get/getSummary
    // I think we should take a list, so we can include references
    public static Geometry calculateGeometry(Dataset ds) {
        List<S100SpatialAttributeType> toConvert = new ArrayList<>();

        for (NAVWARNPart p : findAll(NAVWARNPart.class, ds)) {
            for (NAVWARNPart.Geometry g : p.getGeometries()) {
                S100SpatialAttributeType at = g.getCurveProperty();
                if (at != null) {
                    toConvert.add(at);
                }

                at = g.getPointProperty();
                if (at != null) {
                    toConvert.add(at);
                }

                at = g.getSurfaceProperty();
                if (at != null) {
                    toConvert.add(at);
                }
            }
        }

        for (NAVWARNAreaAffected p : findAll(NAVWARNAreaAffected.class, ds)) {
            for (NAVWARNAreaAffected.Geometry g : p.getGeometries()) {
                S100SpatialAttributeType at = g.getCurveProperty();
                if (at != null) {
                    toConvert.add(at);
                }

                at = g.getPointProperty();
                if (at != null) {
                    toConvert.add(at);
                }

                at = g.getSurfaceProperty();
                if (at != null) {
                    toConvert.add(at);
                }
            }
        }
        return S100GeometryConverter.convertToGeometry(toConvert);
    }
}
