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
            RegionCodeInfo regionCode,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    /**
     * 지역코드 정보 응답 DTO
     */
    @Builder
    public record RegionCodeInfo(
            Long regionCodeId,
            String landRegCode,
            String tempRegCode,
            String name,
            String description
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
            RegionCodeInfo regionCode,
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

    /**
     * 지역코드 목록 응답 DTO
     */
    @Builder
    public record RegionCodeList(
            List<RegionCodeDetail> regionCodes,
            int totalCount
    ) {
    }

    /**
     * 지역코드 상세 정보 DTO
     */
    @Builder
    public record RegionCodeDetail(
            Long regionCodeId,
            String landRegCode,
            String tempRegCode,
            String name,
            String description,
            int regionCount,  // 이 지역코드를 사용하는 지역 수
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    /**
     * 지역코드 생성 응답 DTO
     */
    @Builder
    public record CreateRegionCodeResponse(
            Long regionCodeId,
            String landRegCode,
            String tempRegCode,
            String name,
            String description,
            String message
    ) {
    }

    /**
     * 지역코드 삭제 응답 DTO
     */
    @Builder
    public record DeleteRegionCodeResponse(
            Long regionCodeId,
            String name,
            String message
    ) {
    }

    /**
     * 지역코드별 지역 목록 응답 DTO
     */
    @Builder
    public record RegionsByCodeResponse(
            RegionCodeInfo regionCode,
            List<RegionSimple> regions,
            int regionCount
    ) {
    }
}
