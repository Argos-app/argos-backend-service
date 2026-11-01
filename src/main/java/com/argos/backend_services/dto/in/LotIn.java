package com.argos.backend_services.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.argos.backend_services.entity.enums.HerdCategory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
@Schema(description = "Informações para cadastro de lote")
public record LotIn(

        @NotBlank
        @Size(max = 20)
        @Schema(description = "nome do lote", example = "Lote Engorda")
        String lotName,

        @Schema(description = "UUID de rebanho existente. Omitir para criar um novo rebanho")
        UUID herdId,

        @Schema(description = "Nome do novo rebanho; obrigatório apenas sem herdId", example = "Rebanho Nelore")
        String herdName,

        @Schema(description = "Raça do novo rebanho; obrigatória apenas sem herdId", example = "Nelore")
        String breed,

        @NotNull
        @Schema(description = "Categoria do lote", example = "FATTENING")
        HerdCategory category,

        @NotNull
        @Min(0)
        @Schema(description = "Quantidade de bois do lote", example = "200")
        Integer headCount,

        @NotNull
        @Schema(description = "Dia previsto pra abate", example = "2027-07-01")
        LocalDate releaseSlaughterDate
) {
    public LotIn {
        if (herdId == null) {
            herdName = herdName == null ? null : herdName.strip();
            breed = breed == null ? null : breed.strip();
        }
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "herdName é obrigatório sem herdId e deve conter até 60 caracteres")
    public boolean isHerdNameValid() {
        return herdId != null || (herdName != null && !herdName.isBlank() && herdName.length() <= 60);
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "breed é obrigatório sem herdId e deve conter até 40 caracteres")
    public boolean isBreedValid() {
        return herdId != null || (breed != null && !breed.isBlank() && breed.length() <= 40);
    }

}
