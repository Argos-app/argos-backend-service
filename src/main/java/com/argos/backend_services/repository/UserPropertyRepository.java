package com.argos.backend_services.repository;

import com.argos.backend_services.dto.out.UserProfilePropertyOut;
import com.argos.backend_services.entity.UserProperty;
import com.argos.backend_services.entity.UserPropertyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserPropertyRepository extends JpaRepository<UserProperty, UserPropertyId> {
    List<UserProperty> findAllByProperty_IdAndActiveTrue(UUID propertyId);

    @Query("""
            select new com.argos.backend_services.dto.out.UserProfilePropertyOut(p.id, p.name)
            from UserProperty up join up.property p
            where up.user.id = :userId and up.active = true and p.active = true
            order by lower(p.name) asc, p.id asc
            """)
    List<UserProfilePropertyOut> findActiveProfileProperties(@Param("userId") UUID userId);

    boolean existsByUser_IdAndProperty_IdAndActiveTrue(UUID userId, UUID propertyId);

    List<UserProperty> findByUser_IdAndActiveTrue(UUID userId);

    @Query("""
            select up from UserProperty up join fetch up.property p
            where up.user.id in :userIds and up.active = true and p.active = true
              and p.user.id = :administratorId
            order by lower(p.name) asc, p.id asc
            """)
    List<UserProperty> findActiveLinksByUsersAndAdministrator(@Param("userIds") List<UUID> userIds,
                                                             @Param("administratorId") UUID administratorId);
}
