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

package dk.dma.baleen.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.List;
import java.util.ArrayList;

public class InMemoryLogAppender extends AppenderBase<ILoggingEvent> {

    private static final int MAX_LOGS = 10000; // Keep last 10000 log entries
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // Use a thread-safe deque to store logs
    private static final ConcurrentLinkedDeque<LogEntry> logDeque = new ConcurrentLinkedDeque<>();

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) {
            return;
        }

        LogEntry logEntry = new LogEntry(
            Instant.ofEpochMilli(event.getTimeStamp()).atZone(ZoneId.systemDefault()).format(DATE_FORMATTER),
            event.getLevel().toString(),
            event.getLoggerName(),
            event.getFormattedMessage(),
            event.getThreadName()
        );

        logDeque.addLast(logEntry);

        // Remove oldest entries if we exceed max size
        while (logDeque.size() > MAX_LOGS) {
            logDeque.removeFirst();
        }
    }

    public static List<LogEntry> getLogs() {
        return new ArrayList<>(logDeque);
    }

    public static List<LogEntry> getRecentLogs(int count) {
        List<LogEntry> result = new ArrayList<>();
        int skip = Math.max(0, logDeque.size() - count);
        int index = 0;

        for (LogEntry entry : logDeque) {
            if (index++ >= skip) {
                result.add(entry);
            }
        }

        return result;
    }

    public static void clearLogs() {
        logDeque.clear();
    }

    public static int getLogCount() {
        return logDeque.size();
    }

    public record LogEntry(String timestamp, String level, String logger, String message, String thread) {}
}