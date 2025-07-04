package com.study.demo.testweatherapi.domain.weather.controller;

import com.study.demo.testweatherapi.domain.weather.dto.response.WeatherResDTO;
import com.study.demo.testweatherapi.domain.weather.service.WeatherRecommendationService;
import com.study.demo.testweatherapi.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 관리자용 날씨 추천 조회 API
 */
@RestController
@RequestMapping("/api/admin/weather/recommendation")
@RequiredArgsConstructor
@Validated
@Tag(name = "날씨 추천 관리 API", description = "관리자용 날씨 추천 조회 및 관리 API")
class AdminWeatherRecommendationController {

    private final WeatherRecommendationService weatherRecommendationService;

    /**
     * 특정 날짜의 모든 지역 날씨 추천 정보 조회
     */
    @GetMapping("/all")
    @Operation(summary = "전체 지역 날씨 추천 조회",
            description = "특정 날짜의 모든 지역 날씨 추천 정보를 조회합니다.")
    public ResponseEntity<CustomResponse<List<WeatherResDTO.WeatherRecommendationSummary>>> getAllRecommendationsByDate(
            @Parameter(description = "조회할 날짜 (YYYY-MM-DD)", required = true, example = "2025-07-04")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<WeatherResDTO.WeatherRecommendationSummary> response =
                weatherRecommendationService.getAllRecommendationsByDate(date);

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }
}
