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
package dk.dma.baleen.secom.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.grad.secom.core.models.CapabilityObject;
import org.springframework.stereotype.Service;

import dk.dma.baleen.service.spi.S100DataProductService;
import dk.dma.baleen.service.spi.S100DataProductType;

/**
 *
 */
@Service
public class S100DataProductManager {

    private final List<S100DataProductService> products; // Spring will inject all Base implementations

    final Map<S100DataProductType, S100DataProductService> productMap;

    final SortedSet<S100DataProductType> supportedProducts;

    S100DataProductManager(List<S100DataProductService> implementations) {
        this.products = List.copyOf(implementations); // make immutable
        productMap = implementations.stream().collect(Collectors.toMap(f -> f.type, f -> f));
        this.supportedProducts = Collections.unmodifiableSortedSet(new TreeSet<>(productMap.keySet()));

    }

    public List<CapabilityObject> allCapabilities() {
        System.out.println("Have " + products.size());

        return products.stream().flatMap(product -> product.secomCapabilities().stream()).toList();
    }

    public List<S100DataProductService> products() {
        return products;
    }

    public Optional<S100DataProductService> find(String name) {
        return find(S100DataProductType.valueOf(name));
    }

    public Optional<S100DataProductService> find(S100DataProductType dataProductType) {
        return Optional.of(productMap.get(dataProductType));
    }

    public SortedSet<S100DataProductType> supportedProducts() {
        return supportedProducts;
    }
}
