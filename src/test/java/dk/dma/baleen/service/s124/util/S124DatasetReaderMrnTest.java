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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.MessageSeriesIdentifierType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.WarningTypeLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.impl.DatasetImpl;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.impl.MessageSeriesIdentifierTypeImpl;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.impl.ReferencesImpl;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.impl.WarningTypeTypeImpl;

/**
 * The MRN is the name a warning is stored, referenced and asked for under, and the reader copies whatever the producer
 * put in the dataset. 'urn:mrn:iho:nw:dk:local warning-000-26' - a name with a space in it, which no MRN may hold -
 * reached the datastore that way, and POST /api/upload takes a dataset from anyone, so the way in is still open. These
 * tests hold the parts of the fix together: a name that is an MRN is passed through untouched, one that is not is
 * refused before it is stored, and a reference that names no warning is skipped rather than costing the dataset that
 * holds it.
 */
class S124DatasetReaderMrnTest {

    /** The name Niord supplies today, taken from src/test/resources/datasets/local-warning-120-26.gml. */
    private static final String WELL_FORMED = "urn:mrn:iho:nw:dk:local-warning-120-26";

    /** The name observed in the datastore, from an older Niord template that pasted a shortId in raw. */
    private static final String OBSERVED_MALFORMED = "urn:mrn:iho:nw:dk:local warning-000-26";

    @Test
    void aNameThatIsAnMrnComesBackByteForByte() {
        assertThat(S124DatasetReader.toMRN(supplying(WELL_FORMED))).isEqualTo(WELL_FORMED);
    }

    @Test
    void aWarningKeepsTheDataReferenceItHasAlreadyBeenServedUnder() throws Exception {
        // The SECOM dataReference is a hash of the MRN, so a reader that normalised names instead of checking them -
        // one that mapped ':' to '-', say - would move every warning already handed out to a reference no client
        // knows. This is that reference, pinned.
        UUID reference = MRNToUUID.createUUIDFromMRN(S124DatasetReader.toMRN(supplying(WELL_FORMED)));

        assertThat(reference).isEqualTo(UUID.fromString("6b6b9fc4-4366-80cf-9566-7f71effac9f8"));
    }

    @Test
    void theNameWithTheSpaceInItIsRefused() {
        assertThatThrownBy(() -> S124DatasetReader.toMRN(supplying(OBSERVED_MALFORMED))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(OBSERVED_MALFORMED);
    }

    @Test
    void aNiordShortIdOnItsOwnIsNotAName() {
        assertThatThrownBy(() -> S124DatasetReader.toMRN(supplying("local-warning-120-26"))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("local-warning-120-26");
    }

    @Test
    void aNameThatStopsAtTheOrganisationIsRefused() {
        assertThatThrownBy(() -> S124DatasetReader.toMRN(supplying("urn:mrn:iho"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void spaceAroundAnOtherwiseGoodNameIsNotAReasonToRefuseIt() {
        assertThat(S124DatasetReader.toMRN(supplying("  " + WELL_FORMED + "\n"))).isEqualTo(WELL_FORMED);
    }

    @Test
    void theNameBaleenMintsItselfIsAnMrnEvenWhenTheFieldsItIsMadeOfHoldSpaces() {
        MessageSeriesIdentifierTypeImpl identifier = new MessageSeriesIdentifierTypeImpl();
        identifier.setAgencyResponsibleForProduction("Danish Maritime Authorities");
        identifier.setNationality("DK");
        identifier.setYear(2025);
        identifier.setWarningNumber(4);
        WarningTypeTypeImpl warningType = new WarningTypeTypeImpl();
        warningType.setCode(BigInteger.valueOf(2));
        identifier.setWarningType(warningType);

        assertThat(S124DatasetReader.toMRN(identifier)).isEqualTo("urn:mrn:dk:baleen:s-124:danish-maritime-authorities:dk:2025:4:2");
    }

    @Test
    void anUncodedWarningTypeIsNamedByItsLabel() {
        MessageSeriesIdentifierTypeImpl identifier = new MessageSeriesIdentifierTypeImpl();
        identifier.setNationality("DK");
        identifier.setYear(2026);
        identifier.setWarningNumber(120);
        WarningTypeTypeImpl warningType = new WarningTypeTypeImpl();
        warningType.setValue(WarningTypeLabel.LOCAL_NAVIGATIONAL_WARNING);
        identifier.setWarningType(warningType);

        assertThat(S124DatasetReader.toMRN(identifier)).isEqualTo("urn:mrn:dk:baleen:s-124:dk:2026:120:LOCAL_NAVIGATIONAL_WARNING");
    }

    @Test
    void anIdentifierThatDeclaresNothingAtAllStillYieldsAName() {
        // The references of a Niord dataset carry no interoperabilityIdentifier, and often nothing else either.
        assertThat(S124DatasetReader.toMRN(new MessageSeriesIdentifierTypeImpl())).isEqualTo("urn:mrn:dk:baleen:s-124:0:0");
    }

    @Test
    void anEmptyInteroperabilityIdentifierIsNamedFromTheOtherFieldsRatherThanRefused() {
        assertThat(S124DatasetReader.toMRN(supplying("   "))).isEqualTo("urn:mrn:dk:baleen:s-124:0:0");
    }

    @Test
    void aDatasetWithNoMessageSeriesIdentifierIsRefusedWithSomethingSayable() {
        assertThatThrownBy(() -> S124DatasetReader.toMRN(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageSeriesIdentifier");
    }

    @Test
    void aReferenceThatNamesNoWarningIsLeftOutInsteadOfCostingTheWholeDataset() {
        // The upload names its own dataset and then looks up every warning it references. A reference that is not an
        // MRN matches nothing in the store, which is what it did before the name was checked at all, so it is skipped
        // rather than allowed to refuse a dataset whose own name is perfectly good.
        DatasetImpl dataset = new DatasetImpl();
        DatasetImpl.MembersImpl members = new DatasetImpl.MembersImpl();
        ReferencesImpl references = new ReferencesImpl();
        references.getMessageSeriesIdentifiers().add(supplying(OBSERVED_MALFORMED));
        references.getMessageSeriesIdentifiers().add(supplying(WELL_FORMED));
        members.getNavwarnPartsAndNavwarnAreaAffectedsAndTextPlacements().add(references);
        dataset.setMembers(members);

        assertThat(S124DatasetReader.findAllReferences(dataset)).singleElement()
                .extracting(MessageSeriesIdentifierType::getInteroperabilityIdentifier).isEqualTo(WELL_FORMED);
    }

    /** {@return a message series identifier whose producer supplied this name and nothing else} */
    private static MessageSeriesIdentifierType supplying(String interoperabilityIdentifier) {
        MessageSeriesIdentifierTypeImpl identifier = new MessageSeriesIdentifierTypeImpl();
        identifier.setInteroperabilityIdentifier(interoperabilityIdentifier);
        return identifier;
    }
}
