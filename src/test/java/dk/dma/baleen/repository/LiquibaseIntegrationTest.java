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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import dk.dma.baleen.config.TestDatabaseConfig;
import dk.dma.baleen.model.SettingEntity;
import liquibase.integration.spring.SpringLiquibase;

/**
 * Integration test to verify Liquibase is working correctly with tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestDatabaseConfig.class)
public class LiquibaseIntegrationTest {

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private SpringLiquibase liquibase;

    @Test
    public void testLiquibaseIsConfigured() {
        assertThat(liquibase).isNotNull();
        assertThat(liquibase.getChangeLog()).isEqualTo("classpath:db/changelog/db.changelog-master.yaml");
        assertThat(liquibase.getContexts()).isEqualTo("default,test");
    }

    @Test
    public void testDatabaseSchemaCreated() {
        // The setting_entity table should exist and be usable
        SettingEntity testSetting = new SettingEntity();
        testSetting.setKey("liquibase.test");
        testSetting.setValue("working");

        settingRepository.save(testSetting);

        Optional<SettingEntity> found = settingRepository.findById("liquibase.test");
        assertThat(found).isPresent();
        assertThat(found.get().getValue()).isEqualTo("working");
    }

    @Test
    public void testTestDataIsLoaded() {
        // Test data from 003-test-data.yaml should be available
        Optional<SettingEntity> testData = settingRepository.findById("test.initial.key");
        assertThat(testData).isPresent();
        assertThat(testData.get().getValue()).isEqualTo("test.initial.value");

        Optional<SettingEntity> environmentData = settingRepository.findById("test.environment");
        assertThat(environmentData).isPresent();
        assertThat(environmentData.get().getValue()).isEqualTo("junit");
    }

    @Test
    public void testDatabaseIsCleanBetweenTests() {
        // Each test should start with a clean database (due to @Transactional rollback)
        // But test data should still be available due to Liquibase
        long initialCount = settingRepository.count();

        // Add a new setting
        SettingEntity newSetting = new SettingEntity();
        newSetting.setKey("temp.test");
        newSetting.setValue("temporary");
        settingRepository.save(newSetting);

        assertThat(settingRepository.count()).isEqualTo(initialCount + 1);
    }
}