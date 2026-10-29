    package com.argos.backend_services.entity;
    
    import lombok.*;
    import org.springframework.data.annotation.Id;
    import org.springframework.data.mongodb.core.mapping.Document;
    import org.springframework.data.mongodb.core.mapping.Field;
    
    import java.math.BigDecimal;
    import java.time.LocalDate;
    import java.util.List;
    import java.time.LocalDateTime;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Document(collection = "herd_reports")
    public class HerdReport {
        @Id private String id;
        @Field("report_created_at")
        private LocalDateTime reportCreatedAt;
        @Field("last_updated_at")
        private LocalDateTime lastUpdatedAt;
        @Field("report_type")
        private String reportType;
        @Field("relational_property_id")
        private String relationalPropertyId;
        @Field("property_name")
        private String propertyName;
        private Herd herd;
        private List<Lot> lots;
        @Field("history_summary")
        private HistorySummary historySummary;
    
        @Getter
        @Setter
        @Builder
        public static class Herd {
            @Field("herd_id")
            private String id;
            @Field("herd_name")
            private String name;
            private String breed;
        }
    
        @Getter
        @Setter
        @Builder
        public static class Lot {
            @Field("lot_id")
            private String id;
            @Field("parent_lot_id")
            private String parentLotId;
            @Field("lot_code")
            private String code;
            private String category;
            @Field("is_exception_lot")
            private Boolean exceptionLot;
            @Field("exception_reason")
            private String exceptionReason;
            @Field("head_count")
            private Integer headCount;
            @Field("open_date")
            private Object openDate;
            @Field("close_date")
            private Object closeDate;
            @Field("fit_for_slaughter")
            private Boolean fitForSlaughter;
            @Field("slaughter_release_date")
            private Object slaughterReleaseDate;
            private List<Application> applications;
            private List<Animal> animal;
        }
    
        @Getter
        @Setter
        @Builder
        public static class Application {
            @Field("application_id")
            private String id;
            @Field("application_date")
            private Object date;
            @Field("application_time")
            private Object time;
            @Field("applicator_user_id")
            private String applicatorUserId;
            @Field("medicine_batch_id")
            private String medicineBatchId;
            private Object dose;
            private String unit;
            private String route;
            @Field("head_count_applied")
            private Integer headCountApplied;
            private String goal;
            private String notes;
            @Field("meat_withdrawal_end")
            private Object meatWithdrawalEnd;
            @Field("synced_at")
            private LocalDateTime syncedAt;
        }
    
        @Getter
        @Setter
        @Builder
        public static class Animal {
            @Field("animal_exception_id")
            private String id;
            @Field("ear_tag")
            private String earTag;
            private String category;
            @Field("birth_date")
            private LocalDate birthDate;
            @Field("current_weight")
            private BigDecimal currentWeight;
            @Field("exception_date")
            private LocalDate exceptionDate;
            @Field("exception_reason")
            private String exceptionReason;
            private String status;
            @Field("fit_for_slaughter")
            private Boolean fitForSlaughter;
            @Field("slaughter_release_date")
            private LocalDate slaughterReleaseDate;
            @Field("exception_end_date")
            private LocalDate exceptionEndDate;
            @Field("origin_lot_id")
            private String originLotId;
            @Field("lot_id")
            private String lotId;
            @Field("exception_applications")
            private List<ExceptionApplication> exceptionApplications;
            private List<Occurrence> occurrences;
            @Field("lot_history")
            private List<LotHistory> lotHistory;
        }
    
        @Getter
        @Setter
        @NoArgsConstructor
        public static class ExceptionApplication {
            @Field("application_animal_id")
            private String applicationAnimalId;
            @Field("application_id")
            private String applicationId;
            @Field("lot_id")
            private String lotId;
            @Field("lot_code")
            private String lotCode;
            @Field("application_date")
            private LocalDate applicationDate;
            @Field("application_time")
            private LocalDateTime applicationTime;
            @Field("applicator_user_id")
            private String applicatorUserId;
            @Field("medicine_batch_id")
            private String medicineBatchId;
            @Field("dose_given")
            private BigDecimal doseGiven;
            private String unit;
            private String route;
            @Field("weight_at_time")
            private BigDecimal weightAtTime;
            private String goal;
            private String notes;
            @Field("meat_withdrawal_end")
            private LocalDate meatWithdrawalEnd;
            @Field("released_for_slaughter")
            private Boolean releasedForSlaughter;
        }
    
        @Getter
        @Setter
        @Builder
        public static class Occurrence {
            @Field("occurrence_id")
            private String id;
            @Field("recorded_by_user_id")
            private String recordedByUserId;
            private String type;
            @Field("occurrence_date")
            private LocalDate date;
            private String description;
            private String diagnosis;
            private Boolean resolved;
    
        }
    
        @Getter
        @Setter
        @Builder
        public static class LotHistory {
            @Field("animal_lot_history_id")
            private String id;
            @Field("lot_id")
            private String lotId;
            @Field("entry_date")
            private LocalDate entryDate;
            @Field("exit_date")
            private LocalDate exitDate;
            private String reason;
        }
    
        @Getter
        @Setter
        @Builder
        public static class HistorySummary {
            @Field("total_lots")
            private Integer totalLots;
            @Field("total_exception_lots")
            private Long totalExceptionLots;
            @Field("total_exception_animals")
            private Integer totalExceptionAnimals;
            @Field("total_lot_applications")
            private Integer totalLotApplications;
            @Field("total_exception_applications")
            private Integer totalExceptionApplications;
            @Field("total_occurrences")
            private Integer totalOccurrences;
            @Field("open_occurrence_count")
            private Integer openOccurrenceCount;
            @Field("first_individual_history_date")
            private LocalDate firstIndividualHistoryDate;
            @Field("last_lot_change_date")
            private LocalDate lastLotChangeDate;
            @Field("last_exception_application_date")
            private LocalDate lastExceptionApplicationDate;
            @Field("last_occurrence_date")
            private LocalDate lastOccurrenceDate;
            @Field("all_exception_animals_fit_for_slaughter")
            private Boolean allExceptionAnimalsFitForSlaughter;
            @Field("latest_exception_animal_slaughter_release_date")
            private LocalDate latestExceptionAnimalSlaughterReleaseDate;
        }
    }
