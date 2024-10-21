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
package dk.dma.baleen.rest.beleen;

import java.util.Base64;

import dk.dma.baleen.service.secom.SecomSubscriptionServiceV1;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

/**
 *
 */
@Path("/api")
public class BaleenUploadApi {

    // signature + publicKey is base 64 encoded

    // Think should return something, ADDED, Already_exist_did_nothing, UPDATE Not supported
    // We do not support updates

    @Inject
    SecomSubscriptionServiceV1 service;


    @POST
    @Path("upload")
    @Blocking
    public String accept(String xmlDataset) {
        System.out.println("Recieved " + xmlDataset);
        service.publish(xmlDataset);
        return "ok";
    }

    @SuppressWarnings("unused")
    @POST
    @Path("uploadsigned")
    public void accept(@QueryParam("xmlDataset") String xmlDataset, @QueryParam("signature") String signature,
            @QueryParam("signatureAlgorithm") String signatureAlgorithm, @QueryParam("certificate") String certificate) throws Exception {
        if (xmlDataset == null) {
            throw new NullPointerException();
        }

        if (signature != null || signatureAlgorithm != null || certificate != null) {

        }

        if (signature == null && signatureAlgorithm == null && certificate == null) {
            // no signature
        } else if (signature == null || signatureAlgorithm == null || certificate == null) {
            throw new BadRequestException("If uploading a signed document, both signature, signatureAlgorithm and certificate must be sent");
        } else {
            byte[] decodedSignature = Base64.getDecoder().decode(signature);
            byte[] decodedCertificate = Base64.getDecoder().decode(certificate);
//
//            Signature validate = Signature.getInstance(“SHA384withECDSA”);
//            validate.initVerify(keyPair.getPublic());
//            validate.update(message.getBytes());
//            boolean result = validate.verify(signature);
        }

        // Validate message if signature and key

        // Check if insists. Probably should also take consistancy of IDs
        // put in database
        // Notify subscribers
    }

}
