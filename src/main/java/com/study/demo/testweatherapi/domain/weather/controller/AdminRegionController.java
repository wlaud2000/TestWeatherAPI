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

    // ==== 지역코드 관리 API ====

    /**
     * 새로운 지역코드 등록
     */
    @PostMapping("/codes")
    @Operation(summary = "지역코드 등록", description = "새로운 지역코드를 등록합니다.")
    public ResponseEntity<CustomResponse<RegionResDTO.CreateRegionCodeResponse>> createRegionCode(
            @Valid @RequestBody RegionReqDTO.CreateRegionCode request) {

        log.info("지역코드 등록 API 호출: {}", request.name());

        RegionResDTO.CreateRegionCodeResponse response = regionService.createRegionCode(request);

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 모든 지역코드 조회
     */
    @GetMapping("/codes")
    @Operation(summary = "지역코드 목록 조회", description = "등록된 모든 지역코드 목록을 조회합니다.")
    public ResponseEntity<CustomResponse<RegionResDTO.RegionCodeList>> getAllRegionCodes() {

        log.info("지역코드 목록 조회 API 호출");

        RegionResDTO.RegionCodeList response = regionService.getAllRegionCodes();

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 특정 지역코드를 사용하는 지역들 조회
     */
    @GetMapping("/codes/{regionCodeId}/regions")
    @Operation(summary = "지역코드별 지역 조회", description = "특정 지역코드를 사용하는 모든 지역을 조회합니다.")
    public ResponseEntity<CustomResponse<RegionResDTO.RegionsByCodeResponse>> getRegionsByCode(
            @Parameter(description = "지역코드 ID", required = true)
            @PathVariable Long regionCodeId) {

        log.info("지역코드별 지역 조회 API 호출: regionCodeId={}", regionCodeId);

        RegionResDTO.RegionsByCodeResponse response = regionService.getRegionsByCode(regionCodeId);

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 지역코드 삭제
     */
    @DeleteMapping("/codes/{regionCodeId}")
    @Operation(summary = "지역코드 삭제", description = "지역코드를 삭제합니다. (해당 코드를 사용하는 지역이 없어야 함)")
    public ResponseEntity<CustomResponse<RegionResDTO.DeleteRegionCodeResponse>> deleteRegionCode(
            @Parameter(description = "지역코드 ID", required = true)
            @PathVariable Long regionCodeId) {

        log.info("지역코드 삭제 API 호출: regionCodeId={}", regionCodeId);

        RegionResDTO.DeleteRegionCodeResponse response = regionService.deleteRegionCode(regionCodeId);

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    // ==== 지역 관리 API ====

    /**
     * 새로운 지역 등록 (기존 지역코드 사용)
     */
    @PostMapping
    @Operation(summary = "지역 등록", description = "기존 지역코드를 사용하여 새로운 지역을 등록합니다. 위경도는 자동으로 격자 좌표로 변환됩니다.")
    public ResponseEntity<CustomResponse<RegionResDTO.CreateRegionResponse>> createRegion(
            @Valid @RequestBody RegionReqDTO.CreateRegion request) {

        log.info("지역 등록 API 호출: {}", request.name());

        RegionResDTO.CreateRegionResponse response = regionService.createRegion(request);

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 새로운 지역 등록 (새 지역코드와 함께)
     */
    @PostMapping("/with-new-code")
    @Operation(summary = "지역+지역코드 동시 등록", description = "새로운 지역코드와 함께 지역을 등록합니다.")
    public ResponseEntity<CustomResponse<RegionResDTO.CreateRegionResponse>> createRegionWithNewCode(
            @Valid @RequestBody RegionReqDTO.CreateRegionWithNewCode request) {

        log.info("지역+지역코드 등록 API 호출: {}", request.name());

        RegionResDTO.CreateRegionResponse response = regionService.createRegionWithNewCode(request);

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

