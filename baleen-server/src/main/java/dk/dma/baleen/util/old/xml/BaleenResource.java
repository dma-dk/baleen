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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/")
public class BaleenResource {

    private static final String VALID_TOKEN = "BaleenIsGreat";
    private static final ConcurrentHashMap<String, String> xmlDocuments = new ConcurrentHashMap<>();

    @Inject
    EventResource eventResource;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response home() {
        String response = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Baleen</title>
                <style>
                    body { /* your styles here */ }
                </style>
            </head>
            <body>
                <!-- Your HTML content here -->
                <div id="timestamps">
                    <h2>S-124 Documents</h2>
                    <div id="timestamp-list"></div>
                </div>
                <div id="xml-content">
                    <textarea id="xml-textarea" readonly placeholder="S-124 content will appear here"></textarea>
                </div>
                <script>
                    const timestampsDiv = document.getElementById('timestamp-list');
                    const xmlTextarea = document.getElementById('xml-textarea');

                    const eventSource = new EventSource('/events');
                    eventSource.onmessage = function(event) {
                        const data = JSON.parse(event.data);
                        const button = document.createElement('button');
                        button.textContent = data.timestamp;
                        button.onclick = function() {
                            xmlTextarea.value = data.xml;
                            // Highlight the selected timestamp
                            document.querySelectorAll('#timestamp-list button').forEach(btn => btn.style.backgroundColor = '');
                            this.style.backgroundColor = '#3498db';
                            this.style.color = '#ffffff';
                        };
                        timestampsDiv.insertBefore(button, timestampsDiv.firstChild);
                        xmlTextarea.value = data.xml;
                        // Highlight the newest timestamp
                        button.click();
                    };
                </script>
            </body>
            </html>
        """;

        return Response.ok(response).build();
    }

    @POST
    @Path("/submit")
    @Consumes(MediaType.APPLICATION_XML)
    public Response submit(@HeaderParam("X-Auth-Token") String authToken, String xmlContent) {
        if (authToken == null || !authToken.equals(VALID_TOKEN)) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Unauthorized: Invalid or missing token").build();
        }

        if (isValidXML(xmlContent)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            xmlDocuments.put(timestamp, xmlContent);

            eventResource.broadcastUpdate(timestamp, xmlContent);

            return Response.ok("XML document received and stored.").build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid XML document.").build();
        }
    }

    private boolean isValidXML(String xml) {
        return xml.trim().startsWith("<?xml") || xml.trim().startsWith("<");
    }

    public Map<String, String> getXmlDocuments() {
        return xmlDocuments;
    }
}

