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

import java.nio.charset.StandardCharsets;

import org.grad.secom.core.interfaces.UploadSecomInterface;
import org.grad.secom.core.models.UploadObject;
import org.grad.secom.core.models.UploadResponseObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.common.annotation.Blocking;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;

/**
 * Implementation of the SECOM Upload Interface. This implementation directly pushes new messages to the frontend via WebSockets.
 */
@Path(AbstractSecomApi.SECOM_ROOT_PATH)
public class SecomUploadApi extends AbstractSecomApi implements UploadSecomInterface {

    private Logger logger = LoggerFactory.getLogger(SecomUploadApi.class);

    @Blocking
    @Override
    public UploadResponseObject upload(@Valid UploadObject uploadObject) {
        try {
            logger.info("Received upload object!");
            String gml = new String(uploadObject.getEnvelope().getData(), StandardCharsets.UTF_8);
            logger.info(gml);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return new UploadResponseObject();
    }
}
