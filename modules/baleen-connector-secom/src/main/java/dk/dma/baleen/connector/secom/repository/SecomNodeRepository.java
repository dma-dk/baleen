package dk.dma.baleen.connector.secom.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.dma.baleen.connector.secom.model.SecomNodeEntity;

@Repository
public interface SecomNodeRepository extends JpaRepository<SecomNodeEntity, UUID> {

    Optional<SecomNodeEntity> findByMrn(String mrn);

    default SecomNodeEntity findOrCreate(String mrn) {
        return findByMrn(mrn).orElseGet(() -> {
            SecomNodeEntity entity = new SecomNodeEntity();
            entity.setMrn(mrn);
            return save(entity);
        });
    }
}
