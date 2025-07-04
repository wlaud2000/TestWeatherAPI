package com.study.demo.testweatherapi.domain.weather.controller;

import com.study.demo.testweatherapi.domain.weather.dto.response.RegionResDTO;
import com.study.demo.testweatherapi.domain.weather.service.RegionService;
import com.study.demo.testweatherapi.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 일반 사용자용 지역 조회 API (별도 컨트롤러)
 * /api/regions 경로로 구성
 */
@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
@Tag(name = "지역 조회 API", description = "일반 사용자용 지역 조회 API")
public class PublicRegionController {

    private final RegionService regionService;

    /**
     * 지역 목록 조회 (일반 사용자용)
     */
    @GetMapping
    @Operation(summary = "지역 목록 조회", description = "등록된 지역 목록을 조회합니다.")
    public ResponseEntity<CustomResponse<RegionResDTO.RegionList>> getRegions() {
        RegionResDTO.RegionList response = regionService.getAllRegions();
        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 지역 검색 (일반 사용자용)
     */
    @GetMapping("/search")
    @Operation(summary = "지역 검색", description = "지역명으로 검색합니다.")
    public ResponseEntity<CustomResponse<RegionResDTO.RegionSearchResult>> searchRegions(
            @RequestParam String keyword) {
        RegionResDTO.RegionSearchResult response = regionService.searchRegions(keyword);
        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }
}
