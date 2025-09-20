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
package dk.dma.baleen.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class SettingEntity {

    @Id
    @Column(nullable = false, unique = true)
    private String settingKey;

    @Column
    private String settingValue;

    public String getKey() {
        return settingKey;
    }

    public void setKey(String key) {
        this.settingKey = key;
    }

    public String getValue() {
        return settingValue;
    }

    public void setValue(String value) {
        this.settingValue = value;
    }
}