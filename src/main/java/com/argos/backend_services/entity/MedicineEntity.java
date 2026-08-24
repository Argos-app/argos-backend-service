package com.argos.backend_services.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
@Builder
public class MedicineEntity {

    @Id
    String id;
}
