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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import reactor.netty.http.server.HttpServerRequest;

/**
 * Extracts the MRN from the header of the request
 */
@Provider
public class MRNExtractorRequestFilter implements ContainerRequestFilter {

    @Context
    UriInfo info;

    @Context
    HttpServerRequest request;
//
//    @Inject
//    RoutingContext routingContext;
//
//    @Override
//    public void filter(ContainerRequestContext context) {
////        System.out.println("------------- HEADERS ------------");
////        printHeaders(context.getHeaders());
////        System.out.println("------------- HEADERS ------------");
//
//        final String method = context.getMethod();
//        final String path = info.getPath();
//        final String address = request.remoteAddress().toString();
//
//        String headerString = context.getHeaderString("X-Secom-Cert");
//
//        String mrn = "?";
//        // Caddy sends this String, if not configured correctly
//        // {http.request.tls.client.certificate_der_base64}
//        if (headerString != null && !headerString.equals("{http.request.tls.client.certificate_der_base64}")) {
//            try {
//                // extract the certificate from header string
//                X509Certificate cert = convertToX509Certificate(headerString);
//                mrn = extractUIDFromCertificate(cert);
//                context.getHeaders().add("X-MRN", mrn);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        LOG.infof("Request %s %s from IP %s with MRN %s", method, path, address, mrn);
//
//    }

    public static void printHeaders(MultivaluedMap<String, String> headers) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();

            // Print key
            System.out.print(key + " -> " + String.join(", ", values) + "\n");
        }
    }

    static X509Certificate convertToX509Certificate(String base64Cert) throws Exception {
        // Remove any extra whitespaces or newlines from the Base64 certificate
        base64Cert = base64Cert.replaceAll("\\s+", "");

        // Decode the Base64 encoded string to get the DER-encoded bytes
        byte[] decodedBytes = Base64.getDecoder().decode(base64Cert);

        // Create a CertificateFactory for X.509 certificates
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

        // Convert the DER-encoded bytes into an X509Certificate object
        X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(decodedBytes));

        return certificate;
    }

    static String extractUIDFromCertificate(X509Certificate certificate) {
        String subjectDN = certificate.getSubjectX500Principal().getName();
        // Use a regex or a DN parser to extract the UID
        String[] dnComponents = subjectDN.split(",");
        for (String component : dnComponents) {
            component = component.trim();
            if (component.startsWith("UID=")) {
                return component.substring(4);
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {}
}