package dk.dma.baleen.s125.service;

import org.springframework.context.annotation.Configuration;

@Configuration
public class H2GisConfig {

//    @PostConstruct
//    public void init() {
//        // Initialize H2GIS
//        try {
//            H2GISFunctions.load();
//        } catch (Exception e) {
//            throw new RuntimeException("Could not initialize H2GIS", e);
//        }
//    }
//
//    @Bean
//    public DataSource dataSource() {
//        return new EmbeddedDatabaseBuilder()
//            .setType(EmbeddedDatabaseType.H2)
//            .addScript("classpath:org/h2gis/functions/spatial.sql")
//            .build();
//    }
}