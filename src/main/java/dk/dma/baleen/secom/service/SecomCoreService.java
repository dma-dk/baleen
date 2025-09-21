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

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.grad.secom.core.models.CapabilityObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import dk.dma.baleen.secom.model.SecomNodeEntity;
import dk.dma.baleen.secom.repository.SecomNodeRepository;
import dk.dma.baleen.secom.spi.AuthenticatedMcpNode;

/**
 * Various core SECOM services such as ping and capabilities.
 */
@Service
public class SecomCoreService {

    /** The capabilities of this node, created once. */
    final List<CapabilityObject> capabilities;

    final SecomNodeRepository nodeRepository;

    @Autowired
    public SecomCoreService(S100DataProductManager productManager, SecomNodeRepository nodeRepository) {
        this.capabilities = productManager.allCapabilities();
        this.nodeRepository = requireNonNull(nodeRepository);
    }

    /**
     * {@return the capabilities of this node}
     *
     * @see org.grad.secom.core.interfaces.CapabilitySecomInterface
     * @see org.grad.secom.springboot3.components.SecomClient#capability()
     **/
    public List<CapabilityObject> capabilities() {
        return capabilities;
    }

    /**
     * Do we update last interaction here?
     *
     * @param node
     *            the node to return last interaction time for
     * @return the last interaction with the node, or empty if no interaction
     *
     * @see org.grad.secom.core.interfaces.PingSecomInterface
     */
    @Nullable
    public Optional<Instant> lastInteractionTime(AuthenticatedMcpNode node) {
        Optional<SecomNodeEntity> e = nodeRepository.findByMrn(node.mrn());
        registerInteraction(node);
        return e.map(SecomNodeEntity::getLastInteraction);
    }

    /**
     * Register an interaction with the remote node.
     */
    void registerInteraction(AuthenticatedMcpNode node) {}
}
