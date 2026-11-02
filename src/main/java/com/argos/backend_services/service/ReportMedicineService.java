package com.argos.backend_services.service;

import com.argos.backend_services.entity.Medicine;
import com.argos.backend_services.entity.HerdReport;
import com.argos.backend_services.entity.MedicineReport;
import com.argos.backend_services.exception.NotFoundException;
import com.argos.backend_services.exception.PropertyNotFoundException;
import com.argos.backend_services.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportMedicineService {
    private final MedicineRepository medicineRepository;
    private final MedicineReportRepository medicineReportRepository;
    private final MedicineBatchRepository medicineBatchRepository;

    @Scheduled(cron = "${argos.routines.reports.cron:0 0 4 * * *}", zone = "${argos.routines.time-zone:UTC}")
    @Transactional
    public void synchronizeDaily() {
        medicineRepository.findAll().forEach(this::synchronizeMedicine);
    }

    @Transactional
    public void synchronizeMedicine(Medicine medicine) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        var medicineBatch = medicineBatchRepository.findByMedicineId(medicine)
                .orElseThrow(() -> new NotFoundException("Não foi possível achar o lote de medicamento baseado no id de medicamento" + medicine.getId()));
        if (medicine.isActive() && medicineBatch.getExpiryDate().isBefore(ChronoLocalDate.from(now))) {
            medicine.setActive(false);
            medicineRepository.save(medicine);
        }
    }

    public List<MedicineReport> readMedicine(UUID userId, UUID propertyId) throws PropertyNotFoundException {
        return medicineReportRepository.findAll();
    }
}
