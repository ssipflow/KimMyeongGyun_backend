package com.moneyTransfer.domain.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PageQuery {

    private final Integer page;
    private final Integer size;

    public Integer getOffset() {
        return page * size;
    }

    public static PageQuery of(Integer page, Integer size) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다");
        }
        if (size < 1) {
            throw new IllegalArgumentException("페이지 크기는 1 이상이어야 합니다");
        }
        return new PageQuery(page, size);
    }
}