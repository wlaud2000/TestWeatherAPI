package com.study.demo.testweatherapi.domain.weather.converter;

import com.study.demo.testweatherapi.domain.weather.dto.request.RegionReqDTO;
import com.study.demo.testweatherapi.domain.weather.dto.response.KmaApiResDTO;
import com.study.demo.testweatherapi.domain.weather.dto.response.RegionResDTO;
import com.study.demo.testweatherapi.domain.weather.entity.Region;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class RegionConverter {

    /**
     * 지역 등록 요청 + 격자 변환 결과 -> Region Entity
     * 기상청 API 호출 후 받은 격자 좌표와 함께 Entity 생성
     */
    public static Region toRegionEntity(RegionReqDTO.CreateRegion request,
                                 KmaApiResDTO.GridConversionResponse gridResponse) {

        // 격자 변환이 성공했는지 확인
        if (!"SUCCESS".equals(gridResponse.result())) {
            throw new IllegalArgumentException("격자 좌표 변환 실패: " + gridResponse.message());
        }

        return Region.builder()
                .name(request.name())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .gridX(gridResponse.gridX())
                .gridY(gridResponse.gridY())
                .regCode(request.regCode())
                .build();
    }

    /**
     * 지역 수정 요청 + 격자 변환 결과 -> 기존 Region Entity 업데이트
     */
    public static Region updateRegionEntity(Region existingRegion,
                                     RegionReqDTO.UpdateRegion request,
                                     KmaApiResDTO.GridConversionResponse gridResponse) {

        // 격자 변환이 성공했는지 확인
        if (!"SUCCESS".equals(gridResponse.result())) {
            throw new IllegalArgumentException("격자 좌표 변환 실패: " + gridResponse.message());
        }

        return Region.builder()
                .id(existingRegion.getId()) // 기존 ID 유지
                .name(request.name())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .gridX(gridResponse.gridX())
                .gridY(gridResponse.gridY())
                .regCode(request.regCode())
                // 연관관계는 기존 값 유지
                .shortTermWeathers(existingRegion.getShortTermWeathers())
                .mediumTermWeathers(existingRegion.getMediumTermWeathers())
                .dailyRecommendations(existingRegion.getDailyRecommendations())
                .build();
    }

    /**
     * Region Entity -> 지역 정보 응답 DTO
     * 사용자에게 지역 정보 제공 시 사용
     */
    public static RegionResDTO.RegionInfo toRegionInfoResponse(Region entity) {
        return RegionResDTO.RegionInfo.builder()
                .regionId(entity.getId())
                .name(entity.getName())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .gridX(entity.getGridX())
                .gridY(entity.getGridY())
                .regCode(entity.getRegCode())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Region Entity 목록 -> 지역 목록 응답 DTO
     * 전체 지역 목록 조회 시 사용
     */
    public static RegionResDTO.RegionList toRegionListResponse(List<Region> entities) {
        List<RegionResDTO.RegionInfo> regions = entities.stream()
                .map(RegionConverter::toRegionInfoResponse)
                .collect(Collectors.toList());

        return RegionResDTO.RegionList.builder()
                .regions(regions)
                .totalCount(entities.size())
                .build();
    }

    /**
     * Region Entity -> 지역 등록 성공 응답 DTO
     * 지역 등록 성공 시 간단한 정보 제공
     */
    public static RegionResDTO.RegionCreated toRegionCreatedResponse(Region entity) {
        return RegionResDTO.RegionCreated.builder()
                .regionId(entity.getId())
                .name(entity.getName())
                .message("지역이 성공적으로 등록되었습니다.")
                .build();
    }

    /**
     * Region Entity -> 지역 삭제 성공 응답 DTO
     * 지역 삭제 성공 시 확인 정보 제공
     */
    public static RegionResDTO.RegionDeleted toRegionDeletedResponse(Region entity) {
        return RegionResDTO.RegionDeleted.builder()
                .regionId(entity.getId())
                .name(entity.getName())
                .message("지역이 성공적으로 삭제되었습니다.")
                .build();
    }

    /**
     * 좌표 유효성 검증
     * 한국 영역 내 좌표인지 확인
     */
    public static boolean isValidKoreanCoordinates(java.math.BigDecimal latitude, java.math.BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }

        // 한국 영역 좌표 범위
        java.math.BigDecimal minLat = new java.math.BigDecimal("33.0");
        java.math.BigDecimal maxLat = new java.math.BigDecimal("38.0");
        java.math.BigDecimal minLng = new java.math.BigDecimal("124.0");
        java.math.BigDecimal maxLng = new java.math.BigDecimal("132.0");

        return latitude.compareTo(minLat) >= 0 && latitude.compareTo(maxLat) <= 0 &&
                longitude.compareTo(minLng) >= 0 && longitude.compareTo(maxLng) <= 0;
    }

    /**
     * 거리 계산 (간단한 맨하탄 거리)
     * GPS 좌표 기반 가장 가까운 지역 찾기 시 사용
     */
    public static double calculateManhattanDistance(java.math.BigDecimal lat1, java.math.BigDecimal lng1,
                                             java.math.BigDecimal lat2, java.math.BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return Double.MAX_VALUE;
        }

        double latDiff = Math.abs(lat1.subtract(lat2).doubleValue());
        double lngDiff = Math.abs(lng1.subtract(lng2).doubleValue());

        return latDiff + lngDiff;
    }

    /**
     * 지역명 검증
     * 특수문자, 길이 등 기본적인 검증
     */
    public static boolean isValidRegionName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        // 2~20자 한글, 영문, 숫자, 공백만 허용
        String trimmedName = name.trim();
        if (trimmedName.length() < 2 || trimmedName.length() > 20) {
            return false;
        }

        // 한글, 영문, 숫자, 공백, 하이픈만 허용하는 정규식
        return trimmedName.matches("^[가-힣a-zA-Z0-9\\s\\-]+$");
    }

    /**
     * 지역코드 검증
     * 기상청 지역코드 형식 확인
     */
    public static boolean isValidRegionCode(String regCode) {
        if (regCode == null || regCode.trim().isEmpty()) {
            return false;
        }

        // 기상청 지역코드는 일반적으로 숫자와 문자 조합
        // 예: 11B10101, 21F20401 등
        return regCode.matches("^[0-9A-Z]{6,10}$");
    }
}
