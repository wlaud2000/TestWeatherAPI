package com.study.demo.testweatherapi.domain.weather.service;

import com.study.demo.testweatherapi.domain.weather.converter.WeatherSyncConverter;
import com.study.demo.testweatherapi.domain.weather.dto.response.WeatherSyncResDTO;
import com.study.demo.testweatherapi.domain.weather.entity.*;
import com.study.demo.testweatherapi.domain.weather.entity.enums.WeatherType;
import com.study.demo.testweatherapi.domain.weather.exception.WeatherErrorCode;
import com.study.demo.testweatherapi.domain.weather.exception.WeatherException;
import com.study.demo.testweatherapi.domain.weather.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherRecommendationGenerationService {

    private final RegionRepository regionRepository;
    private final RawShortTermWeatherRepository shortTermWeatherRepository;
    private final RawMediumTermWeatherRepository mediumTermWeatherRepository;
    private final WeatherTemplateRepository weatherTemplateRepository;
    private final DailyRecommendationRepository dailyRecommendationRepository;
    private final WeatherClassificationService classificationService;

    /**
     * 날씨 추천 정보 생성 (메인 메서드)
     * 지정된 지역들과 날짜 범위에 대해 추천 정보 생성
     */
    @Transactional
    public WeatherSyncResDTO.RecommendationGenerationResult generateRecommendations(
            List<Long> regionIds, LocalDate startDate, LocalDate endDate, boolean forceRegenerate) {

        LocalDateTime startTime = LocalDateTime.now();
        log.info("추천 정보 생성 시작: regionIds={}, startDate={}, endDate={}, forceRegenerate={}",
                regionIds, startDate, endDate, forceRegenerate);

        List<Region> targetRegions = getTargetRegions(regionIds);
        List<WeatherSyncResDTO.RegionRecommendationResult> regionResults = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        Map<WeatherType, Integer> weatherStats = new HashMap<>();

        int totalRecommendations = 0, newRecommendations = 0, updatedRecommendations = 0;
        int successfulRegions = 0, failedRegions = 0;

        // 템플릿 데이터 미리 로드
        List<WeatherTemplate> allTemplates = weatherTemplateRepository.findAllWithKeywords();
        Map<String, WeatherTemplate> templateMap = createTemplateMap(allTemplates);

        for (Region region : targetRegions) {
            long regionStartTime = System.currentTimeMillis();

            try {
                log.debug("지역 {} 추천 정보 생성 시작", region.getName());

                RegionRecommendationResult regionResult = generateRecommendationsForRegion(
                        region, startDate, endDate, forceRegenerate, templateMap);

                totalRecommendations += regionResult.recommendationsGenerated();
                newRecommendations += regionResult.newRecommendations();
                updatedRecommendations += regionResult.updatedRecommendations();
                successfulRegions++;

                // 날씨 타입별 통계 업데이트
                updateWeatherStats(weatherStats, regionResult.weatherTypeStats());

                long processingTime = System.currentTimeMillis() - regionStartTime;
                regionResults.add(new WeatherSyncResDTO.RegionRecommendationResult(
                        region.getId(), region.getName(), true,
                        regionResult.recommendationsGenerated(),
                        regionResult.newRecommendations(),
                        regionResult.updatedRecommendations(),
                        regionResult.processedDates(),
                        null, processingTime));

                log.debug("지역 {} 추천 정보 생성 완료: 신규 {}, 업데이트 {}",
                        region.getName(), regionResult.newRecommendations(), regionResult.updatedRecommendations());

            } catch (Exception e) {
                failedRegions++;
                long processingTime = System.currentTimeMillis() - regionStartTime;
                String errorMessage = String.format("지역 %s 추천 생성 실패: %s", region.getName(), e.getMessage());
                errorMessages.add(errorMessage);

                regionResults.add(new WeatherSyncResDTO.RegionRecommendationResult(
                        region.getId(), region.getName(), false, 0, 0, 0,
                        Collections.emptyList(), errorMessage, processingTime));

                log.error("지역 {} 추천 정보 생성 실패", region.getName(), e);
            }
        }

        LocalDateTime endTime = LocalDateTime.now();
        log.info("추천 정보 생성 완료: 성공 {}/{} 지역, 신규 {}, 업데이트 {} 추천",
                successfulRegions, targetRegions.size(), newRecommendations, updatedRecommendations);

        return WeatherSyncConverter.toRecommendationGenerationResult(
                targetRegions.size(), successfulRegions, failedRegions,
                totalRecommendations, newRecommendations, updatedRecommendations,
                startDate, endDate, startTime, endTime, regionResults, weatherStats, errorMessages);
    }

    /**
     * 특정 지역에 대한 추천 정보 생성
     */
    private RegionRecommendationResult generateRecommendationsForRegion(
            Region region, LocalDate startDate, LocalDate endDate, boolean forceRegenerate,
            Map<String, WeatherTemplate> templateMap) {

        int recommendationsGenerated = 0, newRecommendations = 0, updatedRecommendations = 0;
        List<String> processedDates = new ArrayList<>();
        Map<WeatherType, Integer> weatherTypeStats = new HashMap<>();

        // 날짜별로 추천 정보 생성
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            try {
                RecommendationResult result = generateRecommendationForDate(
                        region, currentDate, forceRegenerate, templateMap);

                if (result != null) {
                    recommendationsGenerated++;
                    if (result.isNew()) {
                        newRecommendations++;
                    } else {
                        updatedRecommendations++;
                    }

                    processedDates.add(currentDate.toString());
                    weatherTypeStats.merge(result.weatherType(), 1, Integer::sum);
                }

            } catch (Exception e) {
                log.warn("날짜 {} 추천 생성 실패: {}", currentDate, e.getMessage());
            }

            currentDate = currentDate.plusDays(1);
        }

        return new RegionRecommendationResult(
                recommendationsGenerated, newRecommendations, updatedRecommendations,
                processedDates, weatherTypeStats);
    }

    /**
     * 특정 지역의 특정 날짜에 대한 추천 정보 생성
     */
    private RecommendationResult generateRecommendationForDate(
            Region region, LocalDate date, boolean forceRegenerate,
            Map<String, WeatherTemplate> templateMap) {

        // 1. 기존 추천 정보 확인
        Optional<DailyRecommendation> existingRecommendation =
                dailyRecommendationRepository.findByRegionIdAndDateWithTemplate(region.getId(), date);

        if (existingRecommendation.isPresent() && !forceRegenerate) {
            log.debug("이미 존재하는 추천 정보 스킵: regionId={}, date={}", region.getId(), date);
            return null;
        }

        // 2. 날씨 데이터 분류
        WeatherClassificationService.WeatherClassificationResult classification = classifyWeatherForDate(region, date);

        if (!classification.isValid()) {
            log.warn("유효하지 않은 날씨 분류 결과: regionId={}, date={}", region.getId(), date);
            return null;
        }

        // 3. 템플릿 매칭
        WeatherTemplate matchedTemplate = findMatchingTemplate(classification, templateMap);

        if (matchedTemplate == null) {
            log.warn("매칭되는 템플릿 없음: regionId={}, date={}, classification={}",
                    region.getId(), date, classification.getSummary());
            return null;
        }

        // 4. 추천 정보 저장
        boolean isNew = existingRecommendation.isEmpty();
        DailyRecommendation recommendation = saveOrUpdateRecommendation(
                existingRecommendation.orElse(null), region, date, matchedTemplate);

        log.debug("추천 정보 {}됨: regionId={}, date={}, template={}",
                isNew ? "생성" : "업데이트", region.getId(), date, matchedTemplate.getId());

        return new RecommendationResult(classification.weatherType(), isNew);
    }

    /**
     * 특정 날짜의 날씨 데이터 분류
     * 우선순위: 단기예보(0-2일) > 중기예보(3-6일)
     */
    private WeatherClassificationService.WeatherClassificationResult classifyWeatherForDate(Region region, LocalDate date) {
        LocalDate today = LocalDate.now();
        long daysFromToday = java.time.temporal.ChronoUnit.DAYS.between(today, date);

        if (daysFromToday <= 2) {
            // 단기 예보 데이터 사용 (0-2일)
            List<RawShortTermWeather> shortTermData =
                    shortTermWeatherRepository.findLatestByRegionIdAndFcstDate(region.getId(), date);

            if (!shortTermData.isEmpty()) {
                return classificationService.classifyShortTermWeather(shortTermData, region.getId(), date);
            }
        }

        if (daysFromToday >= 3 && daysFromToday <= 6) {
            // 중기 예보 데이터 사용 (3-6일)
            List<RawMediumTermWeather> mediumTermData =
                    mediumTermWeatherRepository.findLatestByRegionIdAndTmef(region.getId(), date);

            if (!mediumTermData.isEmpty()) {
                return classificationService.classifyMediumTermWeather(mediumTermData, region.getId(), date);
            }
        }

        // 데이터가 없으면 기본 분류 결과 반환
        log.warn("날씨 데이터가 없어서 기본 분류 사용: regionId={}, date={}", region.getId(), date);
        throw new WeatherException(WeatherErrorCode.WEATHER_DATA_NOT_FOUND);
    }

    /**
     * 분류 결과에 매칭되는 템플릿 찾기
     */
    private WeatherTemplate findMatchingTemplate(
            WeatherClassificationService.WeatherClassificationResult classification, Map<String, WeatherTemplate> templateMap) {

        String templateKey = createTemplateKey(
                classification.weatherType(),
                classification.tempCategory(),
                classification.precipCategory());

        WeatherTemplate template = templateMap.get(templateKey);

        if (template == null) {
            log.warn("정확히 매칭되는 템플릿 없음: {}", templateKey);

            // 대체 템플릿 찾기 (강수 카테고리를 낮춰서 시도)
            template = findAlternativeTemplate(classification, templateMap);
        }

        return template;
    }

    /**
     * 대체 템플릿 찾기
     * 강수 카테고리 우선순위: HEAVY > LIGHT > NONE
     */
    private WeatherTemplate findAlternativeTemplate(
            WeatherClassificationService.WeatherClassificationResult classification, Map<String, WeatherTemplate> templateMap) {

        // 강수 카테고리를 단계적으로 낮춰가며 시도
        var precipCategories = List.of(
                classification.precipCategory(),
                com.study.demo.testweatherapi.domain.weather.entity.enums.PrecipCategory.LIGHT,
                com.study.demo.testweatherapi.domain.weather.entity.enums.PrecipCategory.NONE
        );

        for (var precipCategory : precipCategories) {
            String alternativeKey = createTemplateKey(
                    classification.weatherType(),
                    classification.tempCategory(),
                    precipCategory);

            WeatherTemplate template = templateMap.get(alternativeKey);
            if (template != null) {
                log.debug("대체 템플릿 찾음: {} -> {}",
                        classification.precipCategory(), precipCategory);
                return template;
            }
        }

        return null;
    }

    /**
     * 추천 정보 저장 또는 업데이트
     */
    private DailyRecommendation saveOrUpdateRecommendation(
            DailyRecommendation existing, Region region, LocalDate date, WeatherTemplate template) {

        if (existing != null) {
            // 기존 데이터 업데이트 (실제로는 immutable이므로 새로 생성)
            dailyRecommendationRepository.delete(existing);
        }

        DailyRecommendation newRecommendation = DailyRecommendation.builder()
                .region(region)
                .weatherTemplate(template)
                .forecastDate(date)
                .updatedAt(LocalDateTime.now())
                .build();

        return dailyRecommendationRepository.save(newRecommendation);
    }

    // ==== 유틸리티 메서드들 ====

    /**
     * 대상 지역 조회
     */
    private List<Region> getTargetRegions(List<Long> regionIds) {
        if (regionIds == null || regionIds.isEmpty()) {
            return regionRepository.findAllActiveRegions();
        } else {
            return regionRepository.findAllById(regionIds);
        }
    }

    /**
     * 템플릿 맵 생성 (빠른 조회를 위한 인덱스)
     */
    private Map<String, WeatherTemplate> createTemplateMap(List<WeatherTemplate> templates) {
        return templates.stream()
                .collect(Collectors.toMap(
                        template -> createTemplateKey(
                                template.getWeather(),
                                template.getTempCategory(),
                                template.getPrecipCategory()),
                        template -> template,
                        (existing, replacement) -> existing  // 중복 키 시 기존 값 유지
                ));
    }

    /**
     * 템플릿 키 생성
     */
    private String createTemplateKey(
            com.study.demo.testweatherapi.domain.weather.entity.enums.WeatherType weather,
            com.study.demo.testweatherapi.domain.weather.entity.enums.TempCategory tempCategory,
            com.study.demo.testweatherapi.domain.weather.entity.enums.PrecipCategory precipCategory) {
        return String.format("%s_%s_%s", weather, tempCategory, precipCategory);
    }

    /**
     * 날씨 타입별 통계 업데이트
     */
    private void updateWeatherStats(Map<WeatherType, Integer> globalStats,
                                    Map<WeatherType, Integer> regionStats) {
        for (Map.Entry<WeatherType, Integer> entry : regionStats.entrySet()) {
            globalStats.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    // ==== 내부 결과 클래스들 ====

    /**
     * 지역별 추천 생성 결과
     */
    private record RegionRecommendationResult(
            int recommendationsGenerated,
            int newRecommendations,
            int updatedRecommendations,
            List<String> processedDates,
            Map<WeatherType, Integer> weatherTypeStats
    ) {}

    /**
     * 개별 추천 생성 결과
     */
    private record RecommendationResult(
            WeatherType weatherType,
            boolean isNew
    ) {}
}
