package com.argos.backend_services.repository;

import com.argos.backend_services.entity.HerdReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.Collection;

public interface HerdReportRepository extends MongoRepository<HerdReport, String> {
    Optional<HerdReport> findByHerd_Id(String herdId);

    Optional<HerdReport> findByIdAndRelationalPropertyIdIn(String reportId, Collection<String> propertyIds);
}
