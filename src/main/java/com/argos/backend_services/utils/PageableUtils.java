package com.argos.backend_services.utils;

import com.argos.backend_services.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableUtils {

    private static final int MAX_PAGE_SIZE = 100;

    private PageableUtils() {
    }

    public static Pageable createPageRequest(int page, int size, Sort sort) {
        validatePageAndSize(page, size);

        return PageRequest.of(page, size, sort);
    }

    public static void validatePageAndSize(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Página não pode ser negativa");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Tamanho da página deve estar entre 1 e " + MAX_PAGE_SIZE);
        }
    }
}
