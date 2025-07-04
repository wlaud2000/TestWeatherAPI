package com.study.demo.testweatherapi.domain.weather.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public class WeatherSyncReqDTO {

    /**
     * 단기 예보 동기화 요청 DTO
     */
    public record SyncShortTermWeather(
            @Size(min = 1, message = "최소 1개 이상의 지역이 필요합니다.")
            List<@NotNull @Positive Long> regionIds,

            @NotNull(message = "기준 날짜는 필수입니다.")
            LocalDate baseDate,

            @NotBlank(message = "기준 시간은 필수입니다.")
            @Pattern(regexp = "^(02|05|08|11|14|17|20|23)00$",
                    message = "기준 시간은 0200, 0500, 0800, 1100, 1400, 1700, 2000, 2300 중 하나여야 합니다.")
            String baseTime,

            boolean forceUpdate  // 강제 업데이트 여부
    ) {
        /**
         * 모든 지역 동기화용 생성자
         */
        public static SyncShortTermWeather forAllRegions(LocalDate baseDate, String baseTime) {
            return new SyncShortTermWeather(null, baseDate, baseTime, false);
        }

        /**
         * 특정 지역만 동기화용 생성자
         */
        public static SyncShortTermWeather forSpecificRegions(List<Long> regionIds,
                                                              LocalDate baseDate,
                                                              String baseTime) {
            return new SyncShortTermWeather(regionIds, baseDate, baseTime, false);
        }
    }

    /**
     * 중기 예보 동기화 요청 DTO
     */
    public record SyncMediumTermWeather(
            @Size(min = 1, message = "최소 1개 이상의 지역이 필요합니다.")
            List<@NotNull @Positive Long> regionIds,

            @NotNull(message = "발표 날짜는 필수입니다.")
            LocalDate tmfc,  // 발표시각

            boolean forceUpdate  // 강제 업데이트 여부
    ) {
        /**
         * 모든 지역 동기화용 생성자
         */
        public static SyncMediumTermWeather forAllRegions(LocalDate tmfc) {
            return new SyncMediumTermWeather(null, tmfc, false);
        }

        /**
         * 특정 지역만 동기화용 생성자
         */
        public static SyncMediumTermWeather forSpecificRegions(List<Long> regionIds, LocalDate tmfc) {
            return new SyncMediumTermWeather(regionIds, tmfc, false);
        }
    }

    /**
     * 추천 정보 생성 요청 DTO
     */
    public record GenerateRecommendation(
            @Size(min = 1, message = "최소 1개 이상의 지역이 필요합니다.")
            List<@NotNull @Positive Long> regionIds,

            @NotNull(message = "시작 날짜는 필수입니다.")
            LocalDate startDate,

            @NotNull(message = "종료 날짜는 필수입니다.")
            LocalDate endDate,

            boolean forceRegenerate  // 기존 추천 정보 강제 재생성 여부
    ) {
        /**
         * 날짜 범위 검증
         */
        public GenerateRecommendation {
            if (startDate != null && endDate != null) {
                if (startDate.isAfter(endDate)) {
                    throw new IllegalArgumentException("시작 날짜가 종료 날짜보다 늦을 수 없습니다.");
                }

                if (startDate.plusDays(30).isBefore(endDate)) {
                    throw new IllegalArgumentException("생성 가능한 최대 기간은 30일입니다.");
                }
            }
        }

        /**
         * 모든 지역, 오늘부터 7일간 생성
         */
        public static GenerateRecommendation forAllRegionsWeekly() {
            LocalDate today = LocalDate.now();
            return new GenerateRecommendation(null, today, today.plusDays(6), false);
        }

        /**
         * 특정 지역, 특정 날짜 단일 생성
         */
        public static GenerateRecommendation forSingleDate(List<Long> regionIds, LocalDate date) {
            return new GenerateRecommendation(regionIds, date, date, false);
        }
    }

    /**
     * 데이터 정리 요청 DTO
     */
    public record CleanupWeatherData(
            @NotNull(message = "보관 기간은 필수입니다.")
            @Min(value = 1, message = "보관 기간은 최소 1일입니다.")
            @Max(value = 365, message = "보관 기간은 최대 365일입니다.")
            Integer retentionDays,

            boolean cleanupShortTerm,   // 단기 예보 데이터 정리 여부
            boolean cleanupMediumTerm,  // 중기 예보 데이터 정리 여부
            boolean cleanupRecommendations,  // 추천 데이터 정리 여부

            boolean dryRun  // 실제 삭제하지 않고 개수만 확인
    ) {
        /**
         * 기본 정리 설정 (7일 보관)
         */
        public static CleanupWeatherData defaultCleanup() {
            return new CleanupWeatherData(7, true, true, true, false);
        }

        /**
         * 테스트용 dry run
         */
        public static CleanupWeatherData dryRun(Integer retentionDays) {
            return new CleanupWeatherData(retentionDays, true, true, true, true);
        }
    }

    /**
     * 수동 트리거 요청 DTO (관리자용)
     */
    public record ManualTrigger(
            @NotBlank(message = "작업 타입은 필수입니다.")
            @Pattern(regexp = "^(SHORT_TERM|MEDIUM_TERM|RECOMMENDATION|CLEANUP|ALL)$",
                    message = "올바른 작업 타입을 입력해주세요.")
            String jobType,

            List<Long> targetRegionIds,  // 특정 지역만 처리 (null이면 전체)
            boolean forceExecution,      // 강제 실행 여부
            boolean asyncExecution       // 비동기 실행 여부
    ) {
        /**
         * 전체 동기화 트리거
         */
        public static ManualTrigger allSync() {
            return new ManualTrigger("ALL", null, false, true);
        }

        /**
         * 단기 예보만 동기화
         */
        public static ManualTrigger shortTermSync() {
            return new ManualTrigger("SHORT_TERM", null, false, true);
        }

        /**
         * 추천 정보만 재생성
         */
        public static ManualTrigger recommendationOnly() {
            return new ManualTrigger("RECOMMENDATION", null, false, true);
        }
    }
}
