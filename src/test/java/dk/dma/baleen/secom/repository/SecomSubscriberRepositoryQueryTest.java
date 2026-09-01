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
package dk.dma.baleen.secom.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import dk.dma.baleen.secom.model.SecomSubscriberEntity;

/**
 * The publication query decides who gets sent every navigational warning we publish, and Hibernate validates it
 * when the context starts, so a clause naming a field the entity does not have takes the whole application down
 * rather than failing one query. These assertions are on the query text, so they hold without booting anything.
 */
class SecomSubscriberRepositoryQueryTest {

    /** The columns a subscription filters on, all of them nullable and all of them meaning "no restriction". */
    private static final List<String> FILTER_FIELDS = List.of("dataProductType", "productVersion", "dataReference",
            "subscriptionStart", "subscriptionEnd");

    /** Matches an entity path such as {@code s.subscriptionEnd} in the query. */
    private static final Pattern ENTITY_PATH = Pattern.compile("\\bs\\.([a-zA-Z][a-zA-Z0-9]*)");

    private static final String QUERY = findActiveSubscribersQuery();

    private static String findActiveSubscribersQuery() {
        for (Method method : SecomSubscriberRepository.class.getDeclaredMethods()) {
            if (method.getName().equals("findActiveSubscribers")) {
                Query query = method.getAnnotation(Query.class);
                if (query == null) {
                    throw new AssertionError("findActiveSubscribers is no longer an annotated query");
                }
                return query.value();
            }
        }
        throw new AssertionError("SecomSubscriberRepository no longer declares findActiveSubscribers");
    }

    /**
     * Restoring a clause on a field that was renamed or never existed - {@code isActive} is the one the old
     * commented out query named - fails at context startup, not at query time.
     */
    @Test
    void everyPathInTheQueryResolvesToAFieldOnTheEntity() {
        Set<String> declared = new LinkedHashSet<>();
        for (Field field : SecomSubscriberEntity.class.getDeclaredFields()) {
            declared.add(field.getName());
        }

        Set<String> referenced = new LinkedHashSet<>();
        Matcher matcher = ENTITY_PATH.matcher(QUERY);
        while (matcher.find()) {
            referenced.add(matcher.group(1));
        }

        assertThat(referenced).isNotEmpty();
        assertThat(declared).containsAll(referenced);
    }

    /** Without these the query returns every subscriber for every publication, whatever they subscribed to. */
    @Test
    void everyFilterDimensionIsApplied() {
        assertThat(QUERY).containsIgnoringCase("where")
                .contains("s.dataProductType = :dataProductType")
                .contains("s.productVersion = :productVersion")
                .contains("s.dataReference = :dataReference")
                .contains("s.subscriptionStart <= :now")
                .contains("s.subscriptionEnd >= :now");
    }

    /**
     * A subscription that left a filter out is unrestricted on that dimension. Comparing the null column with an
     * equality yields unknown, which would deliver nothing at all - and no subscription taken through the current
     * subscribe path fills any of these in.
     */
    @Test
    void aFilterASubscriptionLeftOutDoesNotExcludeIt() {
        for (String field : FILTER_FIELDS) {
            assertThat(QUERY).as("%s must be null tolerant", field).contains("s." + field + " IS NULL OR");
        }
    }
}
