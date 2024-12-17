package dk.dma.baleen.transport.secom.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;

import org.grad.secom.core.models.enums.NackTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import dk.dma.baleen.connector.secom.model.SecomNodeEntity;
import dk.dma.baleen.connector.secom.model.SecomTransactionalUploadLinkEntity;
import dk.dma.baleen.connector.secom.repository.SecomNodeRepository;
import dk.dma.baleen.connector.secom.repository.SecomUploadedLinkRepository;

@EnableJpaRepositories(basePackageClasses = SecomNodeRepository.class)
@EntityScan(basePackageClasses = SecomNodeEntity.class)
@DataJpaTest
@ActiveProfiles("test")
public class SecomTransactionRepositoryTest {

    @Autowired
    private SecomUploadedLinkRepository repository;

    @Test
    public void shouldSaveAndRetrieveTransaction() {
        // Create a new transaction
        SecomTransactionalUploadLinkEntity transaction = new SecomTransactionalUploadLinkEntity();
        transaction.setAckedAt(Instant.now());
        transaction.setOpenedAt(Instant.now());
        transaction.setError(NackTypeEnum.UNKNOWN_DATA_TYPE_OR_VERSION); // Replace with your actual enum value
        transaction.setErrorAt(Instant.now());

        // Save the transaction
        SecomTransactionalUploadLinkEntity savedTransaction = repository.save(transaction);

        // Verify the transaction was saved with an ID
        assertThat(savedTransaction.getTransactionIdentifier()).isNotNull();

        // Retrieve the transaction
        Optional<SecomTransactionalUploadLinkEntity> retrievedTransaction = repository.findById(savedTransaction.getTransactionIdentifier());

        // Verify the retrieved transaction matches the saved one
        assertThat(retrievedTransaction).isPresent().get().satisfies(trans -> {
            assertThat(trans.getTransactionIdentifier()).isEqualTo(savedTransaction.getTransactionIdentifier());
            assertThat(trans.getAckedAt()).isEqualTo(savedTransaction.getAckedAt());
            assertThat(trans.getOpenedAt()).isEqualTo(savedTransaction.getOpenedAt());
            assertThat(trans.getError()).isEqualTo(savedTransaction.getError());
            assertThat(trans.getErrorAt()).isEqualTo(savedTransaction.getErrorAt());
        });
    }
}
