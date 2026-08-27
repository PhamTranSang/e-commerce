package app.ecommerce.catalog.api.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    int numberOfElements,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last,
    boolean hasNext,
    boolean hasPrevious
) {

    public static <T> PageResponse<T> from(final Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber() + 1,
            page.getSize(),
            page.getNumberOfElements(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
}