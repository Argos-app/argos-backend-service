package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.entity.Lot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LotRepository extends JpaRepository<Lot, UUID> {
    List<Lot> findByHerdId(Herd HerdId);

    Optional<Lot> findByLotCode(String lotCode);

    @EntityGraph(attributePaths = {"herdId", "herdId.propertyId"})
    Optional<Lot> findWithHerdAndPropertyByLotCode(String lotCode);

    @EntityGraph(attributePaths = {"herdId", "herdId.propertyId"})
    List<Lot> findByActiveTrueAndHerdId_PropertyId_IdIn(List<UUID> propertyIds);

    @EntityGraph(attributePaths = {"herdId", "herdId.propertyId"})
    Optional<Lot> findWithHerdAndPropertyById(UUID lotId);
}
