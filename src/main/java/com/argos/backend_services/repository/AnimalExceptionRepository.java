package com.argos.backend_services.repository;

import com.argos.backend_services.entity.AnimalException;
import com.argos.backend_services.entity.Lot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnimalExceptionRepository extends JpaRepository<AnimalException, UUID> {
    List<AnimalException> findByLotId(Lot lotId);

    @EntityGraph(attributePaths = {
            "lotId", "lotId.herdId", "lotId.herdId.propertyId",
            "originLotId", "originLotId.herdId", "originLotId.herdId.propertyId"
    })
    Optional<AnimalException> findWithLotsById(UUID animalExceptionId);

    @EntityGraph(attributePaths = {
            "lotId", "lotId.herdId", "lotId.herdId.propertyId",
            "originLotId", "originLotId.herdId", "originLotId.herdId.propertyId"
    })
    @Query("""
            select ae
            from AnimalException ae
            where (:active is null
                    or (:active = true and ae.status = 'ATIVO')
                    or (:active = false and ae.status <> 'ATIVO'))
              and (:originId is null or ae.originLotId.id = :originId)
              and ae.originLotId.herdId.propertyId.id in :propertyIds
            """)
    Page<AnimalException> findAllByFilters(
            @Param("active") Boolean active,
            @Param("originId") UUID originId,
            @Param("propertyIds") Collection<UUID> propertyIds,
            Pageable pageable);
}
