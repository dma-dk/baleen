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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.impl.DatasetImpl;

/**
 * An exchange set is one package holding the whole result, and the factory that builds it validates the datasets as a
 * batch, stopping at the first one it will not accept. A single warning it objects to - a producer that got a coded
 * value wrong, or one with no extent for the catalogue to describe - therefore used to cost every other warning in the
 * batch its delivery, and the client got a 500 instead of the ones that were fine.
 */
class S124ExchangeSetServiceTest {

    @Test
    void aBatchThatPackagesIsBuiltInOneGo() {
        Packager packager = new Packager();

        byte[] set = S124ExchangeSetService.packageWhatCanBePackaged(datasets("a", "b", "c"), packager);

        assertThat(new String(set, StandardCharsets.UTF_8)).isEqualTo("a,b,c");
        assertThat(packager.calls).hasSize(1); // nothing is offered a second time when the batch is accepted
    }

    @Test
    void theWarningsThatCanBePackagedAreStillDelivered() {
        Packager packager = new Packager("b");

        byte[] set = S124ExchangeSetService.packageWhatCanBePackaged(datasets("a", "b", "c"), packager);

        assertThat(new String(set, StandardCharsets.UTF_8)).isEqualTo("a,c");
    }

    @Test
    void everyRejectedWarningIsLeftOutRatherThanOnlyTheFirst() {
        Packager packager = new Packager("a", "c");

        byte[] set = S124ExchangeSetService.packageWhatCanBePackaged(datasets("a", "b", "c", "d"), packager);

        assertThat(new String(set, StandardCharsets.UTF_8)).isEqualTo("b,d");
    }

    /** There is genuinely nothing to hand over, and saying so beats handing over an empty exchange set. */
    @Test
    void aBatchThatCannotBePackagedAtAllIsAnError() {
        Packager packager = new Packager("a", "b");

        assertThatThrownBy(() -> S124ExchangeSetService.packageWhatCanBePackaged(datasets("a", "b"), packager))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("None of the 2 datasets");
    }

    private static List<Dataset> datasets(String... ids) {
        List<Dataset> datasets = new ArrayList<>();
        for (String id : ids) {
            Dataset dataset = new DatasetImpl();
            dataset.setId(id);
            datasets.add(dataset);
        }
        return datasets;
    }

    /** Stands in for the exchange set factory: packages the ids it is given, and refuses the ones it was told to. */
    private static final class Packager implements Function<List<Dataset>, byte[]> {

        private final List<String> rejected;

        private final List<List<String>> calls = new ArrayList<>();

        Packager(String... rejected) {
            this.rejected = List.of(rejected);
        }

        @Override
        public byte[] apply(List<Dataset> datasets) {
            List<String> ids = datasets.stream().map(Dataset::getId).toList();
            calls.add(ids);
            ids.stream().filter(rejected::contains).findFirst().ifPresent(id -> {
                throw new IllegalStateException("cannot package " + id);
            });
            return String.join(",", ids).getBytes(StandardCharsets.UTF_8);
        }
    }
}
