package com.argos.backend_services.service;

import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.entity.Property;
import com.argos.backend_services.repository.HerdRepository;
import com.argos.backend_services.repository.PropertyRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class HerdServiceTest {
    @Test
    void shouldMapPersistentHerdIdAndPreserveExistingFields() {
        var herds = mock(HerdRepository.class);
        var properties = mock(PropertyRepository.class);
        var userId = UUID.randomUUID();
        var property = Property.builder().id(UUID.randomUUID()).build();
        var herd = Herd.builder().id(UUID.randomUUID()).propertyId(property).herdName("Rebanho")
                .breed("Nelore").headCount(200).build();
        when(properties.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(property));
        when(herds.findByPropertyId(property)).thenReturn(List.of(herd));

        var output = new HerdService(herds, properties).listHerd(userId, property.getId()).getFirst();

        assertThat(output.herdId()).isEqualTo(herd.getId());
        assertThat(output.propertyId()).isEqualTo(property.getId());
        assertThat(output.herdName()).isEqualTo("Rebanho");
        assertThat(output.breed()).isEqualTo("Nelore");
        assertThat(output.headCount()).isEqualTo(200);
    }
}
