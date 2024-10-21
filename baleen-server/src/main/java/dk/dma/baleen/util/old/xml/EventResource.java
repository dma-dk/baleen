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
package dk.dma.baleen.util.old.xml;

import java.util.concurrent.LinkedBlockingQueue;

import org.jboss.resteasy.reactive.RestStreamElementType;

import io.smallrye.mutiny.Multi;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
@Path("/events")
public class EventResource {

    private final LinkedBlockingQueue<String> events = new LinkedBlockingQueue<>();

    private final Vertx vertx;

    public EventResource(Vertx vertx) {
        this.vertx = vertx;
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<String> streamEvents() {
        return Multi.createFrom().emitter(emitter -> {
            vertx.setPeriodic(50, id -> {
                String event = events.poll();
                if (event != null) {
                    emitter.emit(event);
                }
            });
        });
    }

    public void broadcastUpdate(String timestamp, String xml) {
        String message = String.format("{\"timestamp\": \"%s\", \"xml\": %s}", timestamp, escapeJsonString(xml));
        events.offer(message);
    }

    private String escapeJsonString(String input) {
        StringBuilder output = new StringBuilder("\"");
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"':
                    output.append("\\\"");
                    break;
                case '\\':
                    output.append("\\\\");
                    break;
                case '\b':
                    output.append("\\b");
                    break;
                case '\f':
                    output.append("\\f");
                    break;
                case '\n':
                    output.append("\\n");
                    break;
                case '\r':
                    output.append("\\r");
                    break;
                case '\t':
                    output.append("\\t");
                    break;
                default:
                    output.append(c);
            }
        }
        output.append("\"");
        return output.toString();
    }
}

