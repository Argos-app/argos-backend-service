package com.argos.backend_services.repository;

import com.argos.backend_services.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {
    UserCredential findByUserId(UUID userId);
}
