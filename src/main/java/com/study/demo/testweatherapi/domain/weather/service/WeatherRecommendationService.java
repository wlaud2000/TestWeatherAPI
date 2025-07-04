package com.study.demo.testweatherapi.domain.weather.service;

import com.study.demo.testweatherapi.domain.weather.converter.WeatherConverter;
import com.study.demo.testweatherapi.domain.weather.dto.request.WeatherReqDTO;
import com.study.demo.testweatherapi.domain.weather.dto.response.WeatherResDTO;
import com.study.demo.testweatherapi.domain.weather.entity.DailyRecommendation;
import com.study.demo.testweatherapi.domain.weather.entity.Region;
import com.study.demo.testweatherapi.domain.weather.exception.WeatherErrorCode;
import com.study.demo.testweatherapi.domain.weather.exception.WeatherException;
import com.study.demo.testweatherapi.domain.weather.repository.DailyRecommendationRepository;
import com.study.demo.testweatherapi.domain.weather.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeatherRecommendationService {

    private final DailyRecommendationRepository dailyRecommendationRepository;
    private final RegionRepository regionRepository;

    /**
     * 특정 지역, 특정 날짜의 날씨 추천 정보 조회
     */
    public WeatherResDTO.WeatherRecommendation getRecommendation(WeatherReqDTO.GetRecommendation request) {
        log.info("날씨 추천 조회 요청: regionId={}, date={}", request.regionId(), request.date());

        // 1. 지역 존재 확인
        Region region = validateRegionExists(request.regionId());

        // 2. 추천 정보 조회
        Optional<DailyRecommendation> recommendationOpt =
                dailyRecommendationRepository.findByRegionIdAndDateWithTemplate(
                        request.regionId(), request.date());

        if (recommendationOpt.isEmpty()) {
            log.warn("추천 정보 없음: regionId={}, date={}", request.regionId(), request.date());
            throw new WeatherException(WeatherErrorCode.DAILY_RECOMMENDATION_NOT_FOUND);
        }

        DailyRecommendation recommendation = recommendationOpt.get();
        log.info("날씨 추천 조회 완료: recommendationId={}", recommendation.getId());

        return WeatherConverter.toWeatherRecommendation(recommendation);
    }

    /**
     * 특정 지역의 주간 날씨 추천 정보 조회 (7일치)
     */
    public WeatherResDTO.WeeklyRecommendation getWeeklyRecommendation(
            WeatherReqDTO.GetWeeklyRecommendation request) {
        log.info("주간 날씨 추천 조회 요청: regionId={}, startDate={}",
                request.regionId(), request.startDate());

        // 1. 지역 존재 확인
        Region region = validateRegionExists(request.regionId());

        // 2. 주간 추천 정보 조회
        LocalDate endDate = request.getEndDate();
        List<DailyRecommendation> recommendations =
                dailyRecommendationRepository.findWeeklyRecommendations(
                        request.regionId(), request.startDate(), endDate.plusDays(1));

        log.info("주간 날씨 추천 조회 완료: regionId={}, 조회된 데이터 수={}",
                request.regionId(), recommendations.size());

        return WeatherConverter.toWeeklyRecommendation(
                recommendations, region.getId(), region.getName(),
                request.startDate(), endDate);
    }

    /**
     * 날짜 범위로 날씨 추천 정보 조회
     */
    public WeatherResDTO.WeeklyRecommendation getRecommendationByDateRange(
            WeatherReqDTO.GetRecommendationByDateRange request) {
        log.info("날짜 범위 날씨 추천 조회 요청: regionId={}, startDate={}, endDate={}",
                request.regionId(), request.startDate(), request.endDate());

        // 1. 지역 존재 확인
        Region region = validateRegionExists(request.regionId());

        // 2. 날짜 범위 추천 정보 조회
        List<DailyRecommendation> recommendations =
                dailyRecommendationRepository.findByRegionIdAndDateRange(
                        request.regionId(), request.startDate(), request.endDate());

        log.info("날짜 범위 날씨 추천 조회 완료: regionId={}, 조회된 데이터 수={}",
                request.regionId(), recommendations.size());

        return WeatherConverter.toWeeklyRecommendation(
                recommendations, region.getId(), region.getName(),
                request.startDate(), request.endDate());
    }

    /**
     * 특정 지역의 가장 최근 날씨 추천 정보 조회
     */
    public List<WeatherResDTO.WeatherRecommendationSummary> getLatestRecommendations(Long regionId) {
        log.info("최근 날씨 추천 조회 요청: regionId={}", regionId);

        // 1. 지역 존재 확인
        validateRegionExists(regionId);

        // 2. 최근 추천 정보 조회 (최대 7개)
        List<DailyRecommendation> recommendations =
                dailyRecommendationRepository.findLatestByRegionId(regionId);

        List<WeatherResDTO.WeatherRecommendationSummary> summaries = recommendations.stream()
                .limit(7)  // 최근 7일간만
                .map(WeatherConverter::toWeatherRecommendationSummary)
                .toList();

        log.info("최근 날씨 추천 조회 완료: regionId={}, 조회된 데이터 수={}", regionId, summaries.size());
        return summaries;
    }

    /**
     * 특정 날짜의 모든 지역 날씨 추천 정보 조회 (관리자용)
     */
    public List<WeatherResDTO.WeatherRecommendationSummary> getAllRecommendationsByDate(LocalDate date) {
        log.info("특정 날짜 전체 지역 날씨 추천 조회: date={}", date);

        List<DailyRecommendation> recommendations =
                dailyRecommendationRepository.findAllByDate(date);

        List<WeatherResDTO.WeatherRecommendationSummary> summaries = recommendations.stream()
                .map(WeatherConverter::toWeatherRecommendationSummary)
                .toList();

        log.info("특정 날짜 전체 지역 날씨 추천 조회 완료: date={}, 조회된 데이터 수={}",
                date, summaries.size());
        return summaries;
    }

    /**
     * 추천 정보 조회 시 대안 제안
     * 요청한 날짜에 데이터가 없을 때 가장 가까운 날짜의 데이터 제안
     */
    public WeatherResDTO.WeatherRecommendationNotFound getRecommendationAlternatives(
            Long regionId, LocalDate requestedDate) {
        log.info("대안 추천 조회: regionId={}, requestedDate={}", regionId, requestedDate);

        // 1. 지역 존재 확인
        Region region = validateRegionExists(regionId);

        // 2. 가장 가까운 날짜의 데이터 찾기
        List<String> suggestions = findNearestRecommendations(regionId, requestedDate);

        return WeatherConverter.toRecommendationNotFound(
                regionId, region.getName(), requestedDate, suggestions);
    }

    /**
     * 특정 지역의 추천 데이터 존재 여부 확인
     */
    public boolean hasRecommendationData(Long regionId, LocalDate date) {
        return dailyRecommendationRepository.existsByRegionIdAndForecastDate(regionId, date);
    }

    /**
     * 지역 존재 여부 검증
     */
    private Region validateRegionExists(Long regionId) {
        return regionRepository.findById(regionId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 지역: regionId={}", regionId);
                    return new WeatherException(WeatherErrorCode.REGION_NOT_FOUND);
                });
    }

    /**
     * 가장 가까운 날짜의 추천 정보 찾기
     */
    private List<String> findNearestRecommendations(Long regionId, LocalDate requestedDate) {
        List<String> suggestions = List.of();

        try {
            // 전후 3일씩 확인
            LocalDate startDate = requestedDate.minusDays(3);
            LocalDate endDate = requestedDate.plusDays(3);

            List<DailyRecommendation> nearbyRecommendations =
                    dailyRecommendationRepository.findByRegionIdAndDateRange(regionId, startDate, endDate);

            if (!nearbyRecommendations.isEmpty()) {
                // 가장 가까운 날짜 찾기
                DailyRecommendation nearest = nearbyRecommendations.stream()
                        .min((r1, r2) -> {
                            long diff1 = Math.abs(r1.getForecastDate().toEpochDay() - requestedDate.toEpochDay());
                            long diff2 = Math.abs(r2.getForecastDate().toEpochDay() - requestedDate.toEpochDay());
                            return Long.compare(diff1, diff2);
                        })
                        .orElse(null);

                if (nearest != null) {
                    suggestions = List.of(
                            String.format("가장 가까운 날짜: %s", nearest.getForecastDate()),
                            "주간 날씨 추천을 이용해보세요.",
                            "데이터 업데이트는 매 3시간마다 진행됩니다."
                    );
                }
            } else {
                suggestions = List.of(
                        "해당 지역의 날씨 데이터가 아직 수집되지 않았습니다.",
                        "잠시 후 다시 시도해주세요.",
                        "다른 지역의 날씨를 확인해보세요."
                );
            }

        } catch (Exception e) {
            log.error("대안 추천 검색 중 오류 발생", e);
            suggestions = List.of("데이터 조회 중 오류가 발생했습니다.");
        }

        return suggestions;
    }
}
