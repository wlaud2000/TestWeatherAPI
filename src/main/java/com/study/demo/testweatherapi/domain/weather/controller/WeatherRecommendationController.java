package com.study.demo.testweatherapi.domain.weather.controller;

import com.study.demo.testweatherapi.domain.weather.dto.request.WeatherReqDTO;
import com.study.demo.testweatherapi.domain.weather.dto.response.WeatherResDTO;
import com.study.demo.testweatherapi.domain.weather.exception.WeatherErrorCode;
import com.study.demo.testweatherapi.domain.weather.exception.WeatherException;
import com.study.demo.testweatherapi.domain.weather.service.WeatherRecommendationService;
import com.study.demo.testweatherapi.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/weather/recommendation")
@RequiredArgsConstructor
@Validated
@Tag(name = "날씨 추천 조회 API", description = "사용자용 날씨 기반 데이트 코스 추천 조회 API")
public class WeatherRecommendationController {

    private final WeatherRecommendationService weatherRecommendationService;

    /**
     * 특정 지역, 특정 날짜의 날씨 추천 정보 조회
     */
    @GetMapping("/{regionId}")
    @Operation(summary = "날씨 추천 조회",
            description = "특정 지역의 특정 날짜 날씨 기반 데이트 코스 추천을 조회합니다.")
    public ResponseEntity<CustomResponse<WeatherResDTO.WeatherRecommendation>> getRecommendation(
            @Parameter(description = "지역 ID", required = true)
            @PathVariable @NotNull @Positive Long regionId,

            @Parameter(description = "조회할 날짜 (YYYY-MM-DD)", required = true, example = "2025-07-04")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("날씨 추천 조회 API 호출: regionId={}, date={}", regionId, date);

            WeatherReqDTO.GetRecommendation request = new WeatherReqDTO.GetRecommendation(regionId, date);
            WeatherResDTO.WeatherRecommendation response = weatherRecommendationService.getRecommendation(request);

            return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 특정 지역의 주간 날씨 추천 정보 조회 (7일치)
     */
    @GetMapping("/{regionId}/weekly")
    @Operation(summary = "주간 날씨 추천 조회",
            description = "특정 지역의 7일간 날씨 기반 데이트 코스 추천을 조회합니다.")
    public ResponseEntity<CustomResponse<WeatherResDTO.WeeklyRecommendation>> getWeeklyRecommendation(
            @Parameter(description = "지역 ID", required = true)
            @PathVariable @NotNull @Positive Long regionId,

            @Parameter(description = "시작 날짜 (YYYY-MM-DD)", required = true, example = "2025-07-04")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {

        log.info("주간 날씨 추천 조회 API 호출: regionId={}, startDate={}", regionId, startDate);

        WeatherReqDTO.GetWeeklyRecommendation request =
                new WeatherReqDTO.GetWeeklyRecommendation(regionId, startDate);
        WeatherResDTO.WeeklyRecommendation response =
                weatherRecommendationService.getWeeklyRecommendation(request);

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 날짜 범위로 날씨 추천 정보 조회
     */
    @GetMapping("/{regionId}/date-range")
    @Operation(summary = "날짜 범위 날씨 추천 조회",
            description = "특정 지역의 특정 날짜 범위 날씨 기반 데이트 코스 추천을 조회합니다.")
    public ResponseEntity<CustomResponse<WeatherResDTO.WeeklyRecommendation>> getRecommendationByDateRange(
            @Parameter(description = "지역 ID", required = true)
            @PathVariable @NotNull @Positive Long regionId,

            @Parameter(description = "시작 날짜 (YYYY-MM-DD)", required = true, example = "2025-07-04")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "종료 날짜 (YYYY-MM-DD)", required = true, example = "2025-07-10")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("날짜 범위 날씨 추천 조회 API 호출: regionId={}, startDate={}, endDate={}",
                regionId, startDate, endDate);

        WeatherReqDTO.GetRecommendationByDateRange request =
                new WeatherReqDTO.GetRecommendationByDateRange(regionId, startDate, endDate);
        WeatherResDTO.WeeklyRecommendation response =
                weatherRecommendationService.getRecommendationByDateRange(request);

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 특정 지역의 최근 날씨 추천 정보 조회
     */
    @GetMapping("/{regionId}/latest")
    @Operation(summary = "최근 날씨 추천 조회",
            description = "특정 지역의 최근 7일간 날씨 추천 정보를 조회합니다.")
    public ResponseEntity<CustomResponse<List<WeatherResDTO.WeatherRecommendationSummary>>> getLatestRecommendations(
            @Parameter(description = "지역 ID", required = true)
            @PathVariable @NotNull @Positive Long regionId) {

        log.info("최근 날씨 추천 조회 API 호출: regionId={}", regionId);

        List<WeatherResDTO.WeatherRecommendationSummary> response =
                weatherRecommendationService.getLatestRecommendations(regionId);

        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    /**
     * 추천 데이터 존재 여부 확인
     */
    @GetMapping("/{regionId}/check")
    @Operation(summary = "추천 데이터 존재 확인",
            description = "특정 지역의 특정 날짜에 추천 데이터가 있는지 확인합니다.")
    public ResponseEntity<CustomResponse<Boolean>> checkRecommendationExists(
            @Parameter(description = "지역 ID", required = true)
            @PathVariable @NotNull @Positive Long regionId,

            @Parameter(description = "확인할 날짜 (YYYY-MM-DD)", required = true, example = "2025-07-04")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("추천 데이터 존재 확인 API 호출: regionId={}, date={}", regionId, date);

        boolean exists = weatherRecommendationService.hasRecommendationData(regionId, date);

        return ResponseEntity.ok(CustomResponse.onSuccess(exists));
    }
}

