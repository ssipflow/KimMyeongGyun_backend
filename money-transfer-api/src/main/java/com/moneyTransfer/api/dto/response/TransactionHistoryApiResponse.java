package com.moneyTransfer.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "거래내역 조회 API 응답")
public class TransactionHistoryApiResponse {

    @Schema(description = "거래 목록")
    private List<TransactionApiResponse> transactions;

    @Schema(description = "페이징 정보")
    private PageInfoApiResponse pageInfo;

    public TransactionHistoryApiResponse(List<TransactionApiResponse> transactions, PageInfoApiResponse pageInfo) {
        this.transactions = transactions;
        this.pageInfo = pageInfo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "페이징 정보")
    public static class PageInfoApiResponse {

        @Schema(description = "현재 페이지", example = "0")
        private Integer currentPage;

        @Schema(description = "페이지 크기", example = "10")
        private Integer pageSize;

        @Schema(description = "전체 요소 수", example = "50")
        private Long totalElements;

        @Schema(description = "전체 페이지 수", example = "5")
        private Integer totalPages;

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private Boolean hasNext;

        @Schema(description = "이전 페이지 존재 여부", example = "false")
        private Boolean hasPrevious;

        public PageInfoApiResponse(Integer currentPage, Integer pageSize, Long totalElements,
                                  Integer totalPages, Boolean hasNext, Boolean hasPrevious) {
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.hasNext = hasNext;
            this.hasPrevious = hasPrevious;
        }
    }
}