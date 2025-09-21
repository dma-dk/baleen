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
package dk.dma.baleen.app;

/**
 *
 */
public interface BaleenSource {

}
// Baleen Vision
// Extendable framework.
// Can be used stand alone or embedded into an application

// Input
//// REST Upload
//// SECOM Static Subscription
//// SECOM Dynamic Subscription (Added and remove at runtime)
//// Geomesa Source
//// Custom-Database???

// Output
//// SECOM (Querying | Subscriptions)
//// REST (Querying)
//// WebSocket (Subscriptions)

// Security
//// Username/password
//// Keycloak
//// static Bearer Token
//// MCP certificates
//// Signed Dataproducts

// Storage (H2 or PostgresQL)
//// DB
//// In-Memory
//// Lucene (Caching)

// DataProducts
//// S-124