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
package dk.dma.baleen.service.s124.util;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Geometry;

import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.S100SpatialAttributeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.AbstractGMLType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.MessageSeriesIdentifierType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnAreaAffected;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnPart;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnPreamble;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.References;

/**
 *
 */
public class S124DatasetReader {

    public static String toMRN(MessageSeriesIdentifierType identifier) {
        // Add detailed logging for debugging
        System.out.println("DEBUG: toMRN called with identifier: " + identifier);
        System.out.println("DEBUG: identifier.getWarningIdentifier(): " + identifier.getInteroperabilityIdentifier());
        System.out.println("DEBUG: warningIdentifier is null: " + (identifier.getInteroperabilityIdentifier() == null));
        if (identifier.getInteroperabilityIdentifier() != null) {
            System.out.println("DEBUG: warningIdentifier length: " + identifier.getInteroperabilityIdentifier().length());
            System.out.println("DEBUG: warningIdentifier after trim: '" + identifier.getInteroperabilityIdentifier().trim() + "'");
            System.out.println("DEBUG: warningIdentifier is empty after trim: " + identifier.getInteroperabilityIdentifier().trim().isEmpty());
        }
        
        // First check if warningIdentifier is available and use it directly
        if (identifier.getInteroperabilityIdentifier() != null && !identifier.getInteroperabilityIdentifier().trim().isEmpty()) {
            String warningIdentifier = identifier.getInteroperabilityIdentifier().trim();
            System.out.println("DEBUG: Using warningIdentifier as MRN: " + warningIdentifier);
            return warningIdentifier;
        }
        
        // Fallback to constructing MRN from other fields
        System.out.println("DEBUG: warningIdentifier not available, constructing MRN from other fields");
        StringBuilder b = new StringBuilder();
        b.append("urn:mrn:dk:baleen:s-124");
        
        // Log what we're getting
        System.out.println("DEBUG: Creating MRN from identifier: " + identifier);
        if (identifier != null) {
            System.out.println("  Agency: " + identifier.getAgencyResponsibleForProduction());
            System.out.println("  Country: " + identifier.getNationality());
            System.out.println("  WarningNumber: " + identifier.getWarningNumber());
            System.out.println("  Year: " + identifier.getYear());
            System.out.println("  WarningType: " + identifier.getWarningType());
        }
        
        // Add agency
        if (identifier.getAgencyResponsibleForProduction() != null && !identifier.getAgencyResponsibleForProduction().isEmpty()) {
            b.append(":").append(identifier.getAgencyResponsibleForProduction().toLowerCase());
        }
        
        // Add country
        if (identifier.getNationality() != null && !identifier.getNationality().isEmpty()) {
            b.append(":").append(identifier.getNationality().toLowerCase());
        }
        
        // Add year and warning number
        b.append(":").append(identifier.getYear());
        b.append(":").append(identifier.getWarningNumber());
        
        // Add warning type if present
        if (identifier.getWarningType() != null) {
            // Use the code if available, otherwise use the value
            if (identifier.getWarningType().getCode() != null) {
                b.append(":").append(identifier.getWarningType().getCode());
            } else if (identifier.getWarningType().getValue() != null) {
                b.append(":").append(identifier.getWarningType().getValue());
            }
        }

        String result = b.toString();
        System.out.println("  Generated MRN: " + result);
        
        return result;
    }

    private static <T extends AbstractGMLType> List<T> findAll(Class<T> gmlType, Dataset ds) {
        List<T> result = new ArrayList<>();
        if (ds.getMembers() != null) {
            for (AbstractGMLType t : ds.getMembers().getNavwarnPartsAndNavwarnAreaAffectedsAndTextPlacements()) {
                if (gmlType.isInstance(t)) {
                    result.add(gmlType.cast(t));
                }
            }
        }
        return List.copyOf(result);
    }

    public static NavwarnPreamble findPreamble(Dataset ds) {
        List<NavwarnPreamble> list = findAll(NavwarnPreamble.class, ds);
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

        for (NavwarnPart p : findAll(NavwarnPart.class, ds)) {
            for (NavwarnPart.Geometry g : p.getGeometries()) {
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

        for (NavwarnAreaAffected p : findAll(NavwarnAreaAffected.class, ds)) {
            for (NavwarnAreaAffected.Geometry g : p.getGeometries()) {
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
