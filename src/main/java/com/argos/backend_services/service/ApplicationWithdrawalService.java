package com.argos.backend_services.service;

import com.argos.backend_services.dto.out.ApplicationWithdrawalOut;
import com.argos.backend_services.exception.ApplicationWithdrawalRoutineException;
import com.argos.backend_services.exception.InvalidDateWindowException;
import com.argos.backend_services.mapper.ApplicationWithdrawalMapper;
import com.argos.backend_services.repository.ApplicationRepository;
import com.argos.backend_services.repository.DatabaseRoutineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationWithdrawalService {

    private final ApplicationRepository applicationRepository;
    private final DatabaseRoutineRepository databaseRoutineRepository;
    private final ApplicationWithdrawalMapper mapper;
    private final PropertyAccessService propertyAccessService;
    private final Clock clock = Clock.systemDefaultZone();

    @Transactional(readOnly = true)
    public List<ApplicationWithdrawalOut> findNearDate(
            LocalDate referenceDate, int daysBefore, int daysAfter, UUID userId) throws InvalidDateWindowException {

        if (daysBefore < 0 || daysAfter < 0) {
            throw new InvalidDateWindowException("The date window cannot be negative");
        }
        var start = referenceDate.minusDays(daysBefore);
        var end = referenceDate.plusDays(daysAfter);
        try {
            var propertyIds = propertyAccessService.activePropertyIds(userId);
            if (propertyIds.isEmpty()) {
                return List.of();
            }
            return applicationRepository.findByMeatWithdrawalEndBetweenAndLotId_HerdId_PropertyId_IdIn(
                            start, end, propertyIds).stream()
                    .map(application -> mapper.toOut(application, referenceDate))
                    .toList();
        } catch (RuntimeException exception) {
            throw new ApplicationWithdrawalRoutineException("Could not retrieve application withdrawal dates", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<ApplicationWithdrawalOut> findDueToday() {
        var today = LocalDate.now(clock);
        try {
            return applicationRepository.findByMeatWithdrawalEndBetween(today, today).stream()
                    .filter(application -> Boolean.TRUE.equals(databaseRoutineRepository.lotIsReleaseForSlaughter(
                            application.getLotId().getId(), today)))
                    .map(application -> mapper.toOut(application, today))
                    .toList();
        } catch (RuntimeException exception) {
            throw new ApplicationWithdrawalRoutineException("Could not execute application withdrawal routine", exception);
        }
    }
}