package com.study.demo.testweatherapi.domain.weather.controller;

import com.study.demo.testweatherapi.domain.weather.dto.request.RegionReqDTO;
import com.study.demo.testweatherapi.domain.weather.dto.response.RegionResDTO;
import com.study.demo.testweatherapi.domain.weather.service.RegionService;
import com.study.demo.testweatherapi.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/regions")
@RequiredArgsConstructor
@Tag(name = "지역 관리 API", description = "관리자 전용 지역 등록/관리 API")
public class AdminRegionController {

    private final RegionService regionService;

    /**
     * 새로운 지역 등록
     * 위경도를 격자 좌표로 변환하여 저장
     */
    @PostMapping
    @Operation(summary = "지역 등록", description = "새로운 지역을 등록합니다. 위경도는 자동으로 격자 좌표로 변환됩니다.")
    public ResponseEntity<CustomResponse<RegionResDTO.CreateRegionResponse>> createRegion(
            @Valid @RequestBody RegionReqDTO.CreateRegion request) {

        log.info("지역 등록 API 호출: {}", request.name());

        RegionResDTO.CreateRegionResponse response = regionService.createRegion(request);

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 좌표 변환 (미리보기)
     * 실제 저장하지 않고 변환 결과만 확인
     */
    @PostMapping("/coordinate-conversion")
    @Operation(summary = "좌표 변환", description = "위경도를 격자 좌표로 변환합니다. (미리보기 기능)")
    public ResponseEntity<CustomResponse<RegionResDTO.CoordinateConversion>> convertCoordinates(
            @Valid @RequestBody RegionReqDTO.CoordinateConversion request) {

        log.info("좌표 변환 API 호출: lat={}, lon={}", request.latitude(), request.longitude());

        RegionResDTO.CoordinateConversion response = regionService.convertCoordinates(request);

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 모든 지역 조회
     */
    @GetMapping
    @Operation(summary = "지역 목록 조회", description = "등록된 모든 지역 목록을 조회합니다.")
    public ResponseEntity<CustomResponse<RegionResDTO.RegionList>> getAllRegions() {

        log.info("지역 목록 조회 API 호출");

        RegionResDTO.RegionList response = regionService.getAllRegions();

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 특정 지역 상세 조회
     */
    @GetMapping("/{regionId}")
    @Operation(summary = "지역 상세 조회", description = "특정 지역의 상세 정보를 조회합니다.")
    public ResponseEntity<CustomResponse<RegionResDTO.RegionInfo>> getRegion(
            @Parameter(description = "지역 ID", required = true)
            @PathVariable Long regionId) {

        log.info("지역 상세 조회 API 호출: regionId={}", regionId);

        RegionResDTO.RegionInfo response = regionService.getRegionById(regionId);

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 지역 검색
     */
    @GetMapping("/search")
    @Operation(summary = "지역 검색", description = "지역명으로 검색합니다.")
    public ResponseEntity<CustomResponse<RegionResDTO.RegionSearchResult>> searchRegions(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String keyword) {

        log.info("지역 검색 API 호출: keyword={}", keyword);

        RegionResDTO.RegionSearchResult response = regionService.searchRegions(keyword);

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 지역 삭제
     */
    @DeleteMapping("/{regionId}")
    @Operation(summary = "지역 삭제", description = "지역을 삭제합니다. 연관된 모든 날씨 데이터도 함께 삭제됩니다.")
    public ResponseEntity<CustomResponse<RegionResDTO.DeleteRegionResponse>> deleteRegion(
            @Parameter(description = "지역 ID", required = true)
            @PathVariable Long regionId) {

        log.info("지역 삭제 API 호출: regionId={}", regionId);

        RegionResDTO.DeleteRegionResponse response = regionService.deleteRegion(regionId);

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }
}

