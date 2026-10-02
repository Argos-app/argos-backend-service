package com.argos.backend_services.repository;

import com.argos.backend_services.dto.out.UserListItemOut;
import com.argos.backend_services.entity.AppUser;
import com.argos.backend_services.entity.UserProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmail(String email);

    boolean existsByCpf(String cpf);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByCpfAndIdNot(String cpf, UUID id);

    @Query("""
            select new com.argos.backend_services.dto.out.UserListItemOut(
                u.id,
                u.photoUrl,
                u.name,
                u.cpf,
                u.email,
                u.active,
                p.name,
                p.cnpj,
                up.linkDate,
                p.id,
                u.permission.id
            )
            from UserProperty up
            inner join up.user u
            inner join up.property p
            where up.active = true
            order by u.name asc, up.linkDate desc
            """)
    Page<UserListItemOut> findAllLinked(Pageable pageable);

}
