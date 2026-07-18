package com.example.herbalife_clubes.common;

import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;

/**
 * Contrato paginado aditivo (page base 0).
 * Se expone directamente en el body (mismo estilo que {@code List} legacy).
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    public static <T> PagedResponse<T> of(
            List<T> content,
            int page,
            int size,
            long totalElements) {
        List<T> safe = content == null ? List.of() : List.copyOf(content);
        int safeSize = Math.max(size, 1);
        int totalPages = safeSize == 0
                ? 0
                : (int) Math.ceil((double) totalElements / (double) safeSize);
        boolean empty = totalElements == 0;
        boolean first = page <= 0 || empty;
        boolean last = empty || page >= Math.max(totalPages - 1, 0);
        boolean hasNext = !empty && page < totalPages - 1;
        boolean hasPrevious = !empty && page > 0;
        return new PagedResponse<>(
                safe,
                page,
                safeSize,
                totalElements,
                totalPages,
                first,
                last,
                hasNext,
                hasPrevious
        );
    }

    public static <T> PagedResponse<T> empty(int page, int size) {
        return of(Collections.emptyList(), page, size, 0);
    }
}
