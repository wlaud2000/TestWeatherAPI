package com.study.demo.testweatherapi.domain.weather.converter;

import com.study.demo.testweatherapi.domain.weather.dto.response.WeatherSyncResDTO;
import com.study.demo.testweatherapi.domain.weather.entity.RawMediumTermWeather;
import com.study.demo.testweatherapi.domain.weather.entity.RawShortTermWeather;
import com.study.demo.testweatherapi.domain.weather.entity.Region;
import com.study.demo.testweatherapi.domain.weather.entity.enums.WeatherType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WeatherSyncConverter {

    // ==== 기상청 API 응답 파싱 관련 ====

    /**
     * 기상청 단기예보 JSON 응답을 RawShortTermWeather 엔티티로 변환
     */
    public static List<RawShortTermWeather> parseShortTermWeatherResponse(
            String jsonResponse, Region region) {

        List<RawShortTermWeather> results = new ArrayList<>();

        try {
            // JSON 파싱 로직 (실제로는 ObjectMapper 사용)
            // 여기서는 간단히 예시로 작성

            // 기상청 API 응답에서 items 배열 추출
            // 같은 baseDate, baseTime, fcstDate, fcstTime의 데이터들을 그룹핑
            Map<String, WeatherDataGroup> groupedData = groupWeatherData(jsonResponse);

            for (WeatherDataGroup group : groupedData.values()) {
                if (group.isComplete()) {  // TMP, SKY, POP, PTY, PCP 모두 있는 경우만
                    RawShortTermWeather weather = RawShortTermWeather.builder()
                            .region(region)
                            .baseDate(LocalDate.parse(group.baseDate, DateTimeFormatter.ofPattern("yyyyMMdd")))
                            .baseTime(group.baseTime)
                            .fcstDate(LocalDate.parse(group.fcstDate, DateTimeFormatter.ofPattern("yyyyMMdd")))
                            .fcstTime(group.fcstTime)
                            .tmp(Double.parseDouble(group.tmp))
                            .sky(convertSkyValue(group.sky))
                            .pop(Double.parseDouble(group.pop))
                            .pty(convertPtyValue(group.pty))
                            .pcp(convertPcpValue(group.pcp))
                            .build();

                    results.add(weather);
                }
            }

        } catch (Exception e) {
            log.error("단기예보 JSON 파싱 실패: regionId={}", region.getId(), e);
            throw new RuntimeException("단기예보 데이터 파싱 중 오류 발생", e);
        }

        return results;
    }

    /**
     * 기상청 중기예보 텍스트 응답을 RawMediumTermWeather 엔티티로 변환
     */
    public static List<RawMediumTermWeather> parseMediumTermWeatherResponse(
            String landResponse, String tempResponse, Region region) {

        List<RawMediumTermWeather> results = new ArrayList<>();

        try {
            // 중기 육상 예보 파싱 (SKY, POP)
            Map<String, MediumTermLandData> landDataMap = parseMediumTermLandData(landResponse);

            // 중기 기온 예보 파싱 (MIN, MAX)
            Map<String, MediumTermTempData> tempDataMap = parseMediumTermTempData(tempResponse);

            // 같은 tmfc, tmef 키로 데이터 매칭
            for (String key : landDataMap.keySet()) {
                MediumTermLandData landData = landDataMap.get(key);
                MediumTermTempData tempData = tempDataMap.get(key);

                if (landData != null && tempData != null) {
                    RawMediumTermWeather weather = RawMediumTermWeather.builder()
                            .region(region)
                            // 수정: LocalDate.parse()는 이미 LocalDate를 반환하므로 .toLocalDate() 제거
                            .tmfc(LocalDate.parse(landData.tmfc().substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd")))
                            .tmef(LocalDate.parse(landData.tmef().substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd")))
                            .sky(convertMediumTermSkyValue(landData.sky()))
                            .pop(Double.parseDouble(landData.rnSt()))
                            .minTmp(Double.parseDouble(tempData.min()))
                            .maxTmp(Double.parseDouble(tempData.max()))
                            .build();

                    results.add(weather);
                }
            }

        } catch (Exception e) {
            log.error("중기예보 텍스트 파싱 실패: regionId={}", region.getId(), e);
            throw new RuntimeException("중기예보 데이터 파싱 중 오류 발생", e);
        }

        return results;
    }

    // ==== 동기화 결과 변환 관련 ====

    /**
     * 단기예보 동기화 결과 생성
     */
    public static WeatherSyncResDTO.ShortTermSyncResult toShortTermSyncResult(
            int totalRegions, int successfulRegions, int failedRegions,
            int totalDataPoints, int newDataPoints, int updatedDataPoints,
            LocalDate baseDate, String baseTime,
            LocalDateTime startTime, LocalDateTime endTime,
            List<WeatherSyncResDTO.RegionSyncResult> regionResults,
            List<String> errorMessages) {

        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        String message = String.format(
                "단기예보 동기화 완료: 성공 %d/%d 지역, 신규 %d개, 업데이트 %d개 데이터 처리",
                successfulRegions, totalRegions, newDataPoints, updatedDataPoints);

        return WeatherSyncResDTO.ShortTermSyncResult.builder()
                .totalRegions(totalRegions)
                .successfulRegions(successfulRegions)
                .failedRegions(failedRegions)
                .totalDataPoints(totalDataPoints)
                .newDataPoints(newDataPoints)
                .updatedDataPoints(updatedDataPoints)
                .baseDate(baseDate)
                .baseTime(baseTime)
                .processingStartTime(startTime)
                .processingEndTime(endTime)
                .processingDurationMs(durationMs)
                .regionResults(regionResults)
                .errorMessages(errorMessages)
                .message(message)
                .build();
    }

    /**
     * 중기예보 동기화 결과 생성
     */
    public static WeatherSyncResDTO.MediumTermSyncResult toMediumTermSyncResult(
            int totalRegions, int successfulRegions, int failedRegions,
            int totalDataPoints, int newDataPoints, int updatedDataPoints,
            LocalDate tmfc, LocalDateTime startTime, LocalDateTime endTime,
            List<WeatherSyncResDTO.RegionSyncResult> regionResults,
            List<String> errorMessages) {

        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        String message = String.format(
                "중기예보 동기화 완료: 성공 %d/%d 지역, 신규 %d개, 업데이트 %d개 데이터 처리",
                successfulRegions, totalRegions, newDataPoints, updatedDataPoints);

        return WeatherSyncResDTO.MediumTermSyncResult.builder()
                .totalRegions(totalRegions)
                .successfulRegions(successfulRegions)
                .failedRegions(failedRegions)
                .totalDataPoints(totalDataPoints)
                .newDataPoints(newDataPoints)
                .updatedDataPoints(updatedDataPoints)
                .tmfc(tmfc)
                .processingStartTime(startTime)
                .processingEndTime(endTime)
                .processingDurationMs(durationMs)
                .regionResults(regionResults)
                .errorMessages(errorMessages)
                .message(message)
                .build();
    }

    /**
     * 추천 생성 결과 생성
     */
    public static WeatherSyncResDTO.RecommendationGenerationResult toRecommendationGenerationResult(
            int totalRegions, int successfulRegions, int failedRegions,
            int totalRecommendations, int newRecommendations, int updatedRecommendations,
            LocalDate startDate, LocalDate endDate,
            LocalDateTime startTime, LocalDateTime endTime,
            List<WeatherSyncResDTO.RegionRecommendationResult> regionResults,
            Map<WeatherType, Integer> weatherStats,
            List<String> errorMessages) {

        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        String message = String.format(
                "추천 정보 생성 완료: 성공 %d/%d 지역, 신규 %d개, 업데이트 %d개 추천 생성",
                successfulRegions, totalRegions, newRecommendations, updatedRecommendations);

        WeatherSyncResDTO.WeatherTypeStatistics weatherTypeStats = WeatherSyncResDTO.WeatherTypeStatistics.builder()
                .clearWeatherCount(weatherStats.getOrDefault(WeatherType.CLEAR, 0))
                .cloudyWeatherCount(weatherStats.getOrDefault(WeatherType.CLOUDY, 0))
                .snowWeatherCount(weatherStats.getOrDefault(WeatherType.SNOW, 0))
                .detailedStats(weatherStats)
                .build();

        return WeatherSyncResDTO.RecommendationGenerationResult.builder()
                .totalRegions(totalRegions)
                .successfulRegions(successfulRegions)
                .failedRegions(failedRegions)
                .totalRecommendations(totalRecommendations)
                .newRecommendations(newRecommendations)
                .updatedRecommendations(updatedRecommendations)
                .startDate(startDate)
                .endDate(endDate)
                .processingStartTime(startTime)
                .processingEndTime(endTime)
                .processingDurationMs(durationMs)
                .regionResults(regionResults)
                .weatherStats(weatherTypeStats)
                .errorMessages(errorMessages)
                .message(message)
                .build();
    }

    /**
     * 지역별 동기화 결과 생성
     */
    public static WeatherSyncResDTO.RegionSyncResult toRegionSyncResult(
            Long regionId, String regionName, boolean success,
            int dataPointsProcessed, int newDataPoints, int updatedDataPoints,
            String errorMessage, long processingTimeMs) {

        return WeatherSyncResDTO.RegionSyncResult.builder()
                .regionId(regionId)
                .regionName(regionName)
                .success(success)
                .dataPointsProcessed(dataPointsProcessed)
                .newDataPoints(newDataPoints)
                .updatedDataPoints(updatedDataPoints)
                .errorMessage(errorMessage)
                .processingTimeMs(processingTimeMs)
                .build();
    }

    // ==== 내부 유틸리티 메서드들 ====

    /**
     * 단기예보 JSON 응답에서 날씨 데이터 그룹핑
     */
    private static Map<String, WeatherDataGroup> groupWeatherData(String jsonResponse) {
        // 실제 구현에서는 ObjectMapper를 사용해서 JSON 파싱
        // 여기서는 간단한 예시만 제공
        return Map.of(); // 실제 구현 필요
    }

    /**
     * 중기 육상 예보 텍스트 파싱
     */
    private static Map<String, MediumTermLandData> parseMediumTermLandData(String response) {
        Map<String, MediumTermLandData> result = Map.of();

        try {
            // #START7777과 #7777END 사이의 데이터 추출
            Pattern pattern = Pattern.compile("#START7777(.*?)#7777END", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(response);

            if (matcher.find()) {
                String data = matcher.group(1);
                String[] lines = data.split("\n");

                for (String line : lines) {
                    if (line.trim().isEmpty() || line.startsWith("#")) continue;

                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 11) {
                        MediumTermLandData landData = new MediumTermLandData(
                                parts[0],  // REG_ID
                                parts[1],  // TM_FC
                                parts[2],  // TM_EF
                                parts[6],  // SKY
                                parts[10]  // RN_ST
                        );

                        String key = parts[1] + "_" + parts[2]; // tmfc_tmef
                        result = Map.of(key, landData); // 실제로는 HashMap 사용
                    }
                }
            }
        } catch (Exception e) {
            log.error("중기 육상 예보 파싱 실패", e);
        }

        return result;
    }

    /**
     * 중기 기온 예보 텍스트 파싱
     */
    private static Map<String, MediumTermTempData> parseMediumTermTempData(String response) {
        // 중기 육상 예보와 유사한 파싱 로직
        return Map.of(); // 실제 구현 필요
    }

    /**
     * 단기예보 하늘상태 값 변환
     */
    private static String convertSkyValue(String skyCode) {
        return switch (skyCode) {
            case "1" -> "맑음";
            case "3" -> "구름많음";
            case "4" -> "흐림";
            default -> "알수없음";
        };
    }

    /**
     * 단기예보 강수형태 값 변환
     */
    private static String convertPtyValue(String ptyCode) {
        return switch (ptyCode) {
            case "0" -> "없음";
            case "1" -> "비";
            case "2" -> "비/눈";
            case "3" -> "눈";
            case "5" -> "빗방울";
            case "6" -> "빗방울눈날림";
            case "7" -> "눈날림";
            default -> "알수없음";
        };
    }

    /**
     * 단기예보 강수량 값 변환
     */
    private static Double convertPcpValue(String pcpValue) {
        if ("강수없음".equals(pcpValue)) {
            return 0.0;
        }

        try {
            // "1.0mm" 형태에서 숫자만 추출
            String numericValue = pcpValue.replaceAll("[^0-9.]", "");
            return Double.parseDouble(numericValue);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * 중기예보 하늘상태 값 변환
     */
    private static String convertMediumTermSkyValue(String skyCode) {
        return switch (skyCode) {
            case "WB01" -> "맑음";
            case "WB03" -> "구름많음";
            case "WB04" -> "흐림";
            case "WB13", "WB12" -> "눈";
            default -> "알수없음";
        };
    }

    // ==== 내부 데이터 클래스들 ====

    /**
     * 단기예보 데이터 그룹
     */
    private static class WeatherDataGroup {
        String baseDate, baseTime, fcstDate, fcstTime;
        String tmp, sky, pop, pty, pcp;

        boolean isComplete() {
            return tmp != null && sky != null && pop != null && pty != null && pcp != null;
        }
    }

    /**
     * 중기 육상 예보 데이터
     */
    private record MediumTermLandData(String regId, String tmfc, String tmef, String sky, String rnSt) {}

    /**
     * 중기 기온 예보 데이터
     */
    private record MediumTermTempData(String regId, String tmfc, String tmef, String min, String max) {}
}
