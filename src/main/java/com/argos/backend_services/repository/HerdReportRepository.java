package com.argos.backend_services.repository;

import com.argos.backend_services.entity.HerdReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface HerdReportRepository extends MongoRepository<HerdReport, String> {
    Optional<HerdReport> findByHerd_Id(String herdId);
}
