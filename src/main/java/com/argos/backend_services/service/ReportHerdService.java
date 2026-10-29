package com.argos.backend_services.service;

import com.argos.backend_services.dto.out.HerdReportListItemOut;
import com.argos.backend_services.dto.out.PageOut;
import com.argos.backend_services.entity.*;
import com.argos.backend_services.exception.ArgosInternalServerException;
import com.argos.backend_services.exception.PropertyNotFoundException;
import com.argos.backend_services.repository.*;
import com.argos.backend_services.utils.PageableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportHerdService {

    private final PropertyRepository propertyRepository;
    private final HerdRepository herdRepository;
    private final HerdReportRepository herdReportRepository;
    private final LotRepository lotRepository;
    private final ApplicationRepository applicationRepository;
    private final AnimalExceptionRepository animalExceptionRepository;
    private final ApplicationAnimalRepository applicationAnimalRepository;
    private final PropertyAccessService propertyAccessService;

    @Transactional(readOnly = true)
    public PageOut<HerdReportListItemOut> list(UUID propertyId, int page, int size, UUID administratorId) {
        var pageable = PageableUtils.createPageRequest(page, size,
                Sort.by(Sort.Order.desc("reportCreatedAt"), Sort.Order.desc("id")));
        try {
            var propertyIds = propertyRepository.findByUserIdAndActiveTrue(administratorId).stream()
                    .map(property -> property.getId().toString()).toList();
            if (propertyId != null) {
                if (!propertyIds.contains(propertyId.toString())) {
                    throw new AccessDeniedException("Usuário não administra a propriedade informada");
                }
                propertyIds = List.of(propertyId.toString());
            }
            var reports = propertyIds.isEmpty() ? Page.<HerdReport>empty(pageable)
                    : herdReportRepository.findByRelationalPropertyIdIn(propertyIds, pageable);
            var result = reports.map(HerdReportListItemOut::from);
            return new PageOut<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(),
                    result.getTotalPages(), result.isFirst(), result.isLast());
        } catch (DataAccessException exception) {
            var failure = new ArgosInternalServerException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "HERD_REPORT_LIST_FAILED", "Não foi possível listar os relatórios");
            if (log.isErrorEnabled()) {
                log.error("herd_report_list_failed userId={} propertyId={} cause={}",
                        administratorId, propertyId, exception.getClass().getSimpleName());
            }
            failure.initCause(exception);
            throw failure;
        }
    }

    @Transactional(readOnly = true)
    public HerdReport readHerd(UUID userId, UUID propertyId) throws PropertyNotFoundException {
        propertyAccessService.requireActiveAccess(userId, propertyId);
        var property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException("Propriedade não encontrada: " + propertyId));
        var herd = herdRepository.findByPropertyIdAndActiveTrue(property).stream()
                .findFirst()
                .orElseThrow(() -> new PropertyNotFoundException("A propriedade não possui rebanho ativo: " + propertyId));

        return herdReportRepository.findByHerd_Id(herd.getId().toString())
                .orElseThrow(() -> new PropertyNotFoundException("Relatório não encontrado para o rebanho: " + herd.getId()));
    }

    @Transactional
    public void saveHerd(UUID userId, UUID propertyId) throws PropertyNotFoundException {
        synchronizeProperty(userId, propertyId, false);
    }

    @Transactional
    public void syncHerd(UUID userId, UUID propertyId) throws PropertyNotFoundException {
        synchronizeProperty(userId, propertyId, true);
    }

    @Scheduled(cron = "${argos.routines.reports.cron:0 0 4 * * *}", zone = "${argos.routines.time-zone:UTC}")
    @Transactional
    public void synchronizeDaily() {
        propertyRepository.findAll().stream()
                .filter(property -> Boolean.TRUE.equals(property.getActive()))
                .forEach(property -> synchronizeProperty(property, true));
    }

    private void synchronizeProperty(UUID userId, UUID propertyId, boolean dailySynchronization) throws PropertyNotFoundException {
        propertyAccessService.requireActiveAccess(userId, propertyId);
        var property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException("Propriedade não encontrada: " + propertyId));
        synchronizeProperty(property, dailySynchronization);
    }

    private void synchronizeProperty(Property property, boolean dailySynchronization) {
        herdRepository.findByPropertyIdAndActiveTrue(property)
                .forEach(herd -> synchronizeHerd(property, herd, dailySynchronization));
    }

    private void synchronizeHerd(Property property,
                                 Herd herd,
                                 boolean dailySynchronization) {

        var lots = lotRepository.findByHerdId(herd);
        var applications = lots.isEmpty() ? List.<Application>of()
                : applicationRepository.findByLotIdIn(lots);

        var animals = lots.isEmpty() ? List.<AnimalException>of()
                : animalExceptionRepository.findByLotIdIn(lots);

        var animalApplications = animals.isEmpty() ? List.<ApplicationAnimal>of()
                : applicationAnimalRepository.findByAnimalExceptionIdIn(animals);

        var now = LocalDateTime.now(ZoneOffset.UTC);
        var currentReport = herdReportRepository.findByHerd_Id(herd.getId().toString());

        var report = HerdReport.builder()
                .id(currentReport.map(HerdReport::getId).orElse(null))
                .reportCreatedAt(currentReport.map(HerdReport::getReportCreatedAt).orElse(now))
                .lastUpdatedAt(now)
                .reportType(dailySynchronization ? "DAILY_SYNC" : "ON_DEMAND")
                .relationalPropertyId(property.getId().toString())
                .propertyName(property.getName())
                .herd(toReportHerd(herd))
                .lots(lots.stream().map(lot -> toReportLot(lot, applications, animals, animalApplications)).toList())
                .historySummary(toHistorySummary(lots, applications, animals, animalApplications))
                .build();
        herdReportRepository.save(report);
    }

    private HerdReport.Herd toReportHerd(Herd herd) {
        return HerdReport.Herd.builder()
                .id(herd.getId().toString())
                .name(herd.getHerdName())
                .breed(herd.getBreed())
                .build();
    }

    private HerdReport.Lot toReportLot(Lot lot,
                                       List<Application> applications,
                                       List<AnimalException> animals,
                                       List<ApplicationAnimal> animalApplications) {
        return HerdReport.Lot.builder()
                .id(lot.getId().toString())
                .parentLotId(lot.getParentId() == null ? null : lot.getParentId().getId().toString())
                .code(lot.getLotCode())
                .category(lot.getCategory())
                .exceptionLot(lot.isExceptionLot())
                .headCount(lot.getHeadCount())
                .openDate(lot.getOpenDate())
                .closeDate(lot.getCloseDate())
                .fitForSlaughter(lot.isFitForSlaughter())
                .slaughterReleaseDate(lot.getSlaughterReleaseDate())
                .applications(applications.stream().filter(application -> application.getLotId().getId().equals(lot.getId()))
                        .map(this::toReportApplication).toList())
                .animal(animals.stream().filter(animal -> animal.getLotId().getId().equals(lot.getId()))
                        .map(animal -> toReportAnimal(animal, animalApplications)).toList())
                .build();
    }

    private HerdReport.Application toReportApplication(Application application) {
        return HerdReport.Application.builder()
                .id(application.getId().toString())
                .date(application.getApplicationDate())
                .time(application.getApplicationTime())
                .applicatorUserId(application.getUserId().getId().toString())
                .medicineBatchId(application.getMedicineBatchId().getId().toString())
                .dose(application.getDose())
                .unit(application.getUnit())
                .route(application.getRoute())
                .headCountApplied(application.getHeadCountApplied())
                .goal(application.getGoal())
                .notes(application.getNotes())
                .meatWithdrawalEnd(application.getMeatWithdrawalEnd())
                .syncedAt(application.getSyncedAt())
                .build();
    }

    private HerdReport.Animal toReportAnimal(AnimalException animal,
                                               List<ApplicationAnimal> animalApplications) {
        return HerdReport.Animal.builder()
                .id(animal.getId().toString())
                .earTag(animal.getEarTag())
                .category(animal.getCategory())
                .birthDate(animal.getBirthDate())
                .currentWeight(animal.getCurrentWeight())
                .exceptionDate(animal.getExceptionDate())
                .exceptionReason(animal.getExceptionReason())
                .status(animal.getStatus().name())
                .fitForSlaughter(animal.isFitForSlaughter())
                .slaughterReleaseDate(animal.getSlaughterReleaseDate())
                .originLotId(animal.getOriginLotId().getId().toString())
                .lotId(animal.getLotId().getId().toString())
                .exceptionApplications(animalApplications.stream()
                        .filter(application -> application.getAnimalExceptionId().getId().equals(animal.getId()))
                        .map(this::toExceptionApplication).toList())
                .occurrences(List.of())
                .lotHistory(List.of())
                .build();
    }

    private HerdReport.ExceptionApplication toExceptionApplication(
            ApplicationAnimal applicationAnimal) {
        var application = applicationAnimal.getApplicationId();
        var exceptionApplication = new HerdReport.ExceptionApplication();
        exceptionApplication.setApplicationAnimalId(applicationAnimal.getId().toString());
        exceptionApplication.setApplicationId(application.getId().toString());
        exceptionApplication.setLotId(application.getLotId().getId().toString());
        exceptionApplication.setLotCode(application.getLotId().getLotCode());
        exceptionApplication.setApplicationDate(application.getApplicationDate());
        exceptionApplication.setApplicationTime(application.getApplicationDate() == null || application.getApplicationTime() == null ? null
                : LocalDateTime.of(application.getApplicationDate(), application.getApplicationTime()));
        exceptionApplication.setApplicatorUserId(application.getUserId().getId().toString());
        exceptionApplication.setMedicineBatchId(application.getMedicineBatchId().getId().toString());
        exceptionApplication.setDoseGiven(applicationAnimal.getDose_given());
        exceptionApplication.setUnit(application.getUnit());
        exceptionApplication.setRoute(application.getRoute());
        exceptionApplication.setWeightAtTime(applicationAnimal.getWeight_at_time());
        exceptionApplication.setGoal(application.getGoal());
        exceptionApplication.setNotes(application.getNotes());
        exceptionApplication.setMeatWithdrawalEnd(applicationAnimal.getMeatWithdrawalDate());
        exceptionApplication.setReleasedForSlaughter(applicationAnimal.isReleasedForSlaughter());
        return exceptionApplication;
    }

    private HerdReport.HistorySummary toHistorySummary(List<Lot> lots,
                                                        List<Application> applications,
                                                        List<AnimalException> animals,
                                                        List<ApplicationAnimal> animalApplications) {
        int totalOccurrences = 0;
        int openOccurrenceCount = 0;
        for (var animal : animals) {
            totalOccurrences++;
            if (animal.getStatus() == AnimalException.StatusAnimal.ATIVO) {
                openOccurrenceCount++;
            }
        }
        return HerdReport.HistorySummary.builder()
                .totalLots(lots.size())
                .totalExceptionLots(lots.stream().filter(Lot::isExceptionLot).count())
                .totalExceptionAnimals(animals.size())
                .totalLotApplications(applications.size())
                .totalExceptionApplications(animalApplications.size())
                .totalOccurrences(totalOccurrences)
                .openOccurrenceCount(openOccurrenceCount)
                .firstIndividualHistoryDate(minDate(animals.stream()
                        .map(AnimalException::getExceptionDate).toList()))
                .lastLotChangeDate(maxDate(lots.stream().map(lot -> lot.getUpdatedAt() == null
                        ? null : lot.getUpdatedAt().toLocalDate()).toList()))
                .lastExceptionApplicationDate(maxDate(animalApplications.stream()
                        .map(application -> application.getApplicationId().getApplicationDate()).toList()))
                .allExceptionAnimalsFitForSlaughter(animals.stream()
                        .allMatch(AnimalException::isFitForSlaughter))
                .latestExceptionAnimalSlaughterReleaseDate(maxDate(animals.stream()
                        .map(AnimalException::getSlaughterReleaseDate).toList()))
                .build();
    }

    private LocalDate minDate(List<LocalDate> dates) {
        return dates.stream().filter(java.util.Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
    }

    private LocalDate maxDate(List<LocalDate> dates) {
        return dates.stream().filter(java.util.Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
    }
}
