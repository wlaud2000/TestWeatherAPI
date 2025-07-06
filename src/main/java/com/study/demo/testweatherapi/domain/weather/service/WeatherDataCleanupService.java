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
        log.info("데이터 정리 시작: retentionDays={}, dryRun={}, cleanupShortTerm={}, cleanupMediumTerm={}, cleanupRecommendations={}",
                retentionDays, dryRun, cleanupShortTerm, cleanupMediumTerm, cleanupRecommendations);

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
                    .message("데이터 정리 성공")
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
            // 삭제 대상 레코드 수 정확히 조회
            long recordsFound = shortTermWeatherRepository.countOldData(cutoffDate);

            // 상세 통계 정보 조회 (로깅용)
            Object[] statistics = shortTermWeatherRepository.getOldDataStatistics(cutoffDate);
            if (statistics != null && statistics.length == 3 && statistics[2] != null) {
                LocalDate oldestDate = (LocalDate) statistics[0];
                LocalDate newestDate = (LocalDate) statistics[1];
                Long count = (Long) statistics[2];
                log.debug("단기예보 삭제 대상 통계: 최오래된날짜={}, 최신날짜={}, 총개수={}",
                        oldestDate, newestDate, count);
            }

            int recordsDeleted = 0;
            if (!dryRun && recordsFound > 0) {
                // 실제 삭제 실행 및 삭제된 레코드 수 반환
                recordsDeleted = shortTermWeatherRepository.deleteOldData(cutoffDate);
                log.info("단기예보 데이터 삭제 완료: 예상 {}, 실제 삭제 {}", recordsFound, recordsDeleted);
            } else if (dryRun) {
                log.info("단기예보 데이터 정리 시뮬레이션: {} 건이 삭제 대상입니다", recordsFound);
            }

            // 공간 절약량 계산 (단기예보: 평균 1KB per record)
            long spaceSavedMB = recordsDeleted * 1024 / (1024 * 1024);

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
            // 삭제 대상 레코드 수 정확히 조회
            long recordsFound = mediumTermWeatherRepository.countOldData(cutoffDate);

            // 상세 통계 정보 조회 (로깅용)
            Object[] statistics = mediumTermWeatherRepository.getOldDataStatistics(cutoffDate);
            if (statistics != null && statistics.length == 3 && statistics[2] != null) {
                LocalDate oldestDate = (LocalDate) statistics[0];
                LocalDate newestDate = (LocalDate) statistics[1];
                Long count = (Long) statistics[2];
                log.debug("중기예보 삭제 대상 통계: 최오래된날짜={}, 최신날짜={}, 총개수={}",
                        oldestDate, newestDate, count);
            }

            int recordsDeleted = 0;
            if (!dryRun && recordsFound > 0) {
                // 실제 삭제 실행 및 삭제된 레코드 수 반환
                recordsDeleted = mediumTermWeatherRepository.deleteOldData(cutoffDate);
                log.info("중기예보 데이터 삭제 완료: 예상 {}, 실제 삭제 {}", recordsFound, recordsDeleted);
            } else if (dryRun) {
                log.info("중기예보 데이터 정리 시뮬레이션: {} 건이 삭제 대상입니다", recordsFound);
            }

            // 공간 절약량 계산 (중기예보: 평균 512B per record)
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
            // 삭제 대상 레코드 수 정확히 조회
            long recordsFound = dailyRecommendationRepository.countOldRecommendations(cutoffDate);

            // 상세 통계 정보 조회 (로깅용)
            Object[] statistics = dailyRecommendationRepository.getOldRecommendationStatistics(cutoffDate);
            if (statistics != null && statistics.length == 3 && statistics[2] != null) {
                LocalDate oldestDate = (LocalDate) statistics[0];
                LocalDate newestDate = (LocalDate) statistics[1];
                Long count = (Long) statistics[2];
                log.debug("추천정보 삭제 대상 통계: 최오래된날짜={}, 최신날짜={}, 총개수={}",
                        oldestDate, newestDate, count);
            }

            int recordsDeleted = 0;
            if (!dryRun && recordsFound > 0) {
                // 실제 삭제 실행 및 삭제된 레코드 수 반환
                recordsDeleted = dailyRecommendationRepository.deleteOldRecommendations(cutoffDate);
                log.info("추천정보 데이터 삭제 완료: 예상 {}, 실제 삭제 {}", recordsFound, recordsDeleted);
            } else if (dryRun) {
                log.info("추천정보 데이터 정리 시뮬레이션: {} 건이 삭제 대상입니다", recordsFound);
            }

            // 공간 절약량 계산 (추천정보: 평균 256B per record)
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
