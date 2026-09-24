package com.argos.backend_services.exception;

import org.springframework.http.HttpStatus;

public class ApplicationWithdrawalRoutineException extends ArgosInternalServerException {
    public ApplicationWithdrawalRoutineException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "APPLICATION_WITHDRAWAL_ROUTINE_ERROR", message);
        initCause(cause);
    }
}
