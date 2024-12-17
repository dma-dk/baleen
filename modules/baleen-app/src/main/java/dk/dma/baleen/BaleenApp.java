package dk.dma.baleen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = { "dk.dma.baleen", "internal.dk.dma.baleen" })
@EnableScheduling
@ConfigurationProperties
@EnableConfigurationProperties
@EnableJpaRepositories(basePackages = "dk.dma.baleen")
@EntityScan(basePackages = "dk.dma.baleen")
@EnableJpaAuditing
public class BaleenApp {

	/**
	 * The main function to run the application.
	 *
	 * @param args 		The application arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(BaleenApp.class, args);
	}
}
