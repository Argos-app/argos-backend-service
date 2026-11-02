package com.argos.backend_services.repository;

import com.argos.backend_services.entity.MedicineReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MedicineReportRepository extends MongoRepository<MedicineReport, String> {
}
