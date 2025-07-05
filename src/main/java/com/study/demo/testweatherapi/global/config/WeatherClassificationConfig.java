package com.study.demo.testweatherapi.global.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "scheduler.weather.classification")
public class WeatherClassificationConfig {

    /**
     * 단기예보 사용 기준 (일수)
     * 0일부터 이 값까지는 단기예보 데이터 사용
     * 기본값: 2 (0, 1, 2일 = 오늘, 내일, 모레)
     */
    private int shortTermDays = 2;

    /**
     * 중기예보 사용 기준 (일수)
     * shortTermDays + 1일부터 이 값까지는 중기예보 데이터 사용
     * 기본값: 6 (3, 4, 5, 6일)
     */
    private int mediumTermDays = 6;

    /**
     * 기온 분류 기준 (섭씨)
     */
    private TemperatureThresholds temperature = new TemperatureThresholds();

    /**
     * 강수 분류 기준
     */
    private PrecipitationThresholds precipitation = new PrecipitationThresholds();

    @Data
    public static class TemperatureThresholds {
        /**
         * 쌀쌀함/선선함 경계 온도 (기본: 10도)
         * 이 값 미만: CHILLY (쌀쌀함)
         */
        private double chillyCoolBoundary = 10.0;

        /**
         * 선선함/적당함 경계 온도 (기본: 20도)
         * chillyCoolBoundary 이상 ~ 이 값 이하: COOL (선선함)
         */
        private double coolMildBoundary = 20.0;

        /**
         * 적당함/무더움 경계 온도 (기본: 27도)
         * coolMildBoundary 초과 ~ 이 값 이하: MILD (적당함)
         * 이 값 초과: HOT (무더움)
         */
        private double mildHotBoundary = 27.0;
    }

    @Data
    public static class PrecipitationThresholds {
        /**
         * 없음/약간 비옴 경계 강수확률 (기본: 30%)
         * 이 값 미만: NONE (없음)
         */
        private double noneLightProbability = 30.0;

        /**
         * 약간 비옴/강우 많음 경계 강수확률 (기본: 70%)
         * noneLightProbability 이상 ~ 이 값 미만: LIGHT (약간 비옴)
         * 이 값 이상: HEAVY (강우 많음)
         */
        private double lightHeavyProbability = 70.0;

        /**
         * 실제 강수량 기준 - 약간 비옴 경계 (기본: 1.0mm)
         * 이 값 이상이면 강수확률 무관하게 LIGHT 이상으로 분류
         */
        private double lightAmountThreshold = 1.0;

        /**
         * 실제 강수량 기준 - 강우 많음 경계 (기본: 10.0mm)
         * 이 값 이상이면 강수확률 무관하게 HEAVY로 분류
         */
        private double heavyAmountThreshold = 10.0;
    }
}
