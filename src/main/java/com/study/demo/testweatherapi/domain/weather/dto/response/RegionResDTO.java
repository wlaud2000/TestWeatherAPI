package com.study.demo.testweatherapi.domain.weather.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class RegionResDTO {

    /**
     * 지역 정보 응답 DTO
     */
    @Builder
    public record RegionInfo(
            Long regionId,
            String name,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal gridX,
            BigDecimal gridY,
            String landRegCode,
            String tempRegCode,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    /**
     * 지역 생성 응답 DTO
     */
    @Builder
    public record CreateRegionResponse(
            Long regionId,
            String name,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal gridX,
            BigDecimal gridY,
            String landRegCode,
            String tempRegCode,
            String message
    ) {
    }

    /**
     * 지역 목록 응답 DTO
     */
    @Builder
    public record RegionList(
            List<RegionInfo> regions,
            int totalCount
    ) {
    }

    /**
     * 좌표 변환 결과 응답 DTO
     */
    @Builder
    public record CoordinateConversion(
            BigDecimal inputLatitude,
            BigDecimal inputLongitude,
            BigDecimal gridX,
            BigDecimal gridY,
            String message
    ) {
    }

    /**
     * 지역 검색 결과 DTO
     */
    @Builder
    public record RegionSearchResult(
            List<RegionInfo> regions,
            String keyword,
            int resultCount
    ) {
    }

    /**
     * 지역 삭제 응답 DTO
     */
    @Builder
    public record DeleteRegionResponse(
            Long regionId,
            String name,
            String message
    ) {
    }

    /**
     * 간단한 지역 정보 (선택 목록용)
     */
    @Builder
    public record RegionSimple(
            Long regionId,
            String name,
            String landRegCode,
            String tempRegCode
    ) {
    }
}
