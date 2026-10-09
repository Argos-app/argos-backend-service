package com.argos.backend_services.repository;

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
            select up from UserProperty up join fetch up.property p
            where up.user.id in :userIds and up.active = true and p.active = true
              and p.user.id = :administratorId
            order by lower(p.name) asc, p.id asc
            """)
    List<UserProperty> findActiveLinksByUsersAndAdministrator(@Param("userIds") List<UUID> userIds,
                                                             @Param("administratorId") UUID administratorId);
}
