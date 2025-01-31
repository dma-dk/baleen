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

import org.grad.secom.core.exceptions.SecomInvalidCertificateException;
import org.grad.secom.core.exceptions.SecomValidationException;
import org.grad.secom.core.models.AbstractEnvelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;

import dk.dma.baleen.connector.secom.util.UnLoCode;
import dk.dma.baleen.connector.secom.util.WKTUtils;
import jakarta.servlet.http.HttpServletRequest;

/**
 * A SECOM controller.
 */
abstract class AbstractSecomController {

    /**
     * The Request Context.
     */
    @Autowired
    HttpServletRequest httpServletRequest;

    SecomNode mrn() {
        String mrn = (String) httpServletRequest.getAttribute("X-MRN");
        return new SecomNode(mrn);
    }

    static <T extends AbstractEnvelope> T check(@Nullable T envelope) {
        if (envelope == null) {
            throw new SecomValidationException("EnvelopeAckObject was missing");
        } else if (envelope.getEnvelopeRootCertificateThumbprint() == null) {
            throw new SecomInvalidCertificateException("Required attribute 'envelopeRootCertificateThumbprint' was missing from envelop");
        } else if (envelope.getEnvelopeSignatureTime() == null) {
            throw new SecomInvalidCertificateException("Required attribute 'envelopeSignatureTime' was missing from envelop");
        }
        return envelope;
    }

    static Geometry parseGeometry(String geometry, String unlocode) {
        Geometry jtsGeometry = null;
        if (geometry != null) {
            try {
                jtsGeometry = WKTUtils.convertWKTtoGeometry(geometry);
            } catch (ParseException e) {
                throw new SecomValidationException(e.getMessage());
            }
        }

        if (unlocode != null) {
            Geometry unlo = UnLoCode.get(unlocode).map(UnLoCode::toGeometry)
                    .orElseThrow(() -> new SecomValidationException("Unknown Unlocode, unlocode='" + unlocode + "'"));
            jtsGeometry = jtsGeometry == null ? unlo : jtsGeometry.union(unlo);
        }
        return jtsGeometry;
    }

    //
//    @Autowired
//    MyAppConfig myAppConfig;
//
//    @Autowired
//    SecurityIdentity request;
//
//    protected final MRNClient mrn() {
////        System.out.println("------------- HEADERS API------------");
////        ExtractMRNRequestFilter.printHeaders(httpHeaders.getRequestHeaders());
////        System.out.println("------------- HEADERS API ------------");
////
////        System.out.println(httpHeaders.getRequestHeader("X-MRN"));
////        System.out.println("A");
////        System.out.println(httpHeaders.getRequestHeader("X-MRN").size());
////        System.out.println("B");
//        String mrn = httpHeaders.getRequestHeader("X-MRN").get(0);
////        System.out.println("C " + mrn);
//
//        String forceHost = null;
//        if (mrn != null) {
//            for (MyAppConfig.ItemConfig item : myAppConfig.forcecallback()) {
////                System.out.println("ITM " + item.mrn() + " " + item.host());
////                System.out.println(mrn);
////                System.out.println(item.mrn());
//                if (item.mrn().equals(mrn)) {
//                    forceHost = item.host();
//                    System.out.println("Dev mrn found " + mrn + " force callback at " + forceHost);
//                }
//                break;
//            }
//        }
//        return new MRNClient(forceHost, mrn);
//    }

    static <T> T requireAttribute(String name, @Nullable T value) {
        if (value == null) {
            throw new SecomValidationException("Required attribute '" + name + " was missing");
        }
        return value;
    }
}
