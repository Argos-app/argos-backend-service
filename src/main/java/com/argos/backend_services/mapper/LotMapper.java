package com.argos.backend_services.mapper;

import com.argos.backend_services.dto.in.LotIn;
import com.argos.backend_services.dto.out.LotOut;
import com.argos.backend_services.entity.ApplicationAnimal;
import com.argos.backend_services.entity.Herd;
import com.argos.backend_services.entity.Lot;
import com.argos.backend_services.repository.AnimalExceptionRepository;
import com.argos.backend_services.repository.ApplicationAnimalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;



@Component
@RequiredArgsConstructor
public class LotMapper {

    private final AnimalExceptionRepository animalExceptionRepository;
    private final ApplicationAnimalRepository applicationAnimalRepository;

    public LotOut toLotOut(Lot lot) {
        return LotOut.builder()
                .lotName(lot.getLotCode())
                .openDate(lot.getOpenDate())
                .releaseSlaughterDate(lot.getSlaughterReleaseDate())
                .category(lot.getCategory())
                .build();
    }

    public Lot toEntity(LotIn input, Herd herd) {
        return Lot.builder()
                .herdId(herd)
                .parentId(null)
                .lotCode(input.lotName())
                .category(input.category().getDescription())
                .isExceptionLot(false)
                .headCount(input.headCount())
                .slaughterReleaseDate(input.releaseSlaughterDate())
                .fitForSlaughter(false)
                .openDate(null)
                .closeDate(null)
                .active(true)
                .createdAt(null)
                .updatedAt(null)
                .build();
    }

    public LotOut toOut(Lot lot) {
        var output = toLotOut(lot);
        if (!lot.isExceptionLot()) return output;
        var animals = animalExceptionRepository.findByLotId(lot);
        var details = animals.stream()
                .map(animal -> {
                    var applications = applicationAnimalRepository.findByAnimalExceptionId(animal);
                    ApplicationAnimal latest = applications.stream()
                            .reduce((first, second) -> second)
                            .orElse(null);
                    return LotOut.AnimalExceptionOut.builder()
                            .earCode(animal.getEarTag())
                            .medicine(latest == null ? null : latest.getApplicationId().getMedicineBatchId().getMedicineId().getBrandName())
                            .releaseSlaughterDate(latest == null ? animal.getSlaughterReleaseDate() : latest.getMeatWithdrawalDate())
                            .build();
                }).toList();
        output.setAnimalExceptionOutList(details);
        return output;
    }
}
