package com.moneyTransfer.domain.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class PageResult<T> {

    private final List<T> content;
    private final Integer pageNumber;
    private final Integer pageSize;
    private final Long totalElements;
    private final Integer totalPages;

    public Boolean hasNext() {
        return pageNumber < totalPages - 1;
    }

    public Boolean hasPrevious() {
        return pageNumber > 0;
    }

    public Boolean isEmpty() {
        return content.isEmpty();
    }
}