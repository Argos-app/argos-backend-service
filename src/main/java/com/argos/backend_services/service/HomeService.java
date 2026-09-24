package com.argos.backend_services.service;

import com.argos.backend_services.dto.out.HomeApplicationOut;
import com.argos.backend_services.entity.Application;
import com.argos.backend_services.exception.ApplicationWithdrawalRoutineException;
import com.argos.backend_services.mapper.HomeApplicationMapper;
import com.argos.backend_services.repository.ApplicationAnimalRepository;
import com.argos.backend_services.repository.ApplicationRepository;
import com.argos.backend_services.repository.DatabaseRoutineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HomeService {

    private static final int CALENDAR_DAYS_BEFORE = 7;
    private static final int CALENDAR_DAYS_AFTER = 7;

    private final ApplicationRepository applicationRepository;
    private final ApplicationAnimalRepository applicationAnimalRepository;
    private final DatabaseRoutineRepository databaseRoutineRepository;
    private final HomeApplicationMapper mapper;

    @Transactional(readOnly = true)
    public List<HomeApplicationOut> findApplicationCalendar(UUID userId) {
        var referenceDate = LocalDate.now();
        var startDate = referenceDate.minusDays(CALENDAR_DAYS_BEFORE);
        var endDate = referenceDate.plusDays(CALENDAR_DAYS_AFTER);

        try {
            return applicationRepository.findByUserId_IdAndMeatWithdrawalEndBetween(userId, startDate, endDate).stream()
                    .map(application -> toHomeApplicationOut(application, referenceDate))
                    .toList();
        } catch (RuntimeException exception) {
            throw new ApplicationWithdrawalRoutineException("Could not retrieve home application calendar", exception);
        }
    }

    private HomeApplicationOut toHomeApplicationOut(Application application, LocalDate referenceDate) {
        var fitForSlaughter = Boolean.TRUE.equals(databaseRoutineRepository.lotIsReleaseForSlaughter(
                application.getLotId().getId(), referenceDate));

        return HomeApplicationOut.builder()
                .applicationId(application.getId())
                .userId(application.getUserId().getId())
                .lotApplication(List.of(mapper.toLotApplicationOut(application, referenceDate, fitForSlaughter)))
                .animalApplication(applicationAnimalRepository.findByApplicationId(application).stream()
                        .map(applicationAnimal -> mapper.toAnimalApplicationOut(applicationAnimal, application, referenceDate))
                        .toList())
                .build();
    }
}
