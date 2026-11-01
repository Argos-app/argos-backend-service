package com.argos.backend_services.mapper;

import com.argos.backend_services.dto.out.MedicineListItemOut;
import com.argos.backend_services.dto.out.MedicineCatalogItemOut;
import com.argos.backend_services.entity.Medicine;
import com.argos.backend_services.entity.MedicineBatch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface MedicineMapper {

    @Mapping(target = "medicineId", source = "id")
    @Mapping(target = "medicineName", source = "brandName")
    @Mapping(target = "productRegistration", source = "mapaRegistration")
    MedicineCatalogItemOut toCatalogItemOut(Medicine medicine);

    @Mapping(target = "medicineBatchId", source = "batch.id")
    @Mapping(target = "medicineName", source = "batch.medicineId.brandName")
    @Mapping(target = "manufactureDate", source = "batch.manufactureDate")
    @Mapping(target = "expiryDate", source = "batch.expiryDate")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "dose", source = "batch.medicineId.dose")
    @Mapping(target = "withdrawalDays", source = "batch.medicineId.meatWithdrawalDays")
    @Mapping(target = "batch", source = "batch.batchNumber")
    @Mapping(target = "activeIngredient", source = "batch.medicineId.activeIngredient")
    @Mapping(target = "therapeuticCategory", source = "batch.medicineId.therapeuticCategoryId.name")
    MedicineListItemOut toListItemOut(MedicineBatch batch, BigDecimal quantity);
}
