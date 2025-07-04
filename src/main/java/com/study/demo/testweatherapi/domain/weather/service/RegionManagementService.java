package com.study.demo.testweatherapi.domain.weather.service;

import com.study.demo.testweatherapi.domain.weather.converter.RegionConverter;
import com.study.demo.testweatherapi.domain.weather.dto.request.RegionReqDTO;
import com.study.demo.testweatherapi.domain.weather.dto.response.RegionResDTO;
import com.study.demo.testweatherapi.domain.weather.entity.Region;
import com.study.demo.testweatherapi.domain.weather.exception.WeatherErrorCode;
import com.study.demo.testweatherapi.domain.weather.exception.WeatherException;
import com.study.demo.testweatherapi.domain.weather.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionManagementService {

    private final WeatherApiService weatherApiService;
    private final RegionRepository regionRepository;

    /**
     * 새 지역 등록
     */
    @Transactional
    public Mono<RegionResDTO.RegionCreated> createRegion(RegionReqDTO.CreateRegion request) {
        log.info("새 지역 등록 시작: name={}, lat={}, lon={}, regCode={}",
                request.name(), request.latitude(), request.longitude(), request.regCode());

        validateDuplicateRegion(request);
        validateCoordinates(request.latitude(), request.longitude());
        validateRegionData(request);

        return weatherApiService.convertCoordinatesToGrid(request.latitude(), request.longitude())
                .flatMap(gridResponse -> {
                    try {
                        Region region = RegionConverter.toRegionEntity(request, gridResponse);
                        Region savedRegion = regionRepository.save(region);

                        log.info("지역 등록 완료: id={}, name={}, gridX={}, gridY={}",
                                savedRegion.getId(), savedRegion.getName(),
                                savedRegion.getGridX(), savedRegion.getGridY());

                        return Mono.just(RegionConverter.toRegionCreatedResponse(savedRegion));
                    } catch (Exception e) {
                        log.error("지역 저장 중 오류 발생: {}", e.getMessage());
                        throw new WeatherException(WeatherErrorCode.WEATHER_DATA_PROCESSING_ERROR);
                    }
                });
    }

    /**
     * 지역 수정
     */
    @Transactional
    public Mono<RegionResDTO.RegionInfo> updateRegion(Long regionId, RegionReqDTO.UpdateRegion request) {
        log.info("지역 수정 시작: regionId={}, name={}", regionId, request.name());

        Region existingRegion = regionRepository.findById(regionId)
                .orElseThrow(() -> new WeatherException(WeatherErrorCode.REGION_NOT_FOUND));

        validateDuplicateForUpdate(regionId, request);
        validateCoordinates(request.latitude(), request.longitude());
        validateRegionUpdateData(request);

        return weatherApiService.convertCoordinatesToGrid(request.latitude(), request.longitude())
                .flatMap(gridResponse -> {
                    try {
                        Region updatedRegion = RegionConverter.updateRegionEntity(existingRegion, request, gridResponse);
                        Region savedRegion = regionRepository.save(updatedRegion);

                        log.info("지역 수정 완료: id={}, name={}", savedRegion.getId(), savedRegion.getName());

                        return Mono.just(RegionConverter.toRegionInfoResponse(savedRegion));
                    } catch (Exception e) {
                        log.error("지역 수정 중 오류 발생: {}", e.getMessage());
                        throw new WeatherException(WeatherErrorCode.WEATHER_DATA_PROCESSING_ERROR);
                    }
                });
    }

    /**
     * 지역 삭제
     */
    @Transactional
    public RegionResDTO.RegionDeleted deleteRegion(Long regionId) {
        log.info("지역 삭제 시작: regionId={}", regionId);

        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new WeatherException(WeatherErrorCode.REGION_NOT_FOUND));

        int shortTermCount = region.getShortTermWeathers().size();
        int mediumTermCount = region.getMediumTermWeathers().size();
        int recommendationCount = region.getDailyRecommendations().size();

        if (shortTermCount > 0 || mediumTermCount > 0 || recommendationCount > 0) {
            log.warn("지역 삭제 시 연관 데이터도 함께 삭제됩니다: regionId={}, 단기예보={}, 중기예보={}, 추천={}",
                    regionId, shortTermCount, mediumTermCount, recommendationCount);
        }

        regionRepository.delete(region);

        log.info("지역 삭제 완료: regionId={}, name={}", region.getId(), region.getName());

        return RegionConverter.toRegionDeletedResponse(region);
    }

    /**
     * 모든 지역 목록 조회
     */
    @Transactional(readOnly = true)
    public RegionResDTO.RegionList getAllRegions() {
        log.info("모든 지역 목록 조회");

        List<Region> regions = regionRepository.findAllByOrderByName();

        log.info("지역 목록 조회 완료: {} 개 지역", regions.size());

        return RegionConverter.toRegionListResponse(regions);
    }

    /**
     * 특정 지역 조회
     */
    @Transactional(readOnly = true)
    public RegionResDTO.RegionInfo getRegionById(Long regionId) {
        log.info("지역 조회: regionId={}", regionId);

        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new WeatherException(WeatherErrorCode.REGION_NOT_FOUND));

        return RegionConverter.toRegionInfoResponse(region);
    }

    /**
     * 지역명으로 지역 조회
     */
    @Transactional(readOnly = true)
    public RegionResDTO.RegionInfo getRegionByName(String name) {
        log.info("지역명으로 조회: name={}", name);

        Region region = regionRepository.findByName(name)
                .orElseThrow(() -> new WeatherException(WeatherErrorCode.REGION_NOT_FOUND));

        return RegionConverter.toRegionInfoResponse(region);
    }

    /**
     * GPS 좌표로 가장 가까운 지역 조회
     */
    @Transactional(readOnly = true)
    public RegionResDTO.RegionInfo getNearestRegion(BigDecimal latitude, BigDecimal longitude) {
        log.info("가장 가까운 지역 조회: lat={}, lon={}", latitude, longitude);

        validateCoordinates(latitude, longitude);

        Region region = regionRepository.findNearestRegion(latitude, longitude)
                .orElseThrow(() -> new WeatherException(WeatherErrorCode.REGION_NOT_FOUND));

        log.info("가장 가까운 지역 찾음: name={}", region.getName());

        return RegionConverter.toRegionInfoResponse(region);
    }

    /**
     * 지역 검색
     */
    @Transactional(readOnly = true)
    public RegionResDTO.RegionList searchRegions(String keyword) {
        log.info("지역 검색: keyword={}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            throw new WeatherException(WeatherErrorCode.INVALID_COORDINATES);
        }

        List<Region> regions = regionRepository.findByNameContainingIgnoreCaseOrderByName(keyword.trim());

        log.info("지역 검색 완료: {} 개 지역 찾음", regions.size());

        return RegionConverter.toRegionListResponse(regions);
    }

    // ==== 검증 메서드들 ====

    private void validateDuplicateRegion(RegionReqDTO.CreateRegion request) {
        if (regionRepository.existsByName(request.name())) {
            log.warn("중복된 지역명: {}", request.name());
            throw new WeatherException(WeatherErrorCode.REGION_ALREADY_EXISTS);
        }

        if (regionRepository.existsByRegCode(request.regCode())) {
            log.warn("중복된 지역코드: {}", request.regCode());
            throw new WeatherException(WeatherErrorCode.REGION_ALREADY_EXISTS);
        }
    }

    private void validateDuplicateForUpdate(Long regionId, RegionReqDTO.UpdateRegion request) {
        regionRepository.findByName(request.name())
                .ifPresent(region -> {
                    if (!region.getId().equals(regionId)) {
                        log.warn("중복된 지역명: {}", request.name());
                        throw new WeatherException(WeatherErrorCode.REGION_ALREADY_EXISTS);
                    }
                });

        regionRepository.findByRegCode(request.regCode())
                .ifPresent(region -> {
                    if (!region.getId().equals(regionId)) {
                        log.warn("중복된 지역코드: {}", request.regCode());
                        throw new WeatherException(WeatherErrorCode.REGION_ALREADY_EXISTS);
                    }
                });
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (!RegionConverter.isValidKoreanCoordinates(latitude, longitude)) {
            log.warn("올바르지 않은 좌표: lat={}, lon={}", latitude, longitude);
            throw new WeatherException(WeatherErrorCode.INVALID_COORDINATES);
        }
    }

    private void validateRegionData(RegionReqDTO.CreateRegion request) {
        if (!RegionConverter.isValidRegionName(request.name())) {
            log.warn("올바르지 않은 지역명: {}", request.name());
            throw new WeatherException(WeatherErrorCode.INVALID_COORDINATES);
        }

        if (!RegionConverter.isValidRegionCode(request.regCode())) {
            log.warn("올바르지 않은 지역코드: {}", request.regCode());
            throw new WeatherException(WeatherErrorCode.INVALID_COORDINATES);
        }
    }

    private void validateRegionUpdateData(RegionReqDTO.UpdateRegion request) {
        if (!RegionConverter.isValidRegionName(request.name())) {
            log.warn("올바르지 않은 지역명: {}", request.name());
            throw new WeatherException(WeatherErrorCode.INVALID_COORDINATES);
        }

        if (!RegionConverter.isValidRegionCode(request.regCode())) {
            log.warn("올바르지 않은 지역코드: {}", request.regCode());
            throw new WeatherException(WeatherErrorCode.INVALID_COORDINATES);
        }
    }
}
