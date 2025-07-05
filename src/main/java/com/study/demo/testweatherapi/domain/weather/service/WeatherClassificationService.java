package com.study.demo.testweatherapi.domain.weather.service;

import com.study.demo.testweatherapi.domain.weather.entity.RawMediumTermWeather;
import com.study.demo.testweatherapi.domain.weather.entity.RawShortTermWeather;
import com.study.demo.testweatherapi.domain.weather.entity.enums.PrecipCategory;
import com.study.demo.testweatherapi.domain.weather.entity.enums.TempCategory;
import com.study.demo.testweatherapi.domain.weather.entity.enums.WeatherType;
import com.study.demo.testweatherapi.global.config.WeatherClassificationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherClassificationService {

    private final WeatherClassificationConfig config;

    /**
     * 단기 예보 데이터를 분류
     * 특정 지역의 특정 날짜 데이터를 가장 대표적인 값으로 분류
     */
    public WeatherClassificationResult classifyShortTermWeather(
            List<RawShortTermWeather> shortTermData, Long regionId, LocalDate targetDate) {

        if (shortTermData.isEmpty()) {
            log.warn("단기 예보 데이터가 없습니다: regionId={}, date={}", regionId, targetDate);
            return createDefaultClassification();
        }

        try {
            // 해당 날짜의 대표 데이터 선택 (가장 최신 기준시각의 정오 데이터 우선)
            RawShortTermWeather representativeData = selectRepresentativeShortTermData(shortTermData, targetDate);

            // 각 카테고리별 분류
            WeatherType weatherType = classifyWeatherTypeFromShortTerm(representativeData);
            TempCategory tempCategory = classifyTempCategory(representativeData.getTmp());
            PrecipCategory precipCategory = classifyPrecipCategory(representativeData.getPop(), representativeData.getPcp());

            log.debug("단기 예보 분류 완료: regionId={}, date={}, weather={}, temp={}, precip={}, 기온={}°C, 강수확률={}%",
                    regionId, targetDate, weatherType, tempCategory, precipCategory,
                    representativeData.getTmp(), representativeData.getPop());

            return new WeatherClassificationResult(weatherType, tempCategory, precipCategory,
                    representativeData.getTmp(), representativeData.getPop(),
                    representativeData.getPcp(), "단기예보");

        } catch (Exception e) {
            log.error("단기 예보 분류 중 오류 발생: regionId={}, date={}", regionId, targetDate, e);
            return createDefaultClassification();
        }
    }

    /**
     * 중기 예보 데이터를 분류
     */
    public WeatherClassificationResult classifyMediumTermWeather(
            List<RawMediumTermWeather> mediumTermData, Long regionId, LocalDate targetDate) {

        if (mediumTermData.isEmpty()) {
            log.warn("중기 예보 데이터가 없습니다: regionId={}, date={}", regionId, targetDate);
            return createDefaultClassification();
        }

        try {
            // 해당 날짜의 대표 데이터 선택 (가장 최신 발표시각)
            RawMediumTermWeather representativeData = selectRepresentativeMediumTermData(mediumTermData, targetDate);

            // 각 카테고리별 분류
            WeatherType weatherType = classifyWeatherTypeFromMediumTerm(representativeData);
            TempCategory tempCategory = classifyTempCategoryFromRange(representativeData.getMinTmp(), representativeData.getMaxTmp());
            PrecipCategory precipCategory = classifyPrecipCategory(representativeData.getPop(), 0.0); // 중기예보는 강수량 없음

            log.debug("중기 예보 분류 완료: regionId={}, date={}, weather={}, temp={}, precip={}, 최저={}°C, 최고={}°C, 강수확률={}%",
                    regionId, targetDate, weatherType, tempCategory, precipCategory,
                    representativeData.getMinTmp(), representativeData.getMaxTmp(), representativeData.getPop());

            double avgTemp = (representativeData.getMinTmp() + representativeData.getMaxTmp()) / 2.0;
            return new WeatherClassificationResult(weatherType, tempCategory, precipCategory,
                    avgTemp, representativeData.getPop(), 0.0, "중기예보");

        } catch (Exception e) {
            log.error("중기 예보 분류 중 오류 발생: regionId={}, date={}", regionId, targetDate, e);
            return createDefaultClassification();
        }
    }

    /**
     * 단기 예보에서 대표 데이터 선택
     * 우선순위: 가장 최신 기준시각 > 정오(1200) 시간대 > 오후 시간대
     */
    private RawShortTermWeather selectRepresentativeShortTermData(
            List<RawShortTermWeather> dataList, LocalDate targetDate) {

        return dataList.stream()
                .filter(data -> data.getFcstDate().equals(targetDate))
                .sorted((a, b) -> {
                    // 1. 기준시각 최신순
                    int baseDateCompare = b.getBaseDate().compareTo(a.getBaseDate());
                    if (baseDateCompare != 0) return baseDateCompare;

                    int baseTimeCompare = b.getBaseTime().compareTo(a.getBaseTime());
                    if (baseTimeCompare != 0) return baseTimeCompare;

                    // 2. 정오 시간대 우선 (1200)
                    int aTimeScore = getTimeScore(a.getFcstTime());
                    int bTimeScore = getTimeScore(b.getFcstTime());
                    return Integer.compare(bTimeScore, aTimeScore);
                })
                .findFirst()
                .orElse(dataList.get(0));
    }

    /**
     * 중기 예보에서 대표 데이터 선택
     * 가장 최신 발표시각(tmfc) 우선
     */
    private RawMediumTermWeather selectRepresentativeMediumTermData(
            List<RawMediumTermWeather> dataList, LocalDate targetDate) {

        return dataList.stream()
                .filter(data -> data.getTmef().equals(targetDate))
                .sorted((a, b) -> b.getTmfc().compareTo(a.getTmfc()))
                .findFirst()
                .orElse(dataList.get(0));
    }

    /**
     * 예보 시간별 우선순위 점수
     * 정오(1200) > 오후(1500, 1800) > 오전(0900) > 기타
     */
    private int getTimeScore(String fcstTime) {
        return switch (fcstTime) {
            case "1200" -> 100;  // 정오 - 최우선
            case "1500" -> 90;   // 오후 3시
            case "1800" -> 85;   // 오후 6시
            case "0900" -> 80;   // 오전 9시
            case "2100" -> 75;   // 오후 9시
            case "0600" -> 70;   // 오전 6시
            default -> 50;       // 기타
        };
    }

    /**
     * 단기 예보에서 날씨 타입 분류
     * 우선순위: PTY(강수형태) > SKY(하늘상태)
     */
    private WeatherType classifyWeatherTypeFromShortTerm(RawShortTermWeather data) {
        String pty = data.getPty();
        String sky = data.getSky();

        // 1. 강수형태 우선 확인
        if ("눈".equals(pty) || "비/눈".equals(pty)) {
            return WeatherType.SNOW;
        }

        // 2. 하늘상태로 분류
        return switch (sky) {
            case "맑음" -> WeatherType.CLEAR;
            case "구름많음", "흐림" -> WeatherType.CLOUDY;
            default -> WeatherType.CLOUDY; // 기본값
        };
    }

    /**
     * 중기 예보에서 날씨 타입 분류
     */
    private WeatherType classifyWeatherTypeFromMediumTerm(RawMediumTermWeather data) {
        String sky = data.getSky();

        return switch (sky) {
            case "맑음" -> WeatherType.CLEAR;
            case "구름많음", "흐림" -> WeatherType.CLOUDY;
            case "눈" -> WeatherType.SNOW;
            default -> WeatherType.CLOUDY; // 기본값
        };
    }

    /**
     * 기온 카테고리 분류 (단일 온도 기준) - 설정 기반
     */
    private TempCategory classifyTempCategory(Double temperature) {
        if (temperature == null) return TempCategory.MILD;

        WeatherClassificationConfig.TemperatureThresholds thresholds = config.getTemperature();

        if (temperature < thresholds.getChillyCoolBoundary()) {
            return TempCategory.CHILLY;     // 쌀쌀함
        } else if (temperature <= thresholds.getCoolMildBoundary()) {
            return TempCategory.COOL;       // 선선함
        } else if (temperature <= thresholds.getMildHotBoundary()) {
            return TempCategory.MILD;       // 적당함
        } else {
            return TempCategory.HOT;        // 무더움
        }
    }

    /**
     * 기온 카테고리 분류 (최저-최고 온도 범위 기준) - 설정 기반
     * 평균 온도로 판단하되, 최고 온도가 무더움 기준 이상이면 무더움으로 분류
     */
    private TempCategory classifyTempCategoryFromRange(Double minTemp, Double maxTemp) {
        if (minTemp == null || maxTemp == null) return TempCategory.MILD;

        WeatherClassificationConfig.TemperatureThresholds thresholds = config.getTemperature();

        // 최고 온도가 무더움 기준 이상이면 무더움
        if (maxTemp > thresholds.getMildHotBoundary()) {
            return TempCategory.HOT;
        }

        // 평균 온도로 판단
        double avgTemp = (minTemp + maxTemp) / 2.0;
        return classifyTempCategory(avgTemp);
    }

    /**
     * 강수 카테고리 분류 - 설정 기반
     * 강수확률과 실제 강수량을 모두 고려
     */
    private PrecipCategory classifyPrecipCategory(Double precipProbability, Double precipAmount) {
        if (precipProbability == null) precipProbability = 0.0;
        if (precipAmount == null) precipAmount = 0.0;

        WeatherClassificationConfig.PrecipitationThresholds thresholds = config.getPrecipitation();

        // 실제 강수량이 많으면 강수확률과 관계없이 우선 처리
        if (precipAmount >= thresholds.getHeavyAmountThreshold()) {
            return PrecipCategory.HEAVY;
        } else if (precipAmount >= thresholds.getLightAmountThreshold()) {
            return PrecipCategory.LIGHT;
        }

        // 강수확률 기준 분류
        if (precipProbability >= thresholds.getLightHeavyProbability()) {
            return PrecipCategory.HEAVY;
        } else if (precipProbability >= thresholds.getNoneLightProbability()) {
            return PrecipCategory.LIGHT;
        } else {
            return PrecipCategory.NONE;
        }
    }

    /**
     * 기본 분류 결과 생성 (데이터가 없거나 오류 시)
     */
    private WeatherClassificationResult createDefaultClassification() {
        return new WeatherClassificationResult(
                WeatherType.CLOUDY, TempCategory.MILD, PrecipCategory.NONE,
                20.0, 30.0, 0.0, "기본값");
    }

    /**
     * 날씨 분류 결과를 담는 클래스
     */
    public record WeatherClassificationResult(
            WeatherType weatherType,
            TempCategory tempCategory,
            PrecipCategory precipCategory,
            Double temperature,        // 대표 온도
            Double precipProbability,  // 강수확률
            Double precipAmount,       // 강수량
            String dataSource          // 데이터 출처 ("단기예보" or "중기예보")
    ) {
        /**
         * 분류 결과가 유효한지 확인
         */
        public boolean isValid() {
            return weatherType != null && tempCategory != null && precipCategory != null;
        }

        /**
         * 분류 결과 요약 문자열
         */
        public String getSummary() {
            return String.format("%s, %s, %s (%.1f°C, %.0f%%, %.1fmm) [%s]",
                    weatherType, tempCategory, precipCategory,
                    temperature, precipProbability, precipAmount, dataSource);
        }

        /**
         * 설정 기반 분류 기준 정보 포함 요약
         */
        public String getDetailedSummary(WeatherClassificationConfig config) {
            return String.format(
                    "분류결과: %s | 온도: %.1f°C (%s) | 강수: %.0f%% + %.1fmm (%s) | 출처: %s",
                    weatherType, temperature, tempCategory,
                    precipProbability, precipAmount, precipCategory, dataSource
            );
        }
    }
}
