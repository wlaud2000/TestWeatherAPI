package com.study.demo.testweatherapi.domain.weather.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class RegionResDTO {

    /**
     * 지역 정보 응답
     */
    @Builder
    public record RegionInfo(
            Long regionId,
            String name,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal gridX,
            BigDecimal gridY,
            String regCode,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    /**
     * 지역 목록 응답
     */
    @Builder
    public record RegionList(
            List<RegionInfo> regions,
            int totalCount
    ) {
    }

    /**
     * 지역 등록 성공 응답
     */
    @Builder
    public record RegionCreated(
            Long regionId,
            String name,
            String message
    ) {
    }

    /**
     * 지역 삭제 성공 응답
     */
    @Builder
    public record RegionDeleted(
            Long regionId,
            String name,
            String message
    ) {
    }
}
