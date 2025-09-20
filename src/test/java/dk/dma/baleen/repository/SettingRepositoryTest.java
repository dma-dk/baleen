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
package dk.dma.baleen.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import dk.dma.baleen.model.SettingEntity;
import dk.dma.baleen.config.TestDatabaseConfig;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestDatabaseConfig.class)
public class SettingRepositoryTest {

    @Autowired
    private SettingRepository settingRepository;

    @Test
    public void testSaveAndFindSetting() {
        SettingEntity setting = new SettingEntity();
        setting.setKey("test.key");
        setting.setValue("test.value");

        settingRepository.save(setting);

        Optional<SettingEntity> found = settingRepository.findById("test.key");
        assertThat(found).isPresent();
        assertThat(found.get().getKey()).isEqualTo("test.key");
        assertThat(found.get().getValue()).isEqualTo("test.value");
    }

    @Test
    public void testUpdateSetting() {
        SettingEntity setting = new SettingEntity();
        setting.setKey("update.key");
        setting.setValue("original.value");
        settingRepository.save(setting);

        setting.setValue("updated.value");
        settingRepository.save(setting);

        Optional<SettingEntity> found = settingRepository.findById("update.key");
        assertThat(found).isPresent();
        assertThat(found.get().getValue()).isEqualTo("updated.value");
    }

    @Test
    public void testDeleteSetting() {
        SettingEntity setting = new SettingEntity();
        setting.setKey("delete.key");
        setting.setValue("delete.value");
        settingRepository.save(setting);

        assertThat(settingRepository.findById("delete.key")).isPresent();

        settingRepository.deleteById("delete.key");

        assertThat(settingRepository.findById("delete.key")).isEmpty();
    }

    @Test
    public void testFindNonExistentSetting() {
        Optional<SettingEntity> found = settingRepository.findById("non.existent.key");
        assertThat(found).isEmpty();
    }

    @Test
    public void testMultipleSettings() {
        SettingEntity setting1 = new SettingEntity();
        setting1.setKey("key1");
        setting1.setValue("value1");

        SettingEntity setting2 = new SettingEntity();
        setting2.setKey("key2");
        setting2.setValue("value2");

        settingRepository.save(setting1);
        settingRepository.save(setting2);

        // Test data includes 2 pre-loaded settings, so total should be 4
        assertThat(settingRepository.count()).isEqualTo(4);
        assertThat(settingRepository.findById("key1")).isPresent();
        assertThat(settingRepository.findById("key2")).isPresent();
    }

    @Test
    public void testGetSettingValueHelper() {
        SettingEntity setting = new SettingEntity();
        setting.setKey("helper.key");
        setting.setValue("helper.value");
        settingRepository.save(setting);

        Optional<String> value = settingRepository.getSettingValue("helper.key");
        assertThat(value).isPresent();
        assertThat(value.get()).isEqualTo("helper.value");

        Optional<String> nonExistent = settingRepository.getSettingValue("non.existent");
        assertThat(nonExistent).isEmpty();
    }

    @Test
    public void testSetSettingHelper() {
        settingRepository.setSetting("new.key", "new.value");

        Optional<SettingEntity> found = settingRepository.findById("new.key");
        assertThat(found).isPresent();
        assertThat(found.get().getValue()).isEqualTo("new.value");

        settingRepository.setSetting("new.key", "updated.value");

        found = settingRepository.findById("new.key");
        assertThat(found).isPresent();
        assertThat(found.get().getValue()).isEqualTo("updated.value");
    }

    @Test
    public void testNullValue() {
        SettingEntity setting = new SettingEntity();
        setting.setKey("null.key");
        setting.setValue(null);
        settingRepository.save(setting);

        Optional<SettingEntity> found = settingRepository.findById("null.key");
        assertThat(found).isPresent();
        assertThat(found.get().getValue()).isNull();

        Optional<String> value = settingRepository.getSettingValue("null.key");
        // When value is null, getSettingValue returns empty Optional
        assertThat(value).isEmpty();
    }
}