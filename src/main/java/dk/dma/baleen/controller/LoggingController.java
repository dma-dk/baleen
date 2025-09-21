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

package dk.dma.baleen.controller;

import dk.dma.baleen.logging.InMemoryLogAppender;
import dk.dma.baleen.logging.InMemoryLogAppender.LogEntry;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class LoggingController {

    @GetMapping
    public List<LogEntry> getLogs(@RequestParam(defaultValue = "1000") int limit) {
        // Return recent logs up to the specified limit
        return InMemoryLogAppender.getRecentLogs(limit);
    }

    @DeleteMapping
    public Map<String, String> clearLogs() {
        InMemoryLogAppender.clearLogs();
        return Map.of("message", "Logs cleared successfully");
    }

    @GetMapping("/count")
    public Map<String, Object> getLogCount() {
        return Map.of("count", InMemoryLogAppender.getLogCount());
    }
}