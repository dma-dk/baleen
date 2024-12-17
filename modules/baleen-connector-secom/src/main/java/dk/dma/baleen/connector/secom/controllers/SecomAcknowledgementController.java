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
package dk.dma.baleen.connector.secom.controllers;

import static java.util.Objects.requireNonNull;

import java.util.UUID;

import org.grad.secom.core.exceptions.SecomInvalidCertificateException;
import org.grad.secom.core.exceptions.SecomValidationException;
import org.grad.secom.core.interfaces.AcknowledgementSecomInterface;
import org.grad.secom.core.models.AcknowledgementObject;
import org.grad.secom.core.models.AcknowledgementResponseObject;
import org.grad.secom.core.models.EnvelopeAckObject;
import org.grad.secom.core.models.enums.AckTypeEnum;
import org.grad.secom.core.models.enums.NackTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import dk.dma.baleen.connector.secom.service.SecomTransactionalService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;

/** Implements {@link AcknowledgementSecomInterface} */
@Component
@Path("/")
@Validated
public class SecomAcknowledgementController extends AbstractSecomController implements AcknowledgementSecomInterface {

    private SecomTransactionalService transactionalService;

    @Autowired
    public SecomAcknowledgementController(SecomTransactionalService transactionalService) {
        this.transactionalService = requireNonNull(transactionalService);
    }

    /** {@inheritDoc} */
    @Override
    public AcknowledgementResponseObject acknowledgment(@Valid AcknowledgementObject ao) {
        EnvelopeAckObject envelope = check(ao.getEnvelope());
        if (envelope.getCreatedAt() == null) {
            throw new SecomInvalidCertificateException("Required attribute 'createdAt' was missing from envelop");
        } else if (envelope.getEnvelopeCertificate() == null) {
            throw new SecomInvalidCertificateException("Required attribute 'envelopeCertificate' was missing from envelop");
        } else if (envelope.getTransactionIdentifier() == null) {
            throw new SecomInvalidCertificateException("Required attribute 'transactionIdentifier' was missing from envelop");
        } else if (envelope.getAckType() == null) {
            throw new SecomInvalidCertificateException("Required attribute 'ackType' was missing from envelop");
        }

        if (envelope.getAckType() == AckTypeEnum.ERROR && envelope.getNackType() == null) {
            throw new SecomValidationException("AckType was Error, but nackType was not specified");
        }

        UUID transactionIdentifier = envelope.getTransactionIdentifier();
        AckTypeEnum ackType = envelope.getAckType();
        NackTypeEnum nackType = envelope.getNackType();

        transactionalService.acknowledgment(mrn(), envelope.getTransactionIdentifier(), envelope.getAckType(), envelope.getNackType());

        // Create the response
        AcknowledgementResponseObject response = new AcknowledgementResponseObject();
        String message = String.format("Received Acknowledgement for transaction, uuid = %s, ackType = %s%s", transactionIdentifier, ackType,
                nackType != null ? ", nackType = " + nackType : "");
        response.setMessage(message);
        return response;
    }
}
