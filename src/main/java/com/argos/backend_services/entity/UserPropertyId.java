package com.argos.backend_services.entity;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserPropertyId implements Serializable {
    private UUID user;
    private UUID property;
}
