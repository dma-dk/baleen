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
package dk.dma.baleen.service;

import static java.util.Objects.requireNonNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/** Represents a unique identifier of a S-100 data set. */
public final class DatasetId {

    /** The full id */
    private final String id;

    private DatasetId(String id) {
        this.id = requireNonNull(id);
    }

    // Producer Code (assigned by IHO to data producers)
    public String producerCode() {
        return id.substring(3, 7);
    }

    // Product Specification number (e.g., S-101 for Electronic Navigational Charts, S-102 for Bathymetric Surface)
    public String productCode() {
        return id.substring(0, 3);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return id;
    }

    public UUID toUUID() {
        // Uses MD5, Theoretical collision resistance of 2^64 due to the birthday paradox
        // Chance of collision with 10^9 datasets ~0.00271% chance of at least one collision
        return UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8));
    }

    // Uses SHA to create it
    // Collision resistance of 2^80 due to birthday paradox
    // I just keep this for reference
    UUID getUUID() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(id.getBytes(StandardCharsets.UTF_8));

            // Use first 16 bytes (128 bits) of the hash
            long msb = 0;
            long lsb = 0;

            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (hash[i] & 0xff);
                lsb = (lsb << 8) | (hash[8 + i] & 0xff);
            }

            // Set version to 4 (random) and variant to 2 (IETF)
            msb &= ~(0xf000L); // clear version
            msb |= 0x4000; // set version to 4
            lsb &= ~(0xc000000000000000L); // clear variant
            lsb |= 0x8000000000000000L; // set variant to 2

            return new UUID(msb, lsb);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create UUID", e);
        }
    }

    // Dataset ID (unique within the producer's domain)
    public String uniqueIdentifier() {
        return id.substring(7);
    }

    /**
     * Creates a DatasetId from a complete dataset identifier string
     *
     * @param datasetId
     *            The complete dataset identifier string
     * @return A new DatasetId instance
     * @throws IllegalArgumentException
     *             if the string format is invalid
     */
    public static DatasetId of(String datasetId) {
        requireNonNull(datasetId, "Dataset ID cannot be null");

        if (datasetId.length() < 8) {
            throw new IllegalArgumentException(
                    "Dataset ID must be at least 8 characters long (3 for product code + 4 for producer code + 1 for unique identifier)");
        }

        String productCode = datasetId.substring(0, 3);
        String producerCode = datasetId.substring(3, 7);
        String uniqueIdentifier = datasetId.substring(7);

        validateComponents(productCode, producerCode, uniqueIdentifier);
        return new DatasetId(datasetId);
    }

    /**
     * Creates a DatasetId from its component parts
     *
     * @param productCode
     *            Three-digit product specification number
     * @param producerCode
     *            Four-character producer code from IHO registry (letters or digits)
     * @param uniqueIdentifier
     *            Alphanumeric unique identifier
     * @return A new DatasetId instance
     * @throws IllegalArgumentException
     *             if any component is invalid
     */
    public static DatasetId of(String productCode, String producerCode, String uniqueIdentifier) {
        validateComponents(productCode, producerCode, uniqueIdentifier);
        return new DatasetId(productCode + producerCode + uniqueIdentifier);
    }

    private static void validateComponents(String productCode, String producerCode, String uniqueIdentifier) {
        // Null checks
        requireNonNull(productCode, "Product code cannot be null");
        requireNonNull(producerCode, "Producer code cannot be null");
        requireNonNull(uniqueIdentifier, "Unique identifier cannot be null");

        // Length checks
        if (productCode.length() != 3) {
            throw new IllegalArgumentException("Product code must be exactly 3 digits, got: " + productCode);
        }
        if (producerCode.length() != 4) {
            throw new IllegalArgumentException("Producer code must be exactly 4 characters, got: " + producerCode);
        }
        if (uniqueIdentifier.isEmpty()) {
            throw new IllegalArgumentException("Unique identifier cannot be empty");
        }

        // Character validation for product code
        for (int i = 0; i < productCode.length(); i++) {
            if (!Character.isDigit(productCode.charAt(i))) {
                throw new IllegalArgumentException(String.format("Product code must contain only digits, found invalid character '%c' at position %d in '%s'",
                        productCode.charAt(i), i, productCode));
            }
        }

        // Character validation for producer code
        for (int i = 0; i < producerCode.length(); i++) {
            if (!Character.isLetterOrDigit(producerCode.charAt(i))) {
                throw new IllegalArgumentException(
                        String.format("Producer code must contain only letters or digits, found invalid character '%c' at position %d in '%s'",
                                producerCode.charAt(i), i, producerCode));
            }
        }

        // Character validation for unique identifier
        for (int i = 0; i < uniqueIdentifier.length(); i++) {
            if (!Character.isLetterOrDigit(uniqueIdentifier.charAt(i))) {
                throw new IllegalArgumentException(
                        String.format("Unique identifier must contain only letters or digits, found invalid character '%c' at position %d in '%s'",
                                uniqueIdentifier.charAt(i), i, uniqueIdentifier));
            }
        }
    }
}
