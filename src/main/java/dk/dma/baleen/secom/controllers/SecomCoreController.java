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

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.Optional;

import org.grad.secomv2.core.interfaces.CapabilityServiceInterface;
import org.grad.secomv2.core.interfaces.PingServiceInterface;
import org.grad.secomv2.core.models.CapabilityResponseObject;
import org.grad.secomv2.core.models.PingResponseObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import dk.dma.baleen.secom.service.SecomCoreService;
import jakarta.ws.rs.Path;

/** Implements {@link CapabilityServiceInterface}. */
@Controller
@Path("/")
@Validated
public class SecomCoreController extends AbstractSecomController implements CapabilityServiceInterface , PingServiceInterface {

    private final SecomCoreService coreService;

    @Autowired
    public SecomCoreController(SecomCoreService coreService) {
        this.coreService = requireNonNull(coreService);
    }

    /** {@inheritDoc} */
    @Override
    public CapabilityResponseObject capability() {
        CapabilityResponseObject response = new CapabilityResponseObject();
        response.setCapability(coreService.capabilities());
        return response;
    }

    /** {@inheritDoc} */
    @Override
    public PingResponseObject ping() {
        Optional<Instant> t = coreService.lastInteractionTime(mrn());

        PingResponseObject o = new PingResponseObject();
        o.setTimestamp(t.orElse(null));
        return o;
    }
}
