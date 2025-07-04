package com.study.demo.testweatherapi.domain.weather.controller;

import com.study.demo.testweatherapi.domain.weather.dto.request.RegionReqDTO;
import com.study.demo.testweatherapi.domain.weather.dto.response.RegionResDTO;
import com.study.demo.testweatherapi.domain.weather.service.RegionManagementService;
import com.study.demo.testweatherapi.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/weather/regions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Region Management API", description = "지역 관리 API")
public class RegionController {

    private final RegionManagementService regionManagementService;

    /**
     * 새 지역 등록 (관리자 전용)
     */
    @Operation(
            summary = "새 지역 등록",
            description = "새로운 지역을 등록합니다. 위도/경도를 기상청 격자 좌표로 자동 변환합니다."
    )
    @PostMapping
//    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public Mono<ResponseEntity<CustomResponse<RegionResDTO.RegionCreated>>> createRegion(
            @Valid @RequestBody RegionReqDTO.CreateRegion request) {

        log.info("새 지역 등록 요청: name={}, lat={}, lon={}, regCode={}",
                request.name(), request.latitude(), request.longitude(), request.regCode());

        return regionManagementService.createRegion(request)
                .map(response -> ResponseEntity.ok(CustomResponse.onSuccess(response)))
                .doOnSuccess(response -> log.info("새 지역 등록 성공: {}", request.name()))
                .doOnError(error -> log.error("새 지역 등록 실패: {}", error.getMessage()));
    }

    /**
     * 지역 수정 (관리자 전용)
     */
    @Operation(
            summary = "지역 정보 수정",
            description = "기존 지역의 정보를 수정합니다."
    )
    @PutMapping("/{regionId}")
//    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public Mono<ResponseEntity<CustomResponse<RegionResDTO.RegionInfo>>> updateRegion(
            @Parameter(description = "지역 ID") @PathVariable Long regionId,
            @Valid @RequestBody RegionReqDTO.UpdateRegion request) {

        log.info("지역 수정 요청: regionId={}, name={}", regionId, request.name());

        return regionManagementService.updateRegion(regionId, request)
                .map(response -> ResponseEntity.ok(CustomResponse.onSuccess(response)))
                .doOnSuccess(response -> log.info("지역 수정 성공: regionId={}", regionId))
                .doOnError(error -> log.error("지역 수정 실패: regionId={}, error={}", regionId, error.getMessage()));
    }

    /**
     * 지역 삭제 (관리자 전용)
     */
    @Operation(
            summary = "지역 삭제",
            description = "기존 지역을 삭제합니다. 연관된 모든 데이터가 함께 삭제됩니다."
    )
    @DeleteMapping("/{regionId}")
//    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<CustomResponse<RegionResDTO.RegionDeleted>> deleteRegion(
            @Parameter(description = "지역 ID") @PathVariable Long regionId) {

        log.info("지역 삭제 요청: regionId={}", regionId);

        RegionResDTO.RegionDeleted response = regionManagementService.deleteRegion(regionId);

        log.info("지역 삭제 성공: regionId={}", regionId);

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 모든 지역 목록 조회
     */
    @Operation(
            summary = "모든 지역 목록 조회",
            description = "서비스에서 지원하는 모든 지역 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<CustomResponse<RegionResDTO.RegionList>> getAllRegions() {

        log.info("모든 지역 목록 조회 요청");

        RegionResDTO.RegionList response = regionManagementService.getAllRegions();

        log.info("지역 목록 조회 성공: {} 개 지역", response.totalCount());

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 특정 지역 조회
     */
    @Operation(
            summary = "특정 지역 조회",
            description = "지역 ID로 특정 지역의 상세 정보를 조회합니다."
    )
    @GetMapping("/{regionId}")
    public ResponseEntity<CustomResponse<RegionResDTO.RegionInfo>> getRegionById(
            @Parameter(description = "지역 ID") @PathVariable Long regionId) {

        log.info("지역 조회 요청: regionId={}", regionId);

        RegionResDTO.RegionInfo response = regionManagementService.getRegionById(regionId);

        log.info("지역 조회 성공: regionId={}, name={}", regionId, response.name());

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 지역명으로 지역 조회
     */
    @Operation(
            summary = "지역명으로 지역 조회",
            description = "지역명으로 지역의 상세 정보를 조회합니다."
    )
    @GetMapping("/name/{name}")
    public ResponseEntity<CustomResponse<RegionResDTO.RegionInfo>> getRegionByName(
            @Parameter(description = "지역명") @PathVariable String name) {

        log.info("지역명으로 조회 요청: name={}", name);

        RegionResDTO.RegionInfo response = regionManagementService.getRegionByName(name);

        log.info("지역명 조회 성공: name={}, regionId={}", name, response.regionId());

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * GPS 좌표로 가장 가까운 지역 조회
     */
    @Operation(
            summary = "가장 가까운 지역 조회",
            description = "GPS 좌표를 기반으로 가장 가까운 지역을 조회합니다."
    )
    @GetMapping("/nearest")
    public ResponseEntity<CustomResponse<RegionResDTO.RegionInfo>> getNearestRegion(
            @Parameter(description = "위도") @RequestParam BigDecimal latitude,
            @Parameter(description = "경도") @RequestParam BigDecimal longitude) {

        log.info("가장 가까운 지역 조회 요청: lat={}, lon={}", latitude, longitude);

        RegionResDTO.RegionInfo response = regionManagementService.getNearestRegion(latitude, longitude);

        log.info("가장 가까운 지역 조회 성공: name={}, regionId={}", response.name(), response.regionId());

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 지역 검색
     */
    @Operation(
            summary = "지역 검색",
            description = "지역명 키워드로 지역을 검색합니다. 부분 일치 검색을 지원합니다."
    )
    @GetMapping("/search")
    public ResponseEntity<CustomResponse<RegionResDTO.RegionList>> searchRegions(
            @Parameter(description = "검색 키워드") @RequestParam String keyword) {

        log.info("지역 검색 요청: keyword={}", keyword);

        RegionResDTO.RegionList response = regionManagementService.searchRegions(keyword);

        log.info("지역 검색 성공: keyword={}, 찾은 지역 수={}", keyword, response.totalCount());

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }
}

