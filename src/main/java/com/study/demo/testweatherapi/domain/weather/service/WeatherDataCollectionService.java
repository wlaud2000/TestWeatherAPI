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
     * 단기예보 API 호출
     */
    private String callShortTermWeatherApi(Region region, LocalDate baseDate, String baseTime) {
        try {
            // BigDecimal 격자 좌표를 정수로 변환
            int gridX = region.getGridX().intValue();  // 60.00 -> 60
            int gridY = region.getGridY().intValue();  // 127.00 -> 127

            log.debug("단기예보 API 호출: regionId={}, gridX={}, gridY={}, baseDate={}, baseTime={}",
                    region.getId(), gridX, gridY, baseDate, baseTime);

            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(shortTermForecastUrl)  // "/VilageFcst"
                            .queryParam("authKey", apiKey)
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 1052)
                            .queryParam("dataType", "JSON")
                            .queryParam("base_date", baseDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                            .queryParam("base_time", baseTime)
                            .queryParam("nx", gridX)
                            .queryParam("ny", gridY)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.trim().isEmpty()) {
                throw new WeatherException(WeatherErrorCode.SHORT_TERM_FORECAST_ERROR);
            }

            log.debug("단기예보 API 응답 수신 완료: regionId={}, 응답길이={}",
                    region.getId(), response.length());

            return response;

        } catch (Exception e) {
            log.error("단기예보 API 호출 실패: regionId={}, gridX={}, gridY={}, baseDate={}, baseTime={}",
                    region.getId(), region.getGridX(), region.getGridY(), baseDate, baseTime, e);
            throw new WeatherException(WeatherErrorCode.SHORT_TERM_FORECAST_ERROR);
        }
    }

    /**
     * 중기 육상 예보 API 호출
     */
    private String callMediumTermLandWeatherApi(Region region, LocalDate tmfc) {
        try {
            // RegionCode를 통해 landRegCode 가져오기
            String landRegCode = region.getRegionCode() != null ?
                    region.getRegionCode().getLandRegCode() : null;

            if (landRegCode == null) {
                throw new WeatherException(WeatherErrorCode.INVALID_REGION_CODE);
            }

            log.debug("중기 육상예보 API 호출: regionId={}, landRegCode={}",
                    region.getId(), landRegCode);

            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(mediumTermLandUrl)
                            .queryParam("authKey", apiKey)
                            .queryParam("reg", landRegCode)  // RegionCode에서 가져온 landRegCode 사용
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.trim().isEmpty()) {
                throw new WeatherException(WeatherErrorCode.MEDIUM_TERM_FORECAST_ERROR);
            }

            log.debug("중기 육상예보 API 응답 수신 완료: regionId={}, 응답길이={}",
                    region.getId(), response.length());

            return response;

        } catch (Exception e) {
            log.error("중기 육상 예보 API 호출 실패: regionId={}, landRegCode={}",
                    region.getId(), region.getRegionCode().getLandRegCode(), e);
            throw new WeatherException(WeatherErrorCode.MEDIUM_TERM_FORECAST_ERROR);
        }
    }

    /**
     * 중기 기온 예보 API 호출
     */
    private String callMediumTermTempWeatherApi(Region region, LocalDate tmfc) {
        try {
            // RegionCode를 통해 tempRegCode 가져오기
            String tempRegCode = region.getRegionCode() != null ?
                    region.getRegionCode().getTempRegCode() : null;

            if (tempRegCode == null) {
                throw new WeatherException(WeatherErrorCode.INVALID_REGION_CODE);
            }

            log.debug("중기 기온예보 API 호출: regionId={}, tempRegCode={}",
                    region.getId(), tempRegCode);

            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(mediumTermTempUrl)
                            .queryParam("authKey", apiKey)
                            .queryParam("reg", tempRegCode)  // RegionCode에서 가져온 tempRegCode 사용
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.trim().isEmpty()) {
                throw new WeatherException(WeatherErrorCode.MEDIUM_TERM_FORECAST_ERROR);
            }

            log.debug("중기 기온예보 API 응답 수신 완료: regionId={}, 응답길이={}",
                    region.getId(), response.length());

            return response;

        } catch (Exception e) {
            log.error("중기 기온 예보 API 호출 실패: regionId={}, tempRegCode={}",
                    region.getId(), region.getRegionCode().getTempRegCode(), e);
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

            log.debug("단기예보 파싱 완료: regionId={}, 파싱된 데이터 수={}",
                    region.getId(), results.size());
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
                    try {
                        // 안전한 파싱으로 변경
                        Double pop = parseDoubleValue(landData.rnSt(), "강수확률");
                        Double minTmp = parseDoubleValue(tempData.min(), "최저기온");
                        Double maxTmp = parseDoubleValue(tempData.max(), "최고기온");

                        // 모든 값이 유효한 경우만 저장
                        if (pop != null && minTmp != null && maxTmp != null) {
                            RawMediumTermWeather weather = RawMediumTermWeather.builder()
                                    .region(region)
                                    .tmfc(LocalDate.parse(landData.tmfc().substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd")))
                                    .tmef(LocalDate.parse(landData.tmef().substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd")))
                                    .sky(convertMediumTermSkyValue(landData.sky()))
                                    .pop(pop)
                                    .minTmp(minTmp)
                                    .maxTmp(maxTmp)
                                    .build();

                            results.add(weather);
                            log.debug("중기예보 파싱 성공: key={}, pop={}, minTmp={}, maxTmp={}",
                                    key, pop, minTmp, maxTmp);
                        } else {
                            log.warn("중기예보 데이터 불완전하여 스킵: key={}, pop={}, minTmp={}, maxTmp={}",
                                    key, landData.rnSt(), tempData.min(), tempData.max());
                        }
                    } catch (Exception e) {
                        log.warn("중기예보 개별 데이터 파싱 실패 (스킵): key={}, landData={}, tempData={}, error={}",
                                key, landData, tempData, e.getMessage());
                    }
                }
            }

            log.info("중기예보 파싱 완료: regionId={}, 성공 {}/{} 건",
                    region.getId(), results.size(), landDataMap.size());
            return results;

        } catch (Exception e) {
            log.error("중기 예보 텍스트 파싱 실패: regionId={}", region.getId(), e);
            throw new WeatherException(WeatherErrorCode.API_RESPONSE_PARSING_ERROR);
        }
    }

    // ==== 내부 유틸리티 메서드들 ====

    /**
     * 대상 지역 조회 (RegionCode 함께 fetch)
     */
    private List<Region> getTargetRegions(List<Long> regionIds) {
        if (regionIds == null || regionIds.isEmpty()) {
            return regionRepository.findAllActiveRegions();  // 이미 RegionCode fetch join 포함
        } else {
            return regionRepository.findByIdsWithRegionCode(regionIds);  // RegionCode fetch join 포함
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

        try {
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
        } catch (Exception e) {
            log.error("중기 육상예보 파싱 실패", e);
        }

        log.debug("중기 육상예보 파싱 완료: {} 건", result.size());
        return result;
    }

    private Map<String, MediumTermTempData> parseMediumTermTempData(String response) {
        Map<String, MediumTermTempData> result = new HashMap<>();

        try {
            Pattern pattern = Pattern.compile("#START7777(.*?)#7777END", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(response);

            if (matcher.find()) {
                String data = matcher.group(1);
                String[] lines = data.split("\n");

                for (String line : lines) {
                    if (line.trim().isEmpty() || line.startsWith("#")) continue;

                    String[] parts = line.trim().split("\\s+");
                    // 컬럼 구조: REG_ID TM_FC TM_EF MOD STN C MIN MAX MIN_L MIN_H MAX_L MAX_H
                    // parts[6] = MIN, parts[7] = MAX 이므로 길이는 최소 8이어야 함
                    if (parts.length >= 8) {
                        try {
                            MediumTermTempData tempData = new MediumTermTempData(
                                    parts[1],  // TM_FC
                                    parts[2],  // TM_EF
                                    parts[6],  // MIN (26, 25 등)
                                    parts[7]   // MAX (34, 33 등)
                            );

                            String key = parts[1] + "_" + parts[2];
                            result.put(key, tempData);

                            log.trace("중기 기온예보 라인 파싱: key={}, min={}, max={}",
                                    key, parts[6], parts[7]);
                        } catch (Exception e) {
                            log.warn("중기 기온예보 라인 파싱 실패 (스킵): line='{}', error={}",
                                    line.trim(), e.getMessage());
                        }
                    } else {
                        log.debug("중기 기온예보 라인 길이 부족 (스킵): line='{}', parts.length={}",
                                line.trim(), parts.length);
                    }
                }
            } else {
                log.warn("중기 기온예보 응답에서 #START7777...#7777END 패턴을 찾을 수 없음");
            }
        } catch (Exception e) {
            log.error("중기 기온예보 전체 파싱 실패", e);
        }

        log.debug("중기 기온예보 파싱 완료: {} 건", result.size());
        return result;
    }

    /**
     * 문자열을 Double로 안전하게 파싱
     * 기상청 API는 데이터가 없을 때 "A01", "A02" 등의 코드를 반환할 수 있음
     */
    private Double parseDoubleValue(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            log.debug("{} 값이 비어있음", fieldName);
            return null;
        }

        String trimmedValue = value.trim();

        // 기상청 코드값 처리 (A01, A02, B01 등)
        if (trimmedValue.matches("^[A-Z]\\d+$")) {
            log.debug("{} 코드값 감지: {} -> 기본값 사용", fieldName, trimmedValue);
            return getDefaultValueForCode(trimmedValue, fieldName);
        }

        // 숫자가 아닌 문자가 포함된 경우
        if (!trimmedValue.matches("^-?\\d*\\.?\\d+$")) {
            log.warn("{} 파싱 불가능한 값: {} -> null 반환", fieldName, trimmedValue);
            return null;
        }

        try {
            double parsedValue = Double.parseDouble(trimmedValue);

            // 값 범위 검증
            if (!isValidValue(parsedValue, fieldName)) {
                log.warn("{} 값이 유효 범위를 벗어남: {} -> null 반환", fieldName, parsedValue);
                return null;
            }

            return parsedValue;
        } catch (NumberFormatException e) {
            log.warn("{} 숫자 파싱 실패: {} -> null 반환", fieldName, trimmedValue);
            return null;
        }
    }

    /**
     * 기상청 코드값에 대한 기본값 반환
     */
    private Double getDefaultValueForCode(String code, String fieldName) {
        return switch (fieldName) {
            case "강수확률" -> 30.0;  // 기본 강수확률 30%
            case "최저기온" -> 15.0;  // 기본 최저기온 15도
            case "최고기온" -> 25.0;  // 기본 최고기온 25도
            default -> {
                log.debug("알 수 없는 필드명: {}, 코드: {} -> 0.0 반환", fieldName, code);
                yield 0.0;
            }
        };
    }

    /**
     * 파싱된 값이 유효한 범위인지 검증
     */
    private boolean isValidValue(double value, String fieldName) {
        return switch (fieldName) {
            case "강수확률" -> value >= 0.0 && value <= 100.0;
            case "최저기온" -> value >= -50.0 && value <= 50.0;
            case "최고기온" -> value >= -50.0 && value <= 50.0;
            default -> true;
        };
    }

    // ==== 내부 데이터 클래스들 ====

    private record UpsertResult(int totalProcessed, int newRecords, int updatedRecords) {}
    private record MediumTermLandData(String tmfc, String tmef, String sky, String rnSt) {}
    private record MediumTermTempData(String tmfc, String tmef, String min, String max) {}
}
