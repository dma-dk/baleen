package dk.dma.baleen.connector.secom.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dk.dma.baleen.connector.secom.model.SecomTransactionalUploadLinkEntity;

@Repository
public interface SecomUploadedLinkRepository extends JpaRepository<SecomTransactionalUploadLinkEntity, UUID> {

    @Modifying
    @Query("DELETE FROM SecomTransactionalUploadLinkEntity e WHERE e.expiresAt < :now")
    void deleteAllExpired(@Param("now") Instant now);
}
