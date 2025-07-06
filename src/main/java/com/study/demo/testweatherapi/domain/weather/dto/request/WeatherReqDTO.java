package com.study.demo.testweatherapi.domain.weather.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public class WeatherReqDTO {

    /**
     * 날씨 추천 조회 요청 DTO
     * 특정 지역, 특정 날짜의 추천 정보 조회
     */
    public record GetRecommendation(
            @NotNull(message = "지역 ID는 필수 입력값입니다.")
            @Positive(message = "지역 ID는 양수여야 합니다.")
            Long regionId,

            @NotNull(message = "날짜는 필수 입력값입니다.")
            LocalDate date
    ) {
        /**
         * 날짜 유효성 검증을 포함한 정적 팩토리 메서드
         * 과거 30일 ~ 미래 7일까지만 조회 가능
         */
        public static GetRecommendation of(Long regionId, LocalDate date) {
            // Bean Validation이 null 체크를 하므로 여기서는 날짜 범위만 체크
            if (date != null) {
                LocalDate now = LocalDate.now();
                LocalDate minDate = now.minusDays(30);
                LocalDate maxDate = now.plusDays(7);

                if (date.isBefore(minDate) || date.isAfter(maxDate)) {
                    throw new IllegalArgumentException(
                            "조회 가능한 날짜 범위를 벗어났습니다. (30일 전 ~ 7일 후)");
                }
            }

            return new GetRecommendation(regionId, date);
        }
    }

    /**
     * 주간 날씨 추천 조회 요청 DTO
     * 특정 지역의 7일간 추천 정보 조회
     */
    public record GetWeeklyRecommendation(
            @NotNull(message = "지역 ID는 필수 입력값입니다.")
            @Positive(message = "지역 ID는 양수여야 합니다.")
            Long regionId,

            @NotNull(message = "시작 날짜는 필수 입력값입니다.")
            LocalDate startDate
    ) {
        /**
         * 시작 날짜 유효성 검증을 포함한 정적 팩토리 메서드
         * 과거 7일 ~ 미래 7일까지만 조회 가능
         */
        public static GetWeeklyRecommendation of(Long regionId, LocalDate startDate) {
            if (startDate != null) {
                LocalDate now = LocalDate.now();
                LocalDate minDate = now.minusDays(7);
                LocalDate maxDate = now.plusDays(7);

                if (startDate.isBefore(minDate) || startDate.isAfter(maxDate)) {
                    throw new IllegalArgumentException(
                            "조회 가능한 시작 날짜 범위를 벗어났습니다. (7일 전 ~ 7일 후)");
                }
            }

            return new GetWeeklyRecommendation(regionId, startDate);
        }

        /**
         * 종료 날짜 계산 (시작일 + 6일)
         */
        public LocalDate getEndDate() {
            return startDate.plusDays(6);
        }
    }

    /**
     * 날짜 범위 추천 조회 요청 DTO
     */
    public record GetRecommendationByDateRange(
            @NotNull(message = "지역 ID는 필수 입력값입니다.")
            @Positive(message = "지역 ID는 양수여야 합니다.")
            Long regionId,

            @NotNull(message = "시작 날짜는 필수 입력값입니다.")
            LocalDate startDate,

            @NotNull(message = "종료 날짜는 필수 입력값입니다.")
            LocalDate endDate
    ) {
        /**
         * 날짜 범위 유효성 검증을 포함한 정적 팩토리 메서드
         */
        public static GetRecommendationByDateRange of(Long regionId, LocalDate startDate, LocalDate endDate) {
            if (startDate != null && endDate != null) {
                // 시작일이 종료일보다 늦을 수 없음
                if (startDate.isAfter(endDate)) {
                    throw new IllegalArgumentException("시작 날짜가 종료 날짜보다 늦을 수 없습니다.");
                }

                // 최대 30일까지만 조회 가능
                if (startDate.plusDays(30).isBefore(endDate)) {
                    throw new IllegalArgumentException("조회 가능한 최대 기간은 30일입니다.");
                }

                // 조회 가능한 날짜 범위 체크
                LocalDate now = LocalDate.now();
                LocalDate minDate = now.minusDays(30);
                LocalDate maxDate = now.plusDays(7);

                if (startDate.isBefore(minDate) || endDate.isAfter(maxDate)) {
                    throw new IllegalArgumentException(
                            "조회 가능한 날짜 범위를 벗어났습니다. (30일 전 ~ 7일 후)");
                }
            }

            return new GetRecommendationByDateRange(regionId, startDate, endDate);
        }
    }
}
