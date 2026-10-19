package com.argos.backend_services.service;

import com.argos.backend_services.entity.HerdReport;
import com.argos.backend_services.exception.NotFoundException;
import com.argos.backend_services.exception.PropertyNotFoundException;
import com.argos.backend_services.repository.HerdReportRepository;
import com.argos.backend_services.repository.HerdRepository;
import com.argos.backend_services.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportHerdService {

    private final PropertyRepository propertyRepository;
    private final HerdRepository herdRepository;
    private final HerdReportRepository herdReportRepository;

    public HerdReport readHerd(UUID userId, UUID propertyId) throws PropertyNotFoundException {
        var property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException("Propriedade não achada por esse id" + propertyId));
        var herd = herdRepository.findByPropertyId(property);
        return (HerdReport) herdReportRepository.findByHerd(herd)
                .orElseThrow(() -> new NotFoundException("Não foi possível achar um relatório com base no rebanho"));
    }

    public void saveHerd(UUID userId, UUID propertyId) {

    }
}
