package com.argos.backend_services.repository;

import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.entity.HerdReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface HerdReportRepository extends MongoRepository<HerdReport, String> {
    Optional<List<HerdReport>> findByHerd(List<Herd> herd);
}
