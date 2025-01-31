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
package dk.dma.baleen.connector.secom.util;

import java.io.IOException;
import java.net.URI;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Optional;

import org.grad.secom.core.models.ResponseSearchObject;
import org.grad.secom.core.models.SearchFilterObject;
import org.grad.secom.core.models.SubscriptionNotificationObject;
import org.grad.secom.core.models.UploadObject;
import org.grad.secom.springboot3.components.SecomClient;
import org.grad.secom.springboot3.components.SecomConfigProperties;

/**
 *
 */
public final class BaleenSecomClient2 {

    private final URI uri;

    SecomClient client;

    public BaleenSecomClient2(URI uri, SecomConfigProperties config)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        this.client = new SecomClient(uri.toURL(), config);
        this.uri = uri;
    }

    public URI uri() {
        return uri;
    }

    /**
     * @param o
     */
    public void subscriptionNotification(SubscriptionNotificationObject o) {
        client.subscriptionNotification(o);
    }

    /**
     * @param o
     */
    public void upload(UploadObject o) {
        client.upload(o);
    }

    /**
     * @param filter
     * @param i
     * @param maxValue
     * @return
     */
    public Optional<ResponseSearchObject> searchService(SearchFilterObject filter, int page, int maxValue) {
        return client.searchService(filter, page, maxValue);
    }
}
