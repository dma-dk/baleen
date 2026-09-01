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
import java.util.regex.Pattern;

import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.S100SpatialAttributeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.AbstractGMLType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.MessageSeriesIdentifierType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnAreaAffected;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnPart;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnPreamble;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.References;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.WarningTypeType;

/**
 *
 */
public class S124DatasetReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(S124DatasetReader.class);

    /**
     * The shape of a Marine Resource Name, copied verbatim from the MRN_PATTERN constant of
     * {@code dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets.S124ExchangeSetFactory} (niord-xml-bindings
     * 0.1.0), which is what refuses a dataset when the exchange set is built. The S-100 exchange catalogue schema
     * (S100Catalog 20240415, {@code MRNType}) asks only for {@code urn:mrn:.+}, so the factory pattern is the stricter
     * of the two and the one a name must satisfy to make it all the way out to a client.
     */
    private static final Pattern MRN_PATTERN = Pattern.compile("urn:mrn:[A-Za-z0-9][A-Za-z0-9-]*(:[A-Za-z0-9()+,\\-.:=@;$_!*'%/?#]+)+");

    /**
     * A run of characters no MRN segment may hold - the character class {@link #MRN_PATTERN} allows a segment, minus
     * the ':' that separates segments.
     */
    private static final Pattern ILLEGAL_IN_SEGMENT = Pattern.compile("[^A-Za-z0-9()+,\\-.=@;$_!*'%/?#]+");

    /**
     * {@return the MRN naming the warning this message series identifier belongs to}
     * <p>
     * What comes back is what the dataset is stored under and what its SECOM dataReference is hashed from (see
     * {@link MRNToUUID}), so a name the producer supplied is handed back byte for byte. Normalising it - putting it
     * through the kind of helper that maps ':' to '-' - would rewrite every correct MRN and silently move every
     * dataset already served to a different dataReference.
     * <p>
     * A supplied name that is not an MRN is therefore refused, not repaired: 'urn:mrn:iho:nw:dk:local warning-000-26',
     * which an older Niord template produced by pasting a shortId in raw, names a warning nobody can look up, and a
     * repaired name would be a different warning to every client holding the original. Callers already treat one bad
     * dataset as one bad dataset - the Niord reload logs it and moves on to the next - so refusing it costs the batch
     * nothing, while storing it costs every client that later asks for it. POST /api/upload is open to anyone in both
     * security profiles, so every dataset that reaches the store passes through here first.
     *
     * @param identifier
     *            the message series identifier to name the warning from
     * @throws IllegalArgumentException
     *             if there is no identifier, or the name it yields is not a well formed MRN
     */
    public static String toMRN(MessageSeriesIdentifierType identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("The dataset carries no messageSeriesIdentifier, so there is no MRN to name it by");
        }

        // The producer named the warning itself, and that name is the warning's identity, so it is kept as it is.
        String supplied = identifier.getInteroperabilityIdentifier();
        if (supplied != null && !supplied.trim().isEmpty()) {
            LOGGER.debug("Naming dataset by the interoperabilityIdentifier its producer supplied: '{}'", supplied);
            return requireMRN(supplied.trim(), "interoperabilityIdentifier");
        }

        // Nothing was supplied, so Baleen has to mint a name out of the fields that are left. Those are free text, and
        // free text is where the space in the malformed name above came from, so each field is turned into characters
        // an MRN segment may hold before it is appended. That is safe here, and only here: these are segments we name
        // ourselves, not somebody else's finished MRN.
        LOGGER.debug("No interoperabilityIdentifier supplied, naming the dataset from agency={}, nationality={}, year={}, warningNumber={}, type={}",
                identifier.getAgencyResponsibleForProduction(), identifier.getNationality(), identifier.getYear(), identifier.getWarningNumber(),
                identifier.getWarningType());

        StringBuilder b = new StringBuilder();
        b.append("urn:mrn:dk:baleen:s-124");

        // Add agency
        if (identifier.getAgencyResponsibleForProduction() != null && !identifier.getAgencyResponsibleForProduction().isEmpty()) {
            b.append(":").append(toSegment(identifier.getAgencyResponsibleForProduction().toLowerCase()));
        }

        // Add country
        if (identifier.getNationality() != null && !identifier.getNationality().isEmpty()) {
            b.append(":").append(toSegment(identifier.getNationality().toLowerCase()));
        }

        // Add year and warning number
        b.append(":").append(identifier.getYear());
        b.append(":").append(identifier.getWarningNumber());

        // Add warning type if present
        WarningTypeType warningType = identifier.getWarningType();
        if (warningType != null) {
            // Use the code if available, otherwise use the value
            if (warningType.getCode() != null) {
                b.append(":").append(toSegment(warningType.getCode().toString()));
            } else if (warningType.getValue() != null) {
                b.append(":").append(toSegment(String.valueOf(warningType.getValue())));
            }
        }

        String result = b.toString();
        LOGGER.debug("Named a dataset that supplied no interoperabilityIdentifier '{}'", result);

        return requireMRN(result, "the message series identifier fields");
    }

    /**
     * {@return the name unchanged, as long as it is an MRN}
     *
     * @param mrn
     *            the name to check
     * @param source
     *            where the name came from, for the message of the exception
     * @throws IllegalArgumentException
     *             if the name is not a well formed MRN
     */
    private static String requireMRN(String mrn, String source) {
        if (!MRN_PATTERN.matcher(mrn).matches()) {
            throw new IllegalArgumentException("'" + mrn + "' (from " + source + ") is not a Marine Resource Name, it does not match "
                    + MRN_PATTERN.pattern() + ". The dataset is refused rather than stored under a name no client can ask for");
        }
        return mrn;
    }

    /**
     * {@return the text as a single MRN segment, every run of characters an MRN may not hold replaced by a hyphen}
     * <p>
     * Only ever applied to one field at a time, never to a whole MRN: ':' separates segments, so an MRN put through
     * this would come back with its colons turned into hyphens, and would hash to a dataReference other than the one
     * its dataset has already been served under.
     *
     * @param text
     *            the field to turn into a segment
     */
    private static String toSegment(String text) {
        return ILLEGAL_IN_SEGMENT.matcher(text).replaceAll("-");
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

    /**
     * {@return the identifiers of the warnings this dataset references that name a warning by an MRN}
     * <p>
     * A reference is only ever looked up - the upload asks the store for the warning a reference names and links it if
     * it is there - so a reference that is not an MRN can match nothing, and leaving it out here is the same outcome
     * the lookup gave before names were checked at all. Passing it on instead would cost the whole dataset, because
     * {@link #toMRN} refuses a name that is not an MRN, and a dataset named perfectly well itself should not be
     * refused over a warning some older producer named badly.
     */
    public static List<MessageSeriesIdentifierType> findAllReferences(Dataset ds) {
        List<References> list = findAll(References.class, ds);
        if (list.size() == 1) {
            List<MessageSeriesIdentifierType> mt = list.get(0).getMessageSeriesIdentifiers();
            if (mt != null) {
                return mt.stream().filter(S124DatasetReader::namesAWarning).toList();
            }
        } else if (list.size() > 1) {
            throw new IllegalArgumentException("Multiple reference types in dataset");
        }
        return List.of();
    }

    /** {@return whether this identifier yields an MRN, which is the only name a stored warning can be found under} */
    private static boolean namesAWarning(MessageSeriesIdentifierType identifier) {
        try {
            toMRN(identifier);
            return true;
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Leaving out a reference that names no warning we could look up: {}", e.getMessage());
            return false;
        }
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
