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
package dk.dma.baleen.service.s124.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import dk.baleen.s100.xmlbindings.s124.v1_0_0.utils.S124Utils;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.impl.DatasetImpl;
import dk.dma.baleen.service.s124.NiordApiCaller;
import dk.dma.baleen.service.s124.NiordApiCaller.Result;
import dk.dma.baleen.service.spi.DataSet;

/**
 * A SECOM get names the datasets it wants by data reference, by area and by period, and asks for them a page at a
 * time. Every one of those was ignored: the query returned every warning Niord had, under a fresh random data
 * reference each time.
 * <p>
 * The datasets are ones the deployed service really serves, so the coordinates and times below are the real ones - a
 * warning off Lolland, one north of Skagen and one on the east coast of Greenland.
 */
class S124ServiceFindAllTest {

    /** Off Lolland in the western Baltic, published 2026-05-06. */
    private static final String LOLLAND = "datasets/local-warning-120-26.gml";

    /** North of Skagen, published 2026-08-25. */
    private static final String SKAGEN = "datasets/nw-343-26.gml";

    /** The east coast of Greenland, published 2026-08-14. */
    private static final String GREENLAND = "datasets/gl-044-26.gml";

    private S124Service service;

    @BeforeEach
    void setUp() throws Exception {
        service = new S124Service();
        service.niordApi = mock(NiordApiCaller.class);
        when(service.niordApi.getIt()).thenReturn(List.of(result(LOLLAND), result(SKAGEN), result(GREENLAND)));
    }

    @Test
    void withoutAQueryEveryDatasetIsReturned() {
        assertThat(findAll(null, null, null, null, null)).hasSize(3);
    }

    @Test
    void aDataReferenceSelectsTheOneDatasetItNames() {
        UUID greenland = referenceOf(GREENLAND);

        Page<? extends DataSet> page = findAll(greenland, null, null, null, null);

        assertThat(references(page)).containsExactly(greenland);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    /**
     * A data reference read out of a summary has to work in the get that follows it. It used to be a fresh
     * {@code UUID.randomUUID()} on every call, so it never did.
     */
    @Test
    void aDataReferenceIsTheSameOnEveryCall() {
        List<UUID> first = references(findAll(null, null, null, null, null));
        List<UUID> second = references(findAll(null, null, null, null, null));

        assertThat(first).isEqualTo(second).doesNotHaveDuplicates();
    }

    @Test
    void anAreaOfInterestSelectsTheDatasetsInsideIt() throws ParseException {
        // The Skagerrak and the northern Kattegat, which holds the Skagen warning and neither of the others
        Page<? extends DataSet> page = findAll(null, wkt("POLYGON((8 57, 12 57, 12 59, 8 59, 8 57))"), null, null, null);

        assertThat(references(page)).containsExactly(referenceOf(SKAGEN));
    }

    @Test
    void anAreaWithNoDatasetsInItReturnsNone() throws ParseException {
        assertThat(findAll(null, wkt("POLYGON((0 0, 1 0, 1 1, 0 1, 0 0))"), null, null, null)).isEmpty();
    }

    /**
     * A period of interest that ends before a warning was published cannot include it. The two August warnings did not
     * exist in May, when the Lolland one was already in force.
     */
    @Test
    void aPeriodSelectsTheWarningsInForceDuringIt() {
        Page<? extends DataSet> page = findAll(null, null, null, Instant.parse("2026-06-01T00:00:00Z"), null);

        assertThat(references(page)).containsExactly(referenceOf(LOLLAND));
    }

    @Test
    void aPeriodEndingBeforeAWarningWasPublishedExcludesIt() {
        assertThat(findAll(null, null, null, Instant.parse("2025-12-31T23:59:59Z"), null)).isEmpty();
    }

    /** A warning still in force has no cancellation date, and an open end must not be read as an end that has passed. */
    @Test
    void aWarningStillInForceMatchesAPeriodLongAfterItWasPublished() {
        assertThat(findAll(null, null, Instant.parse("2030-01-01T00:00:00Z"), null, null)).hasSize(3);
    }

    @Test
    void thePageAskedForIsTheOneReturned() {
        Page<? extends DataSet> first = findAll(null, null, null, null, PageRequest.of(0, 2));
        Page<? extends DataSet> second = findAll(null, null, null, null, PageRequest.of(1, 2));

        assertThat(first).hasSize(2);
        assertThat(second).hasSize(1);
        assertThat(references(first)).doesNotContainAnyElementsOf(references(second));
        // The total is what matched, not what this page carries, so a client knows there is another page
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(second.getTotalElements()).isEqualTo(3);
    }

    @Test
    void aPageBeyondTheLastOneIsEmptyRatherThanAFailure() {
        Page<? extends DataSet> page = findAll(null, null, null, null, PageRequest.of(9, 2));

        assertThat(page).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void theDatasetIsServedExactlyAsNiordProducedIt() {
        Page<? extends DataSet> page = findAll(referenceOf(LOLLAND), null, null, null, null);

        assertThat(page.getContent().get(0).toByteArray()).isEqualTo(gml(LOLLAND).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * A dataset we cannot read the preamble of has no validity we can speak for. Reading that silence as "in force
     * from the beginning of time until the end of it" would put a warning that may have been cancelled long ago in
     * front of a mariner asking what is in force now.
     */
    @Test
    void aWarningWhoseValidityCannotBeReadDoesNotMatchAPeriod() throws Exception {
        withUnreadableDatasetAdded();

        assertThat(findAll(null, null, Instant.parse("2026-08-01T00:00:00Z"), null, null)).hasSize(3);
        assertThat(findAll(null, null, null, Instant.parse("2030-01-01T00:00:00Z"), null)).hasSize(3);
        assertThat(findAll(null, null, Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-09-01T00:00:00Z"), null)).hasSize(3);
    }

    /** It is still a warning, so a query that constrains nothing still gets it. */
    @Test
    void aWarningWhoseValidityCannotBeReadIsStillServedUnfiltered() throws Exception {
        withUnreadableDatasetAdded();

        assertThat(findAll(null, null, null, null, null)).hasSize(4);
    }

    /**
     * A warning that declares no publication time and no cancellation is open at both ends, which is a statement about
     * the warning rather than an absence of one, and it must still match.
     */
    @Test
    void aWarningThatDeclaresNoBoundsIsOpenEndedRatherThanUnknown() throws Exception {
        givenOnly(gml(LOLLAND).replaceAll("<ns4:publicationTime>[^<]*</ns4:publicationTime>", ""));

        assertThat(findAll(null, null, Instant.parse("1970-01-01T00:00:00Z"), null, null)).hasSize(1);
        assertThat(findAll(null, null, null, Instant.parse("2030-01-01T00:00:00Z"), null)).hasSize(1);
    }

    /**
     * Each dimension is read on its own, so a failure in one does not blind the other. The extent and the preamble
     * used to share a try block with the extent read first, so a geometry that would not parse also cost the warning
     * the validity that read perfectly well. Here it is the preamble that cannot be read, and the extent must survive
     * it.
     */
    @Test
    void anUnreadablePreambleLeavesTheExtentReadable() throws ParseException {
        String noPreamble = gml(LOLLAND).replaceAll("(?s)<ns4:NavwarnPreamble.*?</ns4:NavwarnPreamble>", "");
        givenOnly(noPreamble);

        // The extent survived: the warning is off Lolland and still answers a query naming that area
        assertThat(findAll(null, wkt("POLYGON((11 54, 12 54, 12 55, 11 55, 11 54))"), null, null, null)).hasSize(1);
        assertThat(findAll(null, wkt("POLYGON((0 0, 1 0, 1 1, 0 1, 0 0))"), null, null, null)).isEmpty();
        // ...while the validity did not, so a query naming a period gets nothing
        assertThat(findAll(null, null, Instant.parse("1970-01-01T00:00:00Z"), null, null)).isEmpty();
    }

    /** Makes {@code gml} the only warning Niord serves. */
    private void givenOnly(String gml) {
        try {
            when(service.niordApi.getIt()).thenReturn(List.of(new Result(gml, S124Utils.unmarshallS124(gml))));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Adds a dataset with no preamble, which is what {@code findPreamble} refuses to read. */
    private void withUnreadableDatasetAdded() throws Exception {
        DatasetImpl unreadable = new DatasetImpl();
        unreadable.setId("unreadable");
        when(service.niordApi.getIt())
                .thenReturn(List.of(result(LOLLAND), result(SKAGEN), result(GREENLAND), new Result("<Dataset/>", unreadable)));
    }

    private Page<? extends DataSet> findAll(UUID uuid, Geometry geometry, Instant from, Instant to, Pageable pageable) {
        return service.findAll(uuid, geometry, from, to, pageable);
    }

    private static List<UUID> references(Page<? extends DataSet> page) {
        return page.getContent().stream().map(DataSet::uuid).toList();
    }

    /** {@return the data reference the service gives the warning in {@code resource}} */
    private UUID referenceOf(String resource) {
        byte[] gml = gml(resource).getBytes(StandardCharsets.UTF_8);
        return findAll(null, null, null, null, null).getContent().stream()
                .filter(ds -> java.util.Arrays.equals(ds.toByteArray(), gml))
                .findFirst()
                .orElseThrow()
                .uuid();
    }

    private static Geometry wkt(String wkt) throws ParseException {
        return new WKTReader().read(wkt);
    }

    private static Result result(String resource) throws Exception {
        String gml = gml(resource);
        return new Result(gml, S124Utils.unmarshallS124(gml));
    }

    private static String gml(String resource) {
        try (InputStream in = S124ServiceFindAllTest.class.getClassLoader().getResourceAsStream(resource)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
