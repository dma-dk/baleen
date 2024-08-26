package baleen;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class BaleenServer {

    private static final int PORT = 80;
    private static final ConcurrentHashMap<String, String> xmlDocuments = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new HomeHandler());
        server.createContext("/submit", new SubmitHandler());
        server.createContext("/events", new SSEHandler());
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("Server started on port " + PORT);
    }

    static class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>Baleen</title>
                        <style>
                            body {
                                display: flex;
                                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                                margin: 0;
                                padding: 0;
                                background-color: #f0f4f8;
                                color: #333;
                            }
                            #timestamps {
                                width: 250px;
                                height: 100vh;
                                overflow-y: auto;
                                background-color: #ffffff;
                                box-shadow: 2px 0 5px rgba(0,0,0,0.1);
                                padding: 20px;
                            }
                            #xml-content {
                                flex-grow: 1;
                                height: 100vh;
                                padding: 20px;
                            }
                            textarea {
                                width: 100%;
                                height: calc(100vh - 40px);
                                resize: none;
                                border: none;
                                padding: 15px;
                                box-sizing: border-box;
                                font-family: 'Courier New', Courier, monospace;
                                font-size: 14px;
                                line-height: 1.6;
                                background-color: #ffffff;
                                border-radius: 5px;
                                box-shadow: 0 2px 5px rgba(0,0,0,0.1);
                            }
                            button {
                                display: block;
                                width: 100%;
                                text-align: left;
                                margin-bottom: 10px;
                                padding: 10px 15px;
                                border: none;
                                background-color: #e1e8ed;
                                color: #333;
                                cursor: pointer;
                                border-radius: 5px;
                                transition: all 0.3s ease;
                                font-size: 14px;
                            }
                            button:hover {
                                background-color: #bdc3c7;
                                transform: translateY(-2px);
                                box-shadow: 0 2px 5px rgba(0,0,0,0.2);
                            }
                            button::before {
                                content: '🕒 ';
                                margin-right: 5px;
                            }
                            h2 {
                                margin-top: 0;
                                margin-bottom: 20px;
                                color: #2c3e50;
                            }
                        </style>
                    </head>
                    <body>
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

                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } else {
                String response = "Method not allowed";
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(405, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        }
    }

    static class SubmitHandler implements HttpHandler {
        private static final String VALID_TOKEN = "BaleenIsGreat";

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if ("POST".equals(exchange.getRequestMethod())) {
                    // Check for the token in the header
                    String authToken = exchange.getRequestHeaders().getFirst("X-Auth-Token");
                    if (authToken == null || !authToken.equals(VALID_TOKEN)) {
                        sendResponse(exchange, 401, "Unauthorized: Invalid or missing token");
                        return;
                    }

                    // Read the entire request body
                    byte[] requestBody = exchange.getRequestBody().readAllBytes();
                    String xmlContent = new String(requestBody, StandardCharsets.UTF_8);

                    if (isValidXML(xmlContent)) {
                        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        xmlDocuments.put(timestamp, xmlContent);

                        SSEHandler.broadcastUpdate(timestamp, xmlContent);

                        sendResponse(exchange, 200, "XML document received and stored.");
                    } else {
                        sendResponse(exchange, 400, "Invalid XML document.");
                    }
                } else {
                    sendResponse(exchange, 405, "Method not allowed.");
                }
            } finally {
                exchange.close();
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }

        private boolean isValidXML(String xml) {
            // This is a very basic check. For production, use a proper XML parser.
            return xml.trim().startsWith("<?xml") || xml.trim().startsWith("<");
        }
    }

    static class SSEHandler implements HttpHandler {
        private static final ConcurrentHashMap<String, OutputStream> clients = new ConcurrentHashMap<>();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().add("Cache-Control", "no-cache");
            exchange.getResponseHeaders().add("Connection", "keep-alive");
            exchange.sendResponseHeaders(200, 0);

            OutputStream os = exchange.getResponseBody();
            String clientId = String.valueOf(System.nanoTime());
            clients.put(clientId, os);

            try {
                // Keep the connection open
                while (true) {
                    Thread.sleep(10000);
                    os.write(":\n\n".getBytes());
                    os.flush();
                }
            } catch (Exception e) {
                clients.remove(clientId);
                exchange.close();
            }
        }

        static void broadcastUpdate(String timestamp, String xml) {
            String message = String.format("data: {\"timestamp\": \"%s\", \"xml\": %s}\n\n",
                    timestamp, escapeJsonString(xml));
            clients.values().forEach(os -> {
                try {
                    os.write(message.getBytes());
                    os.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        private static String escapeJsonString(String input) {
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
}