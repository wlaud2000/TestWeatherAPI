package com.study.demo.testweatherapi.domain.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.demo.testweatherapi.domain.weather.converter.WeatherSyncConverter;
import com.study.demo.testweatherapi.domain.weather.dto.response.WeatherSyncResDTO;
import com.study.demo.testweatherapi.domain.weather.entity.RawMediumTermWeather;
import com.study.demo.testweatherapi.domain.weather.entity.RawShortTermWeather;
import com.study.demo.testweatherapi.domain.weather.entity.Region;
import com.study.demo.testweatherapi.domain.weather.exception.WeatherErrorCode;
import com.study.demo.testweatherapi.domain.weather.exception.WeatherException;
import com.study.demo.testweatherapi.domain.weather.repository.RawMediumTermWeatherRepository;
import com.study.demo.testweatherapi.domain.weather.repository.RawShortTermWeatherRepository;
import com.study.demo.testweatherapi.domain.weather.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherDataCollectionService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final RegionRepository regionRepository;
    private final RawShortTermWeatherRepository shortTermWeatherRepository;
    private final RawMediumTermWeatherRepository mediumTermWeatherRepository;

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.short-term-forecast-url}")
    private String shortTermForecastUrl;

    @Value("${weather.api.medium-term-land-url}")
    private String mediumTermLandUrl;

    @Value("${weather.api.medium-term-temp-url}")
    private String mediumTermTempUrl;

    /**
     * 단기 예보 데이터 수집 및 저장
     */
    @Transactional
    public WeatherSyncResDTO.ShortTermSyncResult collectShortTermWeatherData(
            List<Long> regionIds, LocalDate baseDate, String baseTime, boolean forceUpdate) {

        LocalDateTime startTime = LocalDateTime.now();
        log.info("단기 예보 수집 시작: regionIds={}, baseDate={}, baseTime={}", regionIds, baseDate, baseTime);

        List<Region> targetRegions = getTargetRegions(regionIds);
        List<WeatherSyncResDTO.RegionSyncResult> regionResults = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        int totalDataPoints = 0, newDataPoints = 0, updatedDataPoints = 0;
        int successfulRegions = 0, failedRegions = 0;

        for (Region region : targetRegions) {
            long regionStartTime = System.currentTimeMillis();

            try {
                log.debug("지역 {} 단기 예보 수집 시작", region.getName());

                // 1. 기상청 API 호출
                String response = callShortTermWeatherApi(region, baseDate, baseTime);

                // 2. JSON 응답 파싱
                List<RawShortTermWeather> weatherDataList = parseShortTermWeatherResponse(response, region);

                // 3. 데이터베이스 저장 (Upsert)
                UpsertResult upsertResult = upsertShortTermWeatherData(weatherDataList, forceUpdate);

                totalDataPoints += upsertResult.totalProcessed();
                newDataPoints += upsertResult.newRecords();
                updatedDataPoints += upsertResult.updatedRecords();
                successfulRegions++;

                long processingTime = System.currentTimeMillis() - regionStartTime;
                regionResults.add(WeatherSyncConverter.toRegionSyncResult(
                        region.getId(), region.getName(), true,
                        upsertResult.totalProcessed(), upsertResult.newRecords(), upsertResult.updatedRecords(),
                        null, processingTime));

                log.debug("지역 {} 단기 예보 수집 완료: 신규 {}, 업데이트 {}",
                        region.getName(), upsertResult.newRecords(), upsertResult.updatedRecords());

            } catch (Exception e) {
                failedRegions++;
                long processingTime = System.currentTimeMillis() - regionStartTime;
                String errorMessage = String.format("지역 %s 처리 실패: %s", region.getName(), e.getMessage());
                errorMessages.add(errorMessage);

                regionResults.add(WeatherSyncConverter.toRegionSyncResult(
                        region.getId(), region.getName(), false, 0, 0, 0,
                        errorMessage, processingTime));

                log.error("지역 {} 단기 예보 수집 실패", region.getName(), e);
            }
        }

        LocalDateTime endTime = LocalDateTime.now();
        log.info("단기 예보 수집 완료: 성공 {}/{} 지역, 신규 {}, 업데이트 {} 데이터",
                successfulRegions, targetRegions.size(), newDataPoints, updatedDataPoints);

        return WeatherSyncConverter.toShortTermSyncResult(
                targetRegions.size(), successfulRegions, failedRegions,
                totalDataPoints, newDataPoints, updatedDataPoints,
                baseDate, baseTime, startTime, endTime, regionResults, errorMessages);
    }

    /**
     * 중기 예보 데이터 수집 및 저장
     */
    @Transactional
    public WeatherSyncResDTO.MediumTermSyncResult collectMediumTermWeatherData(
            List<Long> regionIds, LocalDate tmfc, boolean forceUpdate) {

        LocalDateTime startTime = LocalDateTime.now();
        log.info("중기 예보 수집 시작: regionIds={}, tmfc={}", regionIds, tmfc);

        List<Region> targetRegions = getTargetRegions(regionIds);
        List<WeatherSyncResDTO.RegionSyncResult> regionResults = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        int totalDataPoints = 0, newDataPoints = 0, updatedDataPoints = 0;
        int successfulRegions = 0, failedRegions = 0;

        for (Region region : targetRegions) {
            long regionStartTime = System.currentTimeMillis();

            try {
                log.debug("지역 {} 중기 예보 수집 시작", region.getName());

                // 1. 기상청 API 호출 (육상 예보 + 기온 예보)
                CompletableFuture<String> landResponseFuture = CompletableFuture.supplyAsync(() ->
                        callMediumTermLandWeatherApi(region, tmfc));
                CompletableFuture<String> tempResponseFuture = CompletableFuture.supplyAsync(() ->
                        callMediumTermTempWeatherApi(region, tmfc));

                String landResponse = landResponseFuture.get();
                String tempResponse = tempResponseFuture.get();

                // 2. 텍스트 응답 파싱
                List<RawMediumTermWeather> weatherDataList = parseMediumTermWeatherResponse(
                        landResponse, tempResponse, region);

                // 3. 데이터베이스 저장 (Upsert)
                UpsertResult upsertResult = upsertMediumTermWeatherData(weatherDataList, forceUpdate);

                totalDataPoints += upsertResult.totalProcessed();
                newDataPoints += upsertResult.newRecords();
                updatedDataPoints += upsertResult.updatedRecords();
                successfulRegions++;

                long processingTime = System.currentTimeMillis() - regionStartTime;
                regionResults.add(WeatherSyncConverter.toRegionSyncResult(
                        region.getId(), region.getName(), true,
                        upsertResult.totalProcessed(), upsertResult.newRecords(), upsertResult.updatedRecords(),
                        null, processingTime));

                log.debug("지역 {} 중기 예보 수집 완료: 신규 {}, 업데이트 {}",
                        region.getName(), upsertResult.newRecords(), upsertResult.updatedRecords());

            } catch (Exception e) {
                failedRegions++;
                long processingTime = System.currentTimeMillis() - regionStartTime;
                String errorMessage = String.format("지역 %s 처리 실패: %s", region.getName(), e.getMessage());
                errorMessages.add(errorMessage);

                regionResults.add(WeatherSyncConverter.toRegionSyncResult(
                        region.getId(), region.getName(), false, 0, 0, 0,
                        errorMessage, processingTime));

                log.error("지역 {} 중기 예보 수집 실패", region.getName(), e);
            }
        }

        LocalDateTime endTime = LocalDateTime.now();
        log.info("중기 예보 수집 완료: 성공 {}/{} 지역, 신규 {}, 업데이트 {} 데이터",
                successfulRegions, targetRegions.size(), newDataPoints, updatedDataPoints);

        return WeatherSyncConverter.toMediumTermSyncResult(
                targetRegions.size(), successfulRegions, failedRegions,
                totalDataPoints, newDataPoints, updatedDataPoints,
                tmfc, startTime, endTime, regionResults, errorMessages);
    }

    /**
     * 단기 예보 API 호출
     */
    private String callShortTermWeatherApi(Region region, LocalDate baseDate, String baseTime) {
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(shortTermForecastUrl)
                            .queryParam("authKey", apiKey)
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 1000)
                            .queryParam("dataType", "JSON")
                            .queryParam("base_date", baseDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                            .queryParam("base_time", baseTime)
                            .queryParam("nx", region.getGridX())
                            .queryParam("ny", region.getGridY())
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.trim().isEmpty()) {
                throw new WeatherException(WeatherErrorCode.SHORT_TERM_FORECAST_ERROR);
            }

            return response;

        } catch (Exception e) {
            log.error("단기 예보 API 호출 실패: regionId={}, baseDate={}, baseTime={}",
                    region.getId(), baseDate, baseTime, e);
            throw new WeatherException(WeatherErrorCode.SHORT_TERM_FORECAST_ERROR);
        }
    }

    /**
     * 중기 육상 예보 API 호출
     */
    private String callMediumTermLandWeatherApi(Region region, LocalDate tmfc) {
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(mediumTermLandUrl)
                            .queryParam("authKey", apiKey)
                            .queryParam("reg", region.getRegCode())
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.trim().isEmpty()) {
                throw new WeatherException(WeatherErrorCode.MEDIUM_TERM_FORECAST_ERROR);
            }

            return response;

        } catch (Exception e) {
            log.error("중기 육상 예보 API 호출 실패: regionId={}, regCode={}", region.getId(), region.getRegCode(), e);
            throw new WeatherException(WeatherErrorCode.MEDIUM_TERM_FORECAST_ERROR);
        }
    }

    /**
     * 중기 기온 예보 API 호출
     */
    private String callMediumTermTempWeatherApi(Region region, LocalDate tmfc) {
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(mediumTermTempUrl)
                            .queryParam("authKey", apiKey)
                            .queryParam("reg", region.getRegCode())
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.trim().isEmpty()) {
                throw new WeatherException(WeatherErrorCode.MEDIUM_TERM_FORECAST_ERROR);
            }

            return response;

        } catch (Exception e) {
            log.error("중기 기온 예보 API 호출 실패: regionId={}, regCode={}", region.getId(), region.getRegCode(), e);
            throw new WeatherException(WeatherErrorCode.MEDIUM_TERM_FORECAST_ERROR);
        }
    }

    /**
     * 단기 예보 JSON 응답 파싱
     */
    private List<RawShortTermWeather> parseShortTermWeatherResponse(String jsonResponse, Region region) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode items = root.path("response").path("body").path("items").path("item");

            if (!items.isArray()) {
                return Collections.emptyList();
            }

            // 동일한 시간대별로 데이터 그룹화
            Map<String, Map<String, String>> groupedData = new HashMap<>();

            for (JsonNode item : items) {
                String baseDate = item.path("baseDate").asText();
                String baseTime = item.path("baseTime").asText();
                String fcstDate = item.path("fcstDate").asText();
                String fcstTime = item.path("fcstTime").asText();
                String category = item.path("category").asText();
                String fcstValue = item.path("fcstValue").asText();

                String key = String.join("_", baseDate, baseTime, fcstDate, fcstTime);
                groupedData.computeIfAbsent(key, k -> new HashMap<>()).put(category, fcstValue);
            }

            List<RawShortTermWeather> results = new ArrayList<>();

            for (Map.Entry<String, Map<String, String>> entry : groupedData.entrySet()) {
                String[] keyParts = entry.getKey().split("_");
                Map<String, String> values = entry.getValue();

                // 필요한 카테고리(TMP, SKY, POP, PTY, PCP)가 모두 있는지 확인
                if (hasRequiredCategories(values)) {
                    RawShortTermWeather weather = RawShortTermWeather.builder()
                            .region(region)
                            .baseDate(LocalDate.parse(keyParts[0], DateTimeFormatter.ofPattern("yyyyMMdd")))
                            .baseTime(keyParts[1])
                            .fcstDate(LocalDate.parse(keyParts[2], DateTimeFormatter.ofPattern("yyyyMMdd")))
                            .fcstTime(keyParts[3])
                            .tmp(Double.parseDouble(values.get("TMP")))
                            .sky(convertSkyValue(values.get("SKY")))
                            .pop(Double.parseDouble(values.get("POP")))
                            .pty(convertPtyValue(values.get("PTY")))
                            .pcp(convertPcpValue(values.get("PCP")))
                            .build();

                    results.add(weather);
                }
            }

            return results;

        } catch (Exception e) {
            log.error("단기 예보 JSON 파싱 실패: regionId={}", region.getId(), e);
            throw new WeatherException(WeatherErrorCode.API_RESPONSE_PARSING_ERROR);
        }
    }

    /**
     * 중기 예보 텍스트 응답 파싱
     */
    private List<RawMediumTermWeather> parseMediumTermWeatherResponse(
            String landResponse, String tempResponse, Region region) {
        try {
            Map<String, MediumTermLandData> landDataMap = parseMediumTermLandData(landResponse);
            Map<String, MediumTermTempData> tempDataMap = parseMediumTermTempData(tempResponse);

            List<RawMediumTermWeather> results = new ArrayList<>();

            for (String key : landDataMap.keySet()) {
                MediumTermLandData landData = landDataMap.get(key);
                MediumTermTempData tempData = tempDataMap.get(key);

                if (landData != null && tempData != null) {
                    RawMediumTermWeather weather = RawMediumTermWeather.builder()
                            .region(region)
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

            return results;

        } catch (Exception e) {
            log.error("중기 예보 텍스트 파싱 실패: regionId={}", region.getId(), e);
            throw new WeatherException(WeatherErrorCode.API_RESPONSE_PARSING_ERROR);
        }
    }

    // ==== 내부 유틸리티 메서드들 ====

    private List<Region> getTargetRegions(List<Long> regionIds) {
        if (regionIds == null || regionIds.isEmpty()) {
            return regionRepository.findAllActiveRegions();
        } else {
            return regionRepository.findAllById(regionIds);
        }
    }

    private boolean hasRequiredCategories(Map<String, String> values) {
        return values.containsKey("TMP") && values.containsKey("SKY") &&
                values.containsKey("POP") && values.containsKey("PTY") && values.containsKey("PCP");
    }

    private String convertSkyValue(String skyCode) {
        return switch (skyCode) {
            case "1" -> "맑음";
            case "3" -> "구름많음";
            case "4" -> "흐림";
            default -> "알수없음";
        };
    }

    private String convertPtyValue(String ptyCode) {
        return switch (ptyCode) {
            case "0" -> "없음";
            case "1" -> "비";
            case "2" -> "비/눈";
            case "3" -> "눈";
            default -> "알수없음";
        };
    }

    private Double convertPcpValue(String pcpValue) {
        if ("강수없음".equals(pcpValue)) return 0.0;
        try {
            return Double.parseDouble(pcpValue.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String convertMediumTermSkyValue(String skyCode) {
        return switch (skyCode) {
            case "WB01" -> "맑음";
            case "WB03" -> "구름많음";
            case "WB04" -> "흐림";
            case "WB13", "WB12" -> "눈";
            default -> "알수없음";
        };
    }

    private UpsertResult upsertShortTermWeatherData(List<RawShortTermWeather> weatherDataList, boolean forceUpdate) {
        int totalProcessed = 0, newRecords = 0, updatedRecords = 0;

        for (RawShortTermWeather weatherData : weatherDataList) {
            Optional<RawShortTermWeather> existingOpt = shortTermWeatherRepository
                    .findByRegionIdAndBaseDateAndBaseTimeAndFcstDateAndFcstTime(
                            weatherData.getRegion().getId(),
                            weatherData.getBaseDate(),
                            weatherData.getBaseTime(),
                            weatherData.getFcstDate(),
                            weatherData.getFcstTime()
                    );

            if (existingOpt.isEmpty()) {
                shortTermWeatherRepository.save(weatherData);
                newRecords++;
            } else if (forceUpdate) {
                // 기존 데이터 업데이트 로직
                updatedRecords++;
            }
            totalProcessed++;
        }

        return new UpsertResult(totalProcessed, newRecords, updatedRecords);
    }

    private UpsertResult upsertMediumTermWeatherData(List<RawMediumTermWeather> weatherDataList, boolean forceUpdate) {
        int totalProcessed = 0, newRecords = 0, updatedRecords = 0;

        for (RawMediumTermWeather weatherData : weatherDataList) {
            Optional<RawMediumTermWeather> existingOpt = mediumTermWeatherRepository
                    .findByRegionIdAndTmfcAndTmef(
                            weatherData.getRegion().getId(),
                            weatherData.getTmfc(),
                            weatherData.getTmef()
                    );

            if (existingOpt.isEmpty()) {
                mediumTermWeatherRepository.save(weatherData);
                newRecords++;
            } else if (forceUpdate) {
                // 기존 데이터 업데이트 로직
                updatedRecords++;
            }
            totalProcessed++;
        }

        return new UpsertResult(totalProcessed, newRecords, updatedRecords);
    }

    private Map<String, MediumTermLandData> parseMediumTermLandData(String response) {
        Map<String, MediumTermLandData> result = new HashMap<>();

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
                            parts[1],  // TM_FC
                            parts[2],  // TM_EF
                            parts[6],  // SKY
                            parts[10]  // RN_ST
                    );

                    String key = parts[1] + "_" + parts[2];
                    result.put(key, landData);
                }
            }
        }

        return result;
    }

    private Map<String, MediumTermTempData> parseMediumTermTempData(String response) {
        Map<String, MediumTermTempData> result = new HashMap<>();

        Pattern pattern = Pattern.compile("#START7777(.*?)#7777END", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            String data = matcher.group(1);
            String[] lines = data.split("\n");

            for (String line : lines) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 7) {
                    MediumTermTempData tempData = new MediumTermTempData(
                            parts[1],  // TM_FC
                            parts[2],  // TM_EF
                            parts[3],  // MIN
                            parts[4]   // MAX
                    );

                    String key = parts[1] + "_" + parts[2];
                    result.put(key, tempData);
                }
            }
        }

        return result;
    }

    // ==== 내부 데이터 클래스들 ====

    private record UpsertResult(int totalProcessed, int newRecords, int updatedRecords) {}
    private record MediumTermLandData(String tmfc, String tmef, String sky, String rnSt) {}
    private record MediumTermTempData(String tmfc, String tmef, String min, String max) {}
}
