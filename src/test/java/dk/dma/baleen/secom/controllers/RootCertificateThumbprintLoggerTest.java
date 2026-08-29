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
package dk.dma.baleen.secom.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import org.grad.secomv2.core.base.SecomConstants;
import org.grad.secomv2.core.utils.SecomPemUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dk.dma.baleen.secom.security.MCPSecurityService;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;

/**
 * The library rejects a caller whose root certificate thumbprint is not ours without ever saying which one
 * arrived, which leaves nothing to act on. This filter supplies that, and must do so without ever being the
 * reason a request fails - it runs ahead of the signature filter and consumes the body to read it.
 */
class RootCertificateThumbprintLoggerTest {

    private ListAppender<ILoggingEvent> appender;

    private Logger logger;

    private X509Certificate root;

    private String ourThumbprint;

    @BeforeEach
    void setUp() throws Exception {
        logger = (Logger) LoggerFactory.getLogger(RootCertificateThumbprintLogger.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);

        try (InputStream in = getClass().getResourceAsStream("/secom/mcp-idreg-new.pem")) {
            root = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
        ourThumbprint = SecomPemUtils.getCertThumbprint(root, SecomConstants.CERTIFICATE_THUMBPRINT_HASH);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void reportsTheThumbprintThatWillBeRejected() throws IOException {
        ContainerRequestContext ctx = post("{\"envelope\":{\"envelopeRootCertificateThumbprint\":\"abc123\"}}");

        filter().filter(ctx);

        assertThat(warnings()).singleElement(org.assertj.core.api.InstanceOfAssertFactories.STRING)
                .contains("abc123")      // what the caller sent, which is the point of the class
                .contains(ourThumbprint) // and what we would have accepted
                .contains("/v2/object/search");
    }

    @Test
    void saysNothingWhenTheCallerAgreesWithUs() throws IOException {
        ContainerRequestContext ctx = post("{\"envelope\":{\"envelopeRootCertificateThumbprint\":\"" + ourThumbprint + "\"}}");

        filter().filter(ctx);

        assertThat(warnings()).isEmpty();
    }

    /** SECOM makes the thumbprint optional, so its absence is not a mismatch worth reporting. */
    @Test
    void saysNothingWhenTheEnvelopeCarriesNoThumbprint() throws IOException {
        filter().filter(post("{\"envelope\":{\"dataProductType\":\"S-124\"}}"));

        assertThat(warnings()).isEmpty();
    }

    @Test
    void ignoresRequestsThatCarryNoSignedBody() throws IOException {
        ContainerRequestContext ctx = mock(ContainerRequestContext.class);
        when(ctx.getMethod()).thenReturn("GET");

        filter().filter(ctx);

        assertThat(warnings()).isEmpty();
    }

    /**
     * Reading the body consumes the entity stream. If it is not put back, every signed POST to the server breaks -
     * which would be a far worse outcome than the missing diagnostic this class exists to supply.
     */
    @Test
    void leavesTheBodyReadableForTheFiltersAfterIt() throws IOException {
        String body = "{\"envelope\":{\"envelopeRootCertificateThumbprint\":\"abc123\"}}";
        ContainerRequestContext ctx = post(body);

        filter().filter(ctx);

        assertThat(new String(ctx.getEntityStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(body);
    }

    @Test
    void survivesABodyThatIsNotTheJsonItExpects() {
        assertThatCode(() -> {
            filter().filter(post("not json at all"));
            filter().filter(post(""));
            filter().filter(post("[]"));
        }).doesNotThrowAnyException();
        assertThat(warnings()).isEmpty();
    }

    private RootCertificateThumbprintLogger filter() {
        MCPSecurityService pki = mock(MCPSecurityService.class);
        when(pki.mcpRootCertificate()).thenReturn(root);
        return new RootCertificateThumbprintLogger(pki);
    }

    private List<String> warnings() {
        return appender.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }

    /** A POST to the interface the rejections have been coming from, carrying the given body. */
    private static ContainerRequestContext post(String body) {
        ContainerRequestContext ctx = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn("/v2/object/search");
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        when(ctx.getMethod()).thenReturn("POST");

        InputStream[] stream = { new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)) };
        when(ctx.getEntityStream()).thenAnswer((InvocationOnMock i) -> stream[0]);
        org.mockito.Mockito.doAnswer(i -> stream[0] = i.getArgument(0)).when(ctx).setEntityStream(org.mockito.ArgumentMatchers.any());
        return ctx;
    }
}
