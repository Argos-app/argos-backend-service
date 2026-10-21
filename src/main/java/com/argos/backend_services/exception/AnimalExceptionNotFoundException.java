package com.argos.backend_services.exception;

import java.util.UUID;

public class AnimalExceptionNotFoundException extends NotFoundException {

    public AnimalExceptionNotFoundException(UUID animalExceptionId) {
        super("Exceção animal não encontrada: " + animalExceptionId);
    }
}
