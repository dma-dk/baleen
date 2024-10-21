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
package dk.dma.baleen.rest.secom;

import java.util.List;

import org.grad.secom.core.interfaces.CapabilitySecomInterface;
import org.grad.secom.core.models.CapabilityObject;
import org.grad.secom.core.models.CapabilityResponseObject;
import org.grad.secom.core.models.ImplementedInterfaces;
import org.grad.secom.core.models.enums.ContainerTypeEnum;
import org.grad.secom.core.models.enums.SECOM_DataProductType;

import jakarta.ws.rs.Path;

/**
 *
 */
@Path(AbstractSecomApi.SECOM_ROOT_PATH)
public class SecomCapabilityApi extends AbstractSecomApi implements CapabilitySecomInterface {


    /** {@inheritDoc} */
    @Override
    public CapabilityResponseObject capability() {

        // Add supported operations
        ImplementedInterfaces implementedInterfaces = new ImplementedInterfaces();
        implementedInterfaces.setGetSummary(true);
        implementedInterfaces.setGet(true);
        implementedInterfaces.setSubscription(true);

        // We only have 1 capability object now (S124)
        CapabilityObject capabilityObject = new CapabilityObject();
        capabilityObject.setContainerType(ContainerTypeEnum.S100_DataSet);
        capabilityObject.setDataProductType(SECOM_DataProductType.S124);
        capabilityObject.setImplementedInterfaces(implementedInterfaces);
        capabilityObject.setServiceVersion("0.1.0");

        // Create the response
        CapabilityResponseObject response = new CapabilityResponseObject();
        response.setCapability(List.of(capabilityObject));
        return response;
    }

}
