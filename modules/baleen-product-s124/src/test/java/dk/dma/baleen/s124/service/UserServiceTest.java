package dk.dma.baleen.s124.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.FileCopyUtils;

import dk.dma.baleen.connector.secom.controllers.SecomCoreController;
import dk.dma.baleen.connector.secom.controllers.SecomSubscriptionController;
import dk.dma.baleen.connector.secom.security.MCPSecurityService;
import dk.dma.baleen.connector.secom.service.SecomCoreService;
import dk.dma.baleen.connector.secom.service.SecomServiceRegistryService;
import dk.dma.baleen.connector.secom.serviceold.SecomSubscriberService;
import dk.dma.baleen.connector.secom.util.MRNToUUID;
import dk.dma.baleen.product.dto.DatasetUploadGmlDto;
import dk.dma.baleen.product.s124.model.S124DatasetInstanceEntity;
import dk.dma.baleen.product.s124.service.S124Service;
import dk.dma.baleen.product.spi.DataSet;

@SpringBootTest
@SpringBootApplication(scanBasePackages = { "dk.dma.baleen", "internal.dk.dma.baleen" })
@EnableScheduling
@ConfigurationProperties
@EnableConfigurationProperties
@EnableJpaRepositories(basePackages = { "dk.dma.baleen" })
@EntityScan(basePackages = "dk.dma.baleen") // Add this
@ActiveProfiles("test")
@EnableJpaAuditing
public class UserServiceTest {

    @MockitoBean
    private SecomServiceRegistryService secomServiceRegistryService;

    @MockitoBean
    private SecomSubscriptionController secomSubscriptionController;

    @MockitoBean
    private SecomSubscriberService secomSubscriberService;

    @MockitoBean
    private SecomCoreController secomCoreController;

    @MockitoBean
    private SecomCoreService secomCoreService;

    @MockitoBean
    private MCPSecurityService mcpSS;

    @Autowired
    S124Service service;

    @Test
    public void uploadAndTest() throws Exception {
        byte[] ds = FileCopyUtils.copyToByteArray(new ClassPathResource("datasets/datasetpoint.xml").getInputStream());
        DatasetUploadGmlDto dto = new DatasetUploadGmlDto("s-124", "1.0.0", new String(ds));
        service.upload(dto);

        UUID uuid = MRNToUUID.createUUIDFromMRN("D");

        // Find dataset from UUID
        Page<? extends DataSet> all = service.findAll(uuid, null, null, null, null);
        assertEquals(1, all.getSize());
        DataSet result = all.get().findFirst().get();
        assertEquals(uuid, result.uuid());
        assertArrayEquals(ds, result.toByteArray());

        // Find all datasets
        all = service.findAll(null, null, null, null, null);

        assertEquals(1, all.getSize());
        result = all.get().findFirst().get();
        assertEquals(uuid, result.uuid());
        assertArrayEquals(ds, result.toByteArray());

        Geometry geom = ((S124DatasetInstanceEntity) result).getGeometry();
        System.out.println("-------------");
        System.out.println(geom);
        System.out.println(GeometryHelper.pointIncluded());
        System.out.println("-------------");

        // Find 1 using geoemtry search with geometry that includes point
        all = service.findAll(null, GeometryHelper.pointIncluded(), null, null, null);

        assertEquals(1, all.getSize());
        result = all.get().findFirst().get();
        assertEquals(uuid, result.uuid());

        assertArrayEquals(ds, result.toByteArray());

        // Find 0 using geoemtry that does not includes point
        all = service.findAll(null, GeometryHelper.pointExcluded(), null, null, null);
        assertEquals(0, all.getSize());

    }
}