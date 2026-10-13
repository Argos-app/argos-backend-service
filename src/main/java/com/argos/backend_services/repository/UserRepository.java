package com.argos.backend_services.repository;

import com.argos.backend_services.dto.out.UserListItemOut;
import com.argos.backend_services.dto.out.UserAccessOut;
import com.argos.backend_services.dto.out.CurrentUserAccessOut;
import com.argos.backend_services.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmail(String email);

    @Query("""
            select u from AppUser u join fetch u.permission permission
            where u.id = :userId and u.active = true and permission.active = true
            """)
    Optional<AppUser> findActiveProfileById(@Param("userId") UUID userId);

    boolean existsByCpf(String cpf);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByCpfAndIdNot(String cpf, UUID id);

    @Query("""
            select new com.argos.backend_services.dto.out.UserAccessOut(
                u.id, u.photoUrl, u.name, u.email, u.active, permission.id, permission.name
            )
            from AppUser u
            join u.permission permission
            where exists (
                select up.user.id from UserProperty up
                where up.user = u and up.active = true and up.property.active = true
                  and up.property.user.id = :administratorId
            )
            order by u.name asc, u.id asc
            """)
    Page<UserAccessOut> findUserAccessesByAdministrator(@Param("administratorId") UUID administratorId,
                                                       Pageable pageable);

    @Query("""
            select u.active as userActive,
                   permission.active as permissionActive,
                   permission.code as permissionCode
            from AppUser u
            join u.permission permission
            where u.id = :userId
            """)
    Optional<CurrentUserAccessOut> findCurrentAccessById(@Param("userId") UUID userId);

    Page<AppUser> findByIdAndActiveTrueAndPermission_ActiveTrueAndPermission_Code(
            UUID id, String permissionCode, Pageable pageable);

    @Query("""
            select u from AppUser u
            where u.id = :userId and exists (
                select up.user.id from UserProperty up
                where up.user = u and up.active = true and up.property.active = true
                  and up.property.user.id = :administratorId
            )
            """)
    Optional<AppUser> findManagedUserById(@Param("userId") UUID userId,
                                         @Param("administratorId") UUID administratorId);

    @Query("""
            select new com.argos.backend_services.dto.out.UserListItemOut(
                u.id,
                u.photoUrl,
                u.name,
                u.cpf,
                u.email,
                u.active,
                u.permission.id
            )
            from AppUser u
            where exists (
                select up.user.id from UserProperty up
                where up.user = u and up.active = true and up.property.active = true
                  and up.property.user.id = :administratorId
            )
            order by u.name asc, u.id asc
            """)
    Page<UserListItemOut> findLinkedByAdministrator(@Param("administratorId") UUID administratorId,
                                                    Pageable pageable);

}
