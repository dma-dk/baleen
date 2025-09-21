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

package dk.dma.baleen.s124.service;

/*
@SpringBootTest
@ActiveProfiles("test")
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
    @Disabled("Temporarily disabled due to classpath issues with AuthenticatedMcpNode")
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
*/