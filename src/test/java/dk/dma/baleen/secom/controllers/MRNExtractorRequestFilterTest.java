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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.grad.secomv2.core.models.ResponseObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dk.dma.baleen.secom.config.SecomAuthenticationProperties;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Nothing in Baleen verifies who a SECOM caller is - the MRN is read out of a header the server never validated.
 * That is a deliberate choice for baleen-test, so it has to be a setting that can be turned off rather than
 * something that just happens, and the log has to say which of the two the server is doing.
 */
class MRNExtractorRequestFilterTest {

    /**
     * A self signed client certificate whose subject carries {@code UID=urn:mrn:mcp:device:dma:baleen-test},
     * which is the only part of it this filter looks at. The whitespace is what the filter strips itself.
     */
    private static final String CLIENT_CERTIFICATE = """
            MIICXDCCAgOgAwIBAgIUD61R2PyBnKYPM0IMjVbLAKxTnc0wCgYIKoZIzj0EAwIwgYIxMjAwBgoJ
            kiaJk/IsZAEBDCJ1cm46bXJuOm1jcDpkZXZpY2U6ZG1hOmJhbGVlbi10ZXN0MRswGQYDVQQDDBJi
            YWxlZW4gdGVzdCBjbGllbnQxIjAgBgNVBAoMGURhbmlzaCBNYXJpdGltZSBBdXRob3JpdHkxCzAJ
            BgNVBAYTAkRLMCAXDTI2MDgzMTE5MTQwM1oYDzIxMjYwODA3MTkxNDAzWjCBgjEyMDAGCgmSJomT
            8ixkAQEMInVybjptcm46bWNwOmRldmljZTpkbWE6YmFsZWVuLXRlc3QxGzAZBgNVBAMMEmJhbGVl
            biB0ZXN0IGNsaWVudDEiMCAGA1UECgwZRGFuaXNoIE1hcml0aW1lIEF1dGhvcml0eTELMAkGA1UE
            BhMCREswWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATJ7LXv5vFsQBCT4jfXdyZBewtVKEcPehkf
            QxMq6lVuahDV2lXQeiF4NqbwAtfy+6Lw2dQNFo4F3cGyFzLE4PCTo1MwUTAdBgNVHQ4EFgQUpuxO
            2nkboaBYIx7mX71YfH+oeSwwHwYDVR0jBBgwFoAUpuxO2nkboaBYIx7mX71YfH+oeSwwDwYDVR0T
            AQH/BAUwAwEB/zAKBggqhkjOPQQDAgNHADBEAiA1x8q/6/+0LOd7ST0BQ9Usd+RkTzQUamWYW/Yi
            g2MKiwIgXFtesl2wVCAI+ZLBK+GJMTyTZ66AekOLyKZhFzeXh6o=""";

    private ListAppender<ILoggingEvent> appender;

    private Logger logger;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(MRNExtractorRequestFilter.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        request = new MockHttpServletRequest();
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    /** The default, and what is deployed today: a caller with no certificate is served all the same. */
    @Test
    void anonymousCallerIsServedWhenAnonymousAccessIsAllowed() throws IOException {
        ContainerRequestContext ctx = get(null);

        filter(true).filter(ctx);

        verify(ctx, never()).abortWith(any());
        assertThat(request.getAttribute(MRNExtractorRequestFilter.MRN_ATTRIBUTE)).isNull();
        assertThat(messages(Level.INFO)).singleElement(org.assertj.core.api.InstanceOfAssertFactories.STRING)
                .contains("anonymous")
                .contains("baleen.secom.allow-anonymous");
    }

    /** One line per anonymous request would be one line per request, since no caller is ever identified. */
    @Test
    void theAnonymousCallerIsOnlyReportedOnce() throws IOException {
        MRNExtractorRequestFilter filter = filter(true);

        filter.filter(get(null));
        filter.filter(get(null));
        filter.filter(get(null));

        assertThat(messages(Level.INFO)).hasSize(1);
        assertThat(messages(Level.DEBUG)).hasSize(2);
    }

    @Test
    void anonymousCallerIsRefusedWhenAnonymousAccessIsNotAllowed() throws IOException {
        ContainerRequestContext ctx = get(null);

        filter(false).filter(ctx);

        Response response = abortedWith(ctx);
        assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
        assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
        assertThat(response.getEntity()).isInstanceOf(ResponseObject.class);
        assertThat(((ResponseObject) response.getEntity()).getMessage()).isNotBlank();
        assertThat(request.getAttribute(MRNExtractorRequestFilter.MRN_ATTRIBUTE)).isNull();
    }

    /**
     * The proxy sending its own placeholder means the client certificate was dropped on the way here, which is
     * our misconfiguration rather than an anonymous caller, so it is reported as its own problem.
     */
    @Test
    void theCaddyPlaceholderIsReportedAsAProxyMisconfiguration() throws IOException {
        ContainerRequestContext ctx = get(MRNExtractorRequestFilter.CADDY_UNCONFIGURED_PLACEHOLDER);

        filter(true).filter(ctx);

        verify(ctx, never()).abortWith(any());
        assertThat(request.getAttribute(MRNExtractorRequestFilter.MRN_ATTRIBUTE)).isNull();
        assertThat(messages(Level.WARN)).singleElement(org.assertj.core.api.InstanceOfAssertFactories.STRING)
                .contains(MRNExtractorRequestFilter.CADDY_UNCONFIGURED_PLACEHOLDER)
                .contains("Caddy");
    }

    /** A header we cannot make sense of is a warning and an unidentified caller, never a stack trace. */
    @Test
    void anUnreadableCertificateIsWarnedAboutAndTreatedAsNoCertificate() throws IOException {
        ContainerRequestContext ctx = get("this is not a certificate");

        filter(false).filter(ctx);

        assertThat(messages(Level.WARN)).isNotEmpty();
        assertThat(abortedWith(ctx).getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
    }

    /** The MRN of a caller that did present a certificate still comes with no promise that it is verified. */
    @Test
    void theExtractedIdentityIsMarkedUnverified() throws IOException {
        ContainerRequestContext ctx = get(CLIENT_CERTIFICATE);

        filter(true).filter(ctx);

        verify(ctx, never()).abortWith(any());
        assertThat(request.getAttribute(MRNExtractorRequestFilter.MRN_ATTRIBUTE)).isEqualTo("urn:mrn:mcp:device:dma:baleen-test");
        assertThat(request.getAttribute(MRNExtractorRequestFilter.MRN_VERIFIED_ATTRIBUTE)).isEqualTo(Boolean.FALSE);
    }

    /** Everything that builds a node from an MRN alone is building an unverified one. */
    @Test
    void aNodeBuiltFromAnMrnAloneIsUnverified() {
        assertThat(new SecomNode("urn:mrn:mcp:device:dma:baleen-test").verified()).isFalse();
    }

    private MRNExtractorRequestFilter filter(boolean allowAnonymous) {
        MRNExtractorRequestFilter filter = new MRNExtractorRequestFilter(new SecomAuthenticationProperties(allowAnonymous));
        filter.req = request;
        return filter;
    }

    private static Response abortedWith(ContainerRequestContext ctx) {
        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).abortWith(captor.capture());
        return captor.getValue();
    }

    private List<String> messages(Level level) {
        return appender.list.stream().filter(e -> e.getLevel() == level).map(ILoggingEvent::getFormattedMessage).toList();
    }

    /** A GET to a SECOM interface, carrying the given value in the certificate header. */
    private static ContainerRequestContext get(String certificateHeader) {
        ContainerRequestContext ctx = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn("/v2/object");
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        when(ctx.getMethod()).thenReturn("GET");
        when(ctx.getHeaderString(MRNExtractorRequestFilter.CERTIFICATE_HEADER)).thenReturn(certificateHeader);
        return ctx;
    }
}
