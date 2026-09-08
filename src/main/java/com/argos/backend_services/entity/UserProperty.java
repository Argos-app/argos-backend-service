package com.argos.backend_services.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "user_property",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "property_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserPropertyId.class)
public class UserProperty {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "link_date", nullable = false)
    private LocalDate linkDate = LocalDate.now();

    @Column(name = "unlink_date")
    private LocalDate unlinkDate;

    @Column(nullable = false)
    private Boolean active = true;
}
