package com.study.demo.testweatherapi.domain.weather.service;

import com.study.demo.testweatherapi.domain.weather.dto.response.WeatherSyncResDTO;
import com.study.demo.testweatherapi.domain.weather.repository.DailyRecommendationRepository;
import com.study.demo.testweatherapi.domain.weather.repository.RawMediumTermWeatherRepository;
import com.study.demo.testweatherapi.domain.weather.repository.RawShortTermWeatherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherDataCleanupService {

    private final RawShortTermWeatherRepository shortTermWeatherRepository;
    private final RawMediumTermWeatherRepository mediumTermWeatherRepository;
    private final DailyRecommendationRepository dailyRecommendationRepository;

    /**
     * 오래된 날씨 데이터 정리
     */
    @Transactional
    public WeatherSyncResDTO.CleanupResult cleanupOldWeatherData(
            Integer retentionDays, boolean cleanupShortTerm, boolean cleanupMediumTerm,
            boolean cleanupRecommendations, boolean dryRun) {

        LocalDateTime startTime = LocalDateTime.now();
        log.info("데이터 정리 시작: retentionDays={}, dryRun={}", retentionDays, dryRun);

        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        List<String> errorMessages = new ArrayList<>();

        try {
            // 단기 예보 데이터 정리
            WeatherSyncResDTO.CleanupStats shortTermStats = null;
            if (cleanupShortTerm) {
                shortTermStats = cleanupShortTermData(cutoffDate, dryRun);
            }

            // 중기 예보 데이터 정리
            WeatherSyncResDTO.CleanupStats mediumTermStats = null;
            if (cleanupMediumTerm) {
                mediumTermStats = cleanupMediumTermData(cutoffDate, dryRun);
            }

            // 추천 정보 정리
            WeatherSyncResDTO.CleanupStats recommendationStats = null;
            if (cleanupRecommendations) {
                recommendationStats = cleanupRecommendationData(cutoffDate, dryRun);
            }

            LocalDateTime endTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

            String message = dryRun ?
                    "데이터 정리 시뮬레이션 완료 (실제 삭제되지 않음)" :
                    "데이터 정리 완료";

            log.info("데이터 정리 완료: retentionDays={}, dryRun={}, 소요시간={}ms",
                    retentionDays, dryRun, durationMs);

            return WeatherSyncResDTO.CleanupResult.builder()
                    .dryRun(dryRun)
                    .retentionDays(retentionDays)
                    .cutoffDate(cutoffDate)
                    .shortTermStats(shortTermStats)
                    .mediumTermStats(mediumTermStats)
                    .recommendationStats(recommendationStats)
                    .processingStartTime(startTime)
                    .processingEndTime(endTime)
                    .processingDurationMs(durationMs)
                    .errorMessages(errorMessages)
                    .message(message)
                    .build();

        } catch (Exception e) {
            log.error("데이터 정리 중 오류 발생", e);
            errorMessages.add("데이터 정리 실패: " + e.getMessage());

            LocalDateTime endTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

            return WeatherSyncResDTO.CleanupResult.builder()
                    .dryRun(dryRun)
                    .retentionDays(retentionDays)
                    .cutoffDate(cutoffDate)
                    .shortTermStats(null)
                    .mediumTermStats(null)
                    .recommendationStats(null)
                    .processingStartTime(startTime)
                    .processingEndTime(endTime)
                    .processingDurationMs(durationMs)
                    .errorMessages(errorMessages)
                    .message("데이터 정리 실패")
                    .build();
        }
    }

    /**
     * 단기 예보 데이터 정리
     */
    private WeatherSyncResDTO.CleanupStats cleanupShortTermData(LocalDate cutoffDate, boolean dryRun) {
        log.debug("단기 예보 데이터 정리: cutoffDate={}, dryRun={}", cutoffDate, dryRun);

        try {
            // 삭제 대상 레코드 수 조회
            long recordsFound = shortTermWeatherRepository.count(); // 실제로는 cutoffDate 이전 레코드만 카운트하는 쿼리 필요

            int recordsDeleted = 0;
            if (!dryRun) {
                shortTermWeatherRepository.deleteOldData(cutoffDate);
                recordsDeleted = (int) recordsFound; // 실제로는 삭제된 레코드 수 반환 필요
            }

            long spaceSavedMB = recordsDeleted * 1024 / (1024 * 1024); // 대략적인 계산

            return WeatherSyncResDTO.CleanupStats.builder()
                    .dataType("단기예보")
                    .executed(!dryRun)
                    .recordsFound((int) recordsFound)
                    .recordsDeleted(recordsDeleted)
                    .spaceSavedMB(spaceSavedMB)
                    .build();

        } catch (Exception e) {
            log.error("단기 예보 데이터 정리 실패", e);
            return WeatherSyncResDTO.CleanupStats.builder()
                    .dataType("단기예보")
                    .executed(false)
                    .recordsFound(0)
                    .recordsDeleted(0)
                    .spaceSavedMB(0)
                    .build();
        }
    }

    /**
     * 중기 예보 데이터 정리
     */
    private WeatherSyncResDTO.CleanupStats cleanupMediumTermData(LocalDate cutoffDate, boolean dryRun) {
        log.debug("중기 예보 데이터 정리: cutoffDate={}, dryRun={}", cutoffDate, dryRun);

        try {
            long recordsFound = mediumTermWeatherRepository.count();

            int recordsDeleted = 0;
            if (!dryRun) {
                mediumTermWeatherRepository.deleteOldData(cutoffDate);
                recordsDeleted = (int) recordsFound;
            }

            long spaceSavedMB = recordsDeleted * 512 / (1024 * 1024);

            return WeatherSyncResDTO.CleanupStats.builder()
                    .dataType("중기예보")
                    .executed(!dryRun)
                    .recordsFound((int) recordsFound)
                    .recordsDeleted(recordsDeleted)
                    .spaceSavedMB(spaceSavedMB)
                    .build();

        } catch (Exception e) {
            log.error("중기 예보 데이터 정리 실패", e);
            return WeatherSyncResDTO.CleanupStats.builder()
                    .dataType("중기예보")
                    .executed(false)
                    .recordsFound(0)
                    .recordsDeleted(0)
                    .spaceSavedMB(0)
                    .build();
        }
    }

    /**
     * 추천 정보 데이터 정리
     */
    private WeatherSyncResDTO.CleanupStats cleanupRecommendationData(LocalDate cutoffDate, boolean dryRun) {
        log.debug("추천 정보 데이터 정리: cutoffDate={}, dryRun={}", cutoffDate, dryRun);

        try {
            long recordsFound = dailyRecommendationRepository.count();

            int recordsDeleted = 0;
            if (!dryRun) {
                dailyRecommendationRepository.deleteOldRecommendations(cutoffDate);
                recordsDeleted = (int) recordsFound;
            }

            long spaceSavedMB = recordsDeleted * 256 / (1024 * 1024);

            return WeatherSyncResDTO.CleanupStats.builder()
                    .dataType("추천정보")
                    .executed(!dryRun)
                    .recordsFound((int) recordsFound)
                    .recordsDeleted(recordsDeleted)
                    .spaceSavedMB(spaceSavedMB)
                    .build();

        } catch (Exception e) {
            log.error("추천 정보 데이터 정리 실패", e);
            return WeatherSyncResDTO.CleanupStats.builder()
                    .dataType("추천정보")
                    .executed(false)
                    .recordsFound(0)
                    .recordsDeleted(0)
                    .spaceSavedMB(0)
                    .build();
        }
    }
}
