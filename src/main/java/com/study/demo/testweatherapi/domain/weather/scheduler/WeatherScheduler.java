package com.study.demo.testweatherapi.domain.weather.scheduler;

import com.study.demo.testweatherapi.domain.weather.dto.response.WeatherSyncResDTO;
import com.study.demo.testweatherapi.domain.weather.service.WeatherDataCleanupService;
import com.study.demo.testweatherapi.domain.weather.service.WeatherDataCollectionService;
import com.study.demo.testweatherapi.domain.weather.service.WeatherRecommendationGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.weather.enabled", havingValue = "true", matchIfMissing = true)
public class WeatherScheduler {

    private final WeatherDataCollectionService dataCollectionService;
    private final WeatherRecommendationGenerationService recommendationGenerationService;
    private final WeatherDataCleanupService dataCleanupService;

    // 스케줄러 실행 상태 추적
    private volatile boolean shortTermSyncRunning = false;
    private volatile boolean mediumTermSyncRunning = false;
    private volatile boolean shortTermRecommendationRunning = false;
    private volatile boolean mediumTermRecommendationRunning = false;
    private volatile boolean cleanupRunning = false;

    /**
     * 단기 예보 데이터 수집 스케줄러
     * 매 3시간마다 실행 (02:10, 05:10, 08:10, 11:10, 14:10, 17:10, 20:10, 23:10)
     * 기상청 발표 시각보다 10분 후에 실행하여 데이터 준비 시간 확보
     */
    @Scheduled(cron = "${scheduler.weather.short-term-cron}")
    @Async("weatherTaskExecutor")
    public void scheduledShortTermWeatherSync() {
        if (shortTermSyncRunning) {
            log.warn("단기 예보 동기화가 이미 실행 중입니다. 스킵합니다.");
            return;
        }

        try {
            shortTermSyncRunning = true;
            log.info("단기 예보 동기화 스케줄러 시작");

            // 현재 시각 기준으로 기준시각 계산
            LocalDateTime now = LocalDateTime.now();
            LocalDate baseDate = now.toLocalDate();
            String baseTime = calculateNearestBaseTime(now.getHour());

            // 모든 지역에 대해 동기화 실행
            WeatherSyncResDTO.ShortTermSyncResult result = dataCollectionService.collectShortTermWeatherData(
                    null, baseDate, baseTime, false);

            log.info("단기 예보 동기화 스케줄러 완료: 성공 {}/{} 지역, 신규 {} 건, 업데이트 {} 건",
                    result.successfulRegions(), result.totalRegions(),
                    result.newDataPoints(), result.updatedDataPoints());

            // 동기화 성공 후 단기예보 추천 정보 생성 트리거 (15분 후)
            if (result.successfulRegions() > 0) {
                triggerShortTermRecommendationGeneration("단기예보 동기화 후", 15);
            }

        } catch (Exception e) {
            log.error("단기 예보 동기화 스케줄러 실행 중 오류 발생", e);
        } finally {
            shortTermSyncRunning = false;
        }
    }

    /**
     * 중기 예보 데이터 수집 스케줄러
     * 매 12시간마다 실행 (06:30, 18:30)
     */
    @Scheduled(cron = "${scheduler.weather.medium-term-cron}")
    @Async("weatherTaskExecutor")
    public void scheduledMediumTermWeatherSync() {
        if (mediumTermSyncRunning) {
            log.warn("중기 예보 동기화가 이미 실행 중입니다. 스킵합니다.");
            return;
        }

        try {
            mediumTermSyncRunning = true;
            log.info("중기 예보 동기화 스케줄러 시작");

            LocalDate tmfc = LocalDate.now();

            // 모든 지역에 대해 동기화 실행
            WeatherSyncResDTO.MediumTermSyncResult result = dataCollectionService.collectMediumTermWeatherData(
                    null, tmfc, false);

            log.info("중기 예보 동기화 스케줄러 완료: 성공 {}/{} 지역, 신규 {} 건, 업데이트 {} 건",
                    result.successfulRegions(), result.totalRegions(),
                    result.newDataPoints(), result.updatedDataPoints());

            // 동기화 성공 후 중기예보 추천 정보 생성 트리거 (30분 후)
            if (result.successfulRegions() > 0) {
                triggerMediumTermRecommendationGeneration("중기예보 동기화 후", 30);
            }

        } catch (Exception e) {
            log.error("중기 예보 동기화 스케줄러 실행 중 오류 발생", e);
        } finally {
            mediumTermSyncRunning = false;
        }
    }

    /**
     * 단기예보 추천 정보 생성 스케줄러 (0-2일)
     * 매 시간 5분에 실행 - 단기예보는 1시간마다 업데이트
     */
    @Scheduled(cron = "${scheduler.weather.recommendation.short-term-cron}")
    @Async("weatherTaskExecutor")
    public void scheduledShortTermRecommendationGeneration() {
        if (shortTermRecommendationRunning) {
            log.warn("단기예보 추천 생성이 이미 실행 중입니다. 스킵합니다.");
            return;
        }

        try {
            shortTermRecommendationRunning = true;
            log.info("단기예보 추천 생성 스케줄러 시작 (0-2일)");

            // 오늘부터 3일간만 처리 (0, 1, 2일)
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusDays(2);

            WeatherSyncResDTO.RecommendationGenerationResult result =
                    recommendationGenerationService.generateRecommendations(
                            null, startDate, endDate, true, "단기예보");  // 강제 재생성으로 최신 데이터 반영

            log.info("단기예보 추천 생성 스케줄러 완료: 성공 {}/{} 지역, 신규 {} 건, 업데이트 {} 건",
                    result.successfulRegions(), result.totalRegions(),
                    result.newRecommendations(), result.updatedRecommendations());

        } catch (Exception e) {
            log.error("단기예보 추천 생성 스케줄러 실행 중 오류 발생", e);
        } finally {
            shortTermRecommendationRunning = false;
        }
    }

    /**
     * 중기예보 추천 정보 생성 스케줄러 (3-6일)
     * 매 6시간 30분에 실행 - 중기예보는 12시간마다 업데이트되므로 6시간마다 충분
     */
    @Scheduled(cron = "${scheduler.weather.recommendation.medium-term-cron}")
    @Async("weatherTaskExecutor")
    public void scheduledMediumTermRecommendationGeneration() {
        if (mediumTermRecommendationRunning) {
            log.warn("중기예보 추천 생성이 이미 실행 중입니다. 스킵합니다.");
            return;
        }

        try {
            mediumTermRecommendationRunning = true;
            log.info("중기예보 추천 생성 스케줄러 시작 (3-6일)");

            // 3일후부터 4일간만 처리 (3, 4, 5, 6일)
            LocalDate startDate = LocalDate.now().plusDays(3);
            LocalDate endDate = LocalDate.now().plusDays(6);

            WeatherSyncResDTO.RecommendationGenerationResult result =
                    recommendationGenerationService.generateRecommendations(
                            null, startDate, endDate, true, "중기예보");  // 강제 재생성

            log.info("중기예보 추천 생성 스케줄러 완료: 성공 {}/{} 지역, 신규 {} 건, 업데이트 {} 건",
                    result.successfulRegions(), result.totalRegions(),
                    result.newRecommendations(), result.updatedRecommendations());

        } catch (Exception e) {
            log.error("중기예보 추천 생성 스케줄러 실행 중 오류 발생", e);
        } finally {
            mediumTermRecommendationRunning = false;
        }
    }

    /**
     * 데이터 정리 스케줄러
     * 매일 새벽 3시에 실행
     */
    @Scheduled(cron = "${scheduler.weather.cleanup-cron}")
    @Async("weatherTaskExecutor")
    public void scheduledDataCleanup() {
        if (cleanupRunning) {
            log.warn("데이터 정리가 이미 실행 중입니다. 스킵합니다.");
            return;
        }

        try {
            cleanupRunning = true;
            log.info("데이터 정리 스케줄러 시작");

            // 7일 이전 데이터 정리
            int retentionDays = 7;
            WeatherSyncResDTO.CleanupResult result = dataCleanupService.cleanupOldWeatherData(
                    retentionDays, true, true, true, false);

            log.info("데이터 정리 스케줄러 완료: 보관기간 {}일, 처리시간 {}ms",
                    retentionDays, result.processingDurationMs());

            if (result.shortTermStats() != null) {
                log.info("단기예보 정리: {} 건 삭제", result.shortTermStats().recordsDeleted());
            }
            if (result.mediumTermStats() != null) {
                log.info("중기예보 정리: {} 건 삭제", result.mediumTermStats().recordsDeleted());
            }
            if (result.recommendationStats() != null) {
                log.info("추천정보 정리: {} 건 삭제", result.recommendationStats().recordsDeleted());
            }

        } catch (Exception e) {
            log.error("데이터 정리 스케줄러 실행 중 오류 발생", e);
        } finally {
            cleanupRunning = false;
        }
    }

    /**
     * 시스템 상태 모니터링 스케줄러
     * 매 30분마다 실행하여 시스템 상태 점검
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30분마다
    public void scheduledSystemHealthCheck() {
        log.debug("시스템 상태 점검 시작");

        try {
            // 각 스케줄러 실행 상태 로깅
            log.debug("스케줄러 실행 상태 - 단기예보수집: {}, 중기예보수집: {}, 단기추천: {}, 중기추천: {}, 정리작업: {}",
                    shortTermSyncRunning, mediumTermSyncRunning,
                    shortTermRecommendationRunning, mediumTermRecommendationRunning, cleanupRunning);

            // 장시간 실행 중인 작업 경고
            if (shortTermSyncRunning) {
                log.warn("단기 예보 동기화가 장시간 실행 중입니다. 확인이 필요합니다.");
            }
            if (mediumTermSyncRunning) {
                log.warn("중기 예보 동기화가 장시간 실행 중입니다. 확인이 필요합니다.");
            }
            if (shortTermRecommendationRunning) {
                log.warn("단기예보 추천 생성이 장시간 실행 중입니다. 확인이 필요합니다.");
            }
            if (mediumTermRecommendationRunning) {
                log.warn("중기예보 추천 생성이 장시간 실행 중입니다. 확인이 필요합니다.");
            }

            // 메모리 사용량 체크
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;

            if (memoryUsagePercent > 80) {
                log.warn("메모리 사용량이 높습니다: {:.1f}%", memoryUsagePercent);
            }

        } catch (Exception e) {
            log.error("시스템 상태 점검 중 오류 발생", e);
        }
    }

    /**
     * 애플리케이션 시작 시 초기 데이터 동기화
     * 서버 재시작 후 최신 데이터 확보
     */
    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE) // 1분 후 1회 실행
    @Async("weatherTaskExecutor")
    public void initialDataSync() {
        log.info("애플리케이션 시작 후 초기 데이터 동기화 시작");

        try {
            // 현재 시각 기준 단기 예보 동기화
            LocalDateTime now = LocalDateTime.now();
            LocalDate baseDate = now.toLocalDate();
            String baseTime = calculateNearestBaseTime(now.getHour());

            CompletableFuture<Void> shortTermFuture = CompletableFuture.runAsync(() -> {
                try {
                    dataCollectionService.collectShortTermWeatherData(null, baseDate, baseTime, false);
                    log.info("초기 단기 예보 동기화 완료");
                } catch (Exception e) {
                    log.error("초기 단기 예보 동기화 실패", e);
                }
            });

            // 중기 예보 동기화
            CompletableFuture<Void> mediumTermFuture = CompletableFuture.runAsync(() -> {
                try {
                    dataCollectionService.collectMediumTermWeatherData(null, LocalDate.now(), false);
                    log.info("초기 중기 예보 동기화 완료");
                } catch (Exception e) {
                    log.error("초기 중기 예보 동기화 실패", e);
                }
            });

            // 두 동기화 작업 완료 후 추천 정보 생성
            CompletableFuture.allOf(shortTermFuture, mediumTermFuture).thenRun(() -> {
                try {
                    // 전체 기간 추천 정보 생성
                    LocalDate startDate = LocalDate.now();
                    LocalDate endDate = startDate.plusDays(6);
                    recommendationGenerationService.generateRecommendations(
                            null, startDate, endDate, false, "초기동기화");
                    log.info("초기 추천 정보 생성 완료");
                } catch (Exception e) {
                    log.error("초기 추천 정보 생성 실패", e);
                }
            });

            log.info("초기 데이터 동기화 작업이 시작되었습니다.");

        } catch (Exception e) {
            log.error("초기 데이터 동기화 중 오류 발생", e);
        }
    }

    // ==== 내부 유틸리티 메서드들 ====

    /**
     * 단기예보 추천 정보 생성 트리거 (비동기, 지연 실행)
     */
    private void triggerShortTermRecommendationGeneration(String trigger, int delayMinutes) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delayMinutes * 60 * 1000); // 지연 시간
                log.debug("단기예보 추천 정보 생성 트리거: {}", trigger);

                LocalDate startDate = LocalDate.now();
                LocalDate endDate = startDate.plusDays(2);

                recommendationGenerationService.generateRecommendations(
                        null, startDate, endDate, true, "트리거-" + trigger);
                log.debug("단기예보 추천 정보 생성 트리거 완료: {}", trigger);

            } catch (Exception e) {
                log.error("단기예보 추천 정보 생성 트리거 실패: {}", trigger, e);
            }
        });
    }

    /**
     * 중기예보 추천 정보 생성 트리거 (비동기, 지연 실행)
     */
    private void triggerMediumTermRecommendationGeneration(String trigger, int delayMinutes) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delayMinutes * 60 * 1000); // 지연 시간
                log.debug("중기예보 추천 정보 생성 트리거: {}", trigger);

                LocalDate startDate = LocalDate.now().plusDays(3);
                LocalDate endDate = LocalDate.now().plusDays(6);

                recommendationGenerationService.generateRecommendations(
                        null, startDate, endDate, true, "트리거-" + trigger);
                log.debug("중기예보 추천 정보 생성 트리거 완료: {}", trigger);

            } catch (Exception e) {
                log.error("중기예보 추천 정보 생성 트리거 실패: {}", trigger, e);
            }
        });
    }

    /**
     * 현재 시각을 기준으로 가장 가까운 기준시각 계산
     */
    private String calculateNearestBaseTime(int currentHour) {
        int[] baseTimes = {2, 5, 8, 11, 14, 17, 20, 23};

        // 현재 시각보다 이전 또는 같은 가장 가까운 기준시각 찾기
        for (int i = baseTimes.length - 1; i >= 0; i--) {
            if (currentHour >= baseTimes[i]) {
                return String.format("%02d00", baseTimes[i]);
            }
        }

        // 현재 시각이 새벽 2시 이전이면 전날의 23시
        return "2300";
    }

    /**
     * 스케줄러 실행 상태 조회 (관리자용)
     */
    public SchedulerStatus getSchedulerStatus() {
        return new SchedulerStatus(
                shortTermSyncRunning,
                mediumTermSyncRunning,
                shortTermRecommendationRunning,
                mediumTermRecommendationRunning,
                cleanupRunning,
                LocalDateTime.now()
        );
    }

    /**
     * 스케줄러 상태 정보
     */
    public record SchedulerStatus(
            boolean shortTermSyncRunning,
            boolean mediumTermSyncRunning,
            boolean shortTermRecommendationRunning,
            boolean mediumTermRecommendationRunning,
            boolean cleanupRunning,
            LocalDateTime statusTime
    ) {}
}
