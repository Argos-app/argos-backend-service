package com.argos.backend_services.repository;

import com.argos.backend_services.entity.HerdReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;
import java.util.Collection;

public interface HerdReportRepository extends MongoRepository<HerdReport, String> {
    Optional<HerdReport> findByHerd_Id(String herdId);

    Optional<HerdReport> findByIdAndRelationalPropertyIdIn(String reportId, Collection<String> propertyIds);

    @Query(fields = "{ '_id': 1, 'report_type': 1, 'report_created_at': 1, 'last_updated_at': 1, "
            + "'relational_property_id': 1, 'property_name': 1, 'herd.herd_name': 1, 'herd.breed': 1 }")
    Page<HerdReport> findByRelationalPropertyIdIn(Collection<String> propertyIds, Pageable pageable);
}
