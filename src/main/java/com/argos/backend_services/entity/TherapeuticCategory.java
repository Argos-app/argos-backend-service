package com.argos.backend_services.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Table(name = "therapeutic_category")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TherapeuticCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 40)
    private String name;

    @Column
    private String description;
}
