package com.argos.backend_services.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "medicine_reports")
public class MedicineReport {
    @Id private String id;
    @Field("report_created_at")
    private LocalDateTime reportCreatedAt;
    @Field("last_updated_at")
    private LocalDateTime lastUpdatedAt;
    @Field("report_type")
    private String reportType;
    @Field("relational_medicine_id")
    private String relationalMedicineId;
    @Field("brand_name")
    private String brandName;
    @Field("active_ingredient")
    private String activeIngredient;
    @Field("mapa_registration")
    private String mapaRegistration;
    private String indication;
    private String description;
    @Field("therapeutic_category")
    private TherapeuticCategory therapeuticCategory;
    private Supplier supplier;
    @Field("needs_prescription")
    private boolean needsPrescription;
    private boolean active;
    @Field("meat_withdrawal_days")
    private Integer meatWithdrawalDays;
    private BigDecimal dose;
    private String unit;
    private String route;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TherapeuticCategory {
        @Field("therapeutic_category_id")
        private String id;
        private String name;
        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Supplier {
        @Field("supplier_id")
        private String id;
        @Field("company_name")
        private String companyName;
        private String cnpj;
        private String email;
        private String phone;
        private String address;
    }
}
