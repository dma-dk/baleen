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

package dk.dma.baleen.secom.controllers;

import dk.dma.baleen.secom.spi.AuthenticatedMcpNode;

/**
 * A remote node, and whether anything actually established that it is who it says it is.
 *
 * @param mrn the MRN of the node, or null when the caller is anonymous
 * @param verified whether the MRN was verified against a trusted client certificate. Always false as things
 *            stand: {@link MRNExtractorRequestFilter} reads the MRN out of an unvalidated header.
 */
public record SecomNode(String mrn, boolean verified) implements AuthenticatedMcpNode {

    /** Creates a node whose identity nothing has verified. */
    public SecomNode(String mrn) {
        this(mrn, false);
    }
}
