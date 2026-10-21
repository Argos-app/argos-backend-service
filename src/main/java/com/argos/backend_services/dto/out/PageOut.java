package com.argos.backend_services.dto.out;

import java.util.List;

public record PageOut<T>(
        List<T> data,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
