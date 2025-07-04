package com.study.demo.testweatherapi.domain.weather.dto.response;

import com.study.demo.testweatherapi.domain.weather.entity.enums.PrecipCategory;
import com.study.demo.testweatherapi.domain.weather.entity.enums.TempCategory;
import com.study.demo.testweatherapi.domain.weather.entity.enums.WeatherType;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class WeatherResDTO {

    /**
     * 날씨 추천 정보 (단일 날짜)
     */
    @Builder
    public record WeatherRecommendation(
            Long recommendationId,
            LocalDate forecastDate,
            RegionInfo region,
            WeatherInfo weather,
            RecommendationInfo recommendation,
            LocalDateTime updatedAt
    ) {
    }


    /**
     * 지역 정보 (날씨 관련 응답용)
     */
    @Builder
    public record RegionInfo(
            Long regionId,
            String regionName,
            String landRegCode,
            String tempRegCode
    ) {
    }

    /**
     * 날씨 정보
     */
    @Builder
    public record WeatherInfo(
            WeatherType weatherType,      // CLEAR, CLOUDY, SNOW
            TempCategory tempCategory,    // CHILLY, COOL, MILD, HOT
            PrecipCategory precipCategory, // NONE, LIGHT, HEAVY
            String weatherDescription,    // "맑음", "흐림", "눈" 등 한글 설명
            String tempDescription,       // "쌀쌀함", "선선함", "적당함", "무더움"
            String precipDescription      // "없음", "약간 비옴", "강우 많음"
    ) {
    }

    /**
     * 추천 정보 (메시지, 이모지, 키워드)
     */
    @Builder
    public record RecommendationInfo(
            String message,
            String emoji,
            List<String> keywords
    ) {
    }

    /**
     * 주간 날씨 추천 정보 (7일치)
     */
    @Builder
    public record WeeklyRecommendation(
            RegionInfo region,
            LocalDate startDate,
            LocalDate endDate,
            List<DailyWeatherRecommendation> dailyRecommendations,
            int totalDays,
            String message
    ) {
    }

    /**
     * 일별 날씨 추천 (주간 조회용 - 간소화된 버전)
     */
    @Builder
    public record DailyWeatherRecommendation(
            LocalDate forecastDate,
            WeatherType weatherType,
            TempCategory tempCategory,
            PrecipCategory precipCategory,
            String message,
            String emoji,
            List<String> keywords,
            boolean hasRecommendation  // 해당 날짜에 추천 데이터가 있는지 여부
    ) {
    }

    /**
     * 날씨 추천 조회 실패 응답 (데이터 없음)
     */
    @Builder
    public record WeatherRecommendationNotFound(
            Long regionId,
            String regionName,
            LocalDate requestedDate,
            String message,
            List<String> suggestions  // 대안 제안 (예: 가장 가까운 날짜의 데이터)
    ) {
    }

    /**
     * 날씨 추천 통계 정보 (관리자용)
     */
    @Builder
    public record WeatherRecommendationStats(
            Long regionId,
            String regionName,
            LocalDate startDate,
            LocalDate endDate,
            int totalRecommendations,
            WeatherTypeStats weatherTypeStats,
            TempCategoryStats tempCategoryStats
    ) {
    }

    /**
     * 날씨 타입별 통계
     */
    @Builder
    public record WeatherTypeStats(
            int clearDays,
            int cloudyDays,
            int snowDays
    ) {
    }

    /**
     * 기온 카테고리별 통계
     */
    @Builder
    public record TempCategoryStats(
            int chillyDays,
            int coolDays,
            int mildDays,
            int hotDays
    ) {
    }

    /**
     * 간단한 날씨 정보 (목록 조회용)
     */
    @Builder
    public record WeatherRecommendationSummary(
            Long recommendationId,
            LocalDate forecastDate,
            String regionName,
            WeatherType weatherType,
            String emoji,
            String shortMessage  // 메시지의 첫 50자
    ) {
    }

    /**
     * 지역별 최신 날씨 추천 목록
     */
    @Builder
    public record RegionWeatherSummary(
            List<WeatherRecommendationSummary> recommendations,
            int totalCount,
            LocalDateTime lastUpdated
    ) {
    }
}
