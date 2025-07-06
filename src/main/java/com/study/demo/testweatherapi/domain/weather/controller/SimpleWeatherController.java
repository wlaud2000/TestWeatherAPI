package com.study.demo.testweatherapi.domain.weather.controller;

import com.study.demo.testweatherapi.domain.weather.dto.request.WeatherReqDTO;
import com.study.demo.testweatherapi.domain.weather.dto.response.WeatherResDTO;
import com.study.demo.testweatherapi.domain.weather.exception.WeatherErrorCode;
import com.study.demo.testweatherapi.domain.weather.exception.WeatherException;
import com.study.demo.testweatherapi.domain.weather.service.WeatherRecommendationService;
import com.study.demo.testweatherapi.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 간편 조회용 API (별도 컨트롤러)
 * 복잡한 파라미터 없이 간단하게 조회
 */
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@Validated
@Tag(name = "간편 날씨 조회 API", description = "간단한 날씨 추천 조회 API")
class SimpleWeatherController {

    private final WeatherRecommendationService weatherRecommendationService;

    /**
     * 오늘 날씨 추천 조회
     */
    @GetMapping("/today/{regionId}")
    @Operation(summary = "오늘 날씨 추천", description = "특정 지역의 오늘 날씨 추천을 조회합니다.")
    public ResponseEntity<CustomResponse<WeatherResDTO.WeatherRecommendation>> getTodayRecommendation(
            @PathVariable @NotNull @Positive Long regionId) {

        LocalDate today = LocalDate.now();

        // 정적 팩토리 메서드 사용
        WeatherReqDTO.GetRecommendation request = WeatherReqDTO.GetRecommendation.of(regionId, today);

        WeatherResDTO.WeatherRecommendation response = weatherRecommendationService.getRecommendation(request);
        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 내일 날씨 추천 조회
     */
    @GetMapping("/tomorrow/{regionId}")
    @Operation(summary = "내일 날씨 추천", description = "특정 지역의 내일 날씨 추천을 조회합니다.")
    public ResponseEntity<CustomResponse<WeatherResDTO.WeatherRecommendation>> getTomorrowRecommendation(
            @PathVariable @NotNull @Positive Long regionId) {

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // 정적 팩토리 메서드 사용
        WeatherReqDTO.GetRecommendation request = WeatherReqDTO.GetRecommendation.of(regionId, tomorrow);

        WeatherResDTO.WeatherRecommendation response = weatherRecommendationService.getRecommendation(request);
        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 이번 주 날씨 추천 조회
     */
    @GetMapping("/this-week/{regionId}")
    @Operation(summary = "이번 주 날씨 추천", description = "특정 지역의 이번 주 날씨 추천을 조회합니다.")
    public ResponseEntity<CustomResponse<WeatherResDTO.WeeklyRecommendation>> getThisWeekRecommendation(
            @PathVariable @NotNull @Positive Long regionId) {

        LocalDate today = LocalDate.now();

        // 정적 팩토리 메서드 사용
        WeatherReqDTO.GetWeeklyRecommendation request =
                WeatherReqDTO.GetWeeklyRecommendation.of(regionId, today);

        WeatherResDTO.WeeklyRecommendation response =
                weatherRecommendationService.getWeeklyRecommendation(request);

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }
}
