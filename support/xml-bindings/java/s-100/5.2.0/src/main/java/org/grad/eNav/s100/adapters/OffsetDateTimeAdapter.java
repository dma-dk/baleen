/*
 * Copyright (c) 2024 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grad.eNav.s100.adapters;

import static org.grad.eNav.s100.adapters.DateAdapter.S100_DATE_FORMAT;
import static org.grad.eNav.s100.adapters.TimeAdapter.S100_TIME_FORMAT;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * The DateTime Adapter Class.
 * <p/>
 * This is used to translate between the Java util.Date objects and the XML dateTime elements.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class OffsetDateTimeAdapter extends XmlAdapter<String, OffsetDateTime> {

    // We should not support LocalDateTime
    public static final DateTimeFormatter S100_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(S100_DATE_FORMAT)
            .appendPattern("'T'").appendPattern(S100_TIME_FORMAT).optionalStart().appendOffset("+HHMM", "Z").parseStrict().toFormatter();

    /**
     * Marshall a Java Date object into an XML element.
     *
     * @param date
     *            The java Date object
     * @return The XML element
     */
    @Override
    public String marshal(OffsetDateTime date) {
        return S100_DATE_TIME_FORMATTER.format(date);
    }

    /**
     * Unmarshall an XML element into a Java Date object.
     *
     * @param xml
     *            The XML element
     * @return The Java Date object
     */
    @Override
    public OffsetDateTime unmarshal(String xml) {
        return OffsetDateTime.parse(xml, S100_DATE_TIME_FORMATTER);
    }
}