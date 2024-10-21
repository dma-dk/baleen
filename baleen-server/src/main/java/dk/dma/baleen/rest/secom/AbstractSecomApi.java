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

import dk.dma.baleen.util.MyAppConfig;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;

/**
 *
 */
public abstract class AbstractSecomApi {

    static final String SECOM_ROOT_PATH = "secom";

    @Inject
    HttpHeaders httpHeaders;

    @Inject
    MyAppConfig myAppConfig;

    @Inject
    SecurityIdentity request;

    protected final MRNClient mrn() {
//        System.out.println("------------- HEADERS API------------");
//        ExtractMRNRequestFilter.printHeaders(httpHeaders.getRequestHeaders());
//        System.out.println("------------- HEADERS API ------------");
//
//        System.out.println(httpHeaders.getRequestHeader("X-MRN"));
//        System.out.println("A");
//        System.out.println(httpHeaders.getRequestHeader("X-MRN").size());
//        System.out.println("B");
        String mrn = httpHeaders.getRequestHeader("X-MRN").get(0);
//        System.out.println("C " + mrn);

        String forceHost = null;
        if (mrn != null) {
            for (MyAppConfig.ItemConfig item : myAppConfig.forcecallback()) {
//                System.out.println("ITM " + item.mrn() + " " + item.host());
//                System.out.println(mrn);
//                System.out.println(item.mrn());
                if (item.mrn().equals(mrn)) {
                    forceHost = item.host();
                    System.out.println("Dev mrn found " + mrn + " force callback at " + forceHost);
                }
                break;
            }
        }
        return new MRNClient(forceHost, mrn);
    }

    public record MRNClient(String forceHost, String mrn) {}

}
