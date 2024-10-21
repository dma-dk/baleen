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

import org.grad.secom.core.exceptions.SecomInvalidCertificateException;
import org.grad.secom.core.exceptions.SecomSignatureVerificationException;
import org.grad.secom.core.exceptions.SecomValidationException;
import org.grad.secom.core.interfaces.AcknowledgementSecomInterface;
import org.grad.secom.core.models.AcknowledgementObject;
import org.grad.secom.core.models.AcknowledgementResponseObject;
import org.grad.secom.core.models.EnvelopeAckObject;
import org.jboss.logging.Logger;

import jakarta.validation.Valid;
import jakarta.ws.rs.Path;

/**
 *
 */
@Path(AbstractSecomApi.SECOM_ROOT_PATH)
public class SecomAcknowledgementApi extends AbstractSecomApi implements AcknowledgementSecomInterface {

    private static final Logger LOG = Logger.getLogger(SecomAcknowledgementApi.class);

    /** {@inheritDoc} */
    @Override
    public AcknowledgementResponseObject acknowledgment(@Valid AcknowledgementObject ao) {
        // Handle errors - dummy field check
        if (ao == null || ao.getEnvelope() == null || ao.getEnvelope().getTransactionIdentifier() == null) {
            throw new SecomValidationException("No valid transaction identifier provided");
        } else if (ao.getEnvelopeSignature() == null) {
            throw new SecomSignatureVerificationException("No valid signature provided");
        } else if (ao.getEnvelope().getEnvelopeSignatureCertificate() == null) {
            throw new SecomInvalidCertificateException("No valid certificate provided");
        }

        // We might actually be in the process of still sending the upload
        EnvelopeAckObject e = ao.getEnvelope();
        if (e != null) {
            LOG.debugv("Acknowledgement of type {} for transaction {} received at {}", e.getAckType(), e.getTransactionIdentifier(), e.getCreatedAt());
        }

        // Create the response
        AcknowledgementResponseObject responce = new AcknowledgementResponseObject();
        responce.setMessage(String.format("Successfully received ACK for %s", ao.getEnvelope().getTransactionIdentifier()));
        responce.setSECOM_ResponseCode(null);

        // Return the response
        return responce;
    }

}
