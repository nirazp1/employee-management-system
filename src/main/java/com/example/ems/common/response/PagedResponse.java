package com.example.ems.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(boolean success, List<T> data, PaginationMeta pagination) {

    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(true, page.getContent(), PaginationMeta.from(page));
    }
}
