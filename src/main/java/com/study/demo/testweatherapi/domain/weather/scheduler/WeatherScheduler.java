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
    private volatile boolean completeRecommendationRunning = false;
    private volatile boolean cleanupRunning = false;

    /**
     * 단기 예보 데이터 수집 스케줄러
     * 매 3시간마다 실행 (02:10, 05:10, 08:10, 11:10, 14:10, 17:10, 20:10, 23:10)
     * 기상청 발표 시각보다 10분 후에 실행하여 데이터 준비 시간 확보
     */
    @Scheduled(cron = "${scheduler.weather.short-term-cron:0 10 2,5,8,11,14,17,20,23 * * *}")
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

            // 동기화 성공 후 단기예보 기반 추천 정보 생성 트리거 (15분 후)
            if (result.successfulRegions() > 0) {
                triggerRecommendationGeneration("단기예보 동기화 후", 15, "단기예보");
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
    @Scheduled(cron = "${scheduler.weather.medium-term-cron:0 30 6,18 * * *}")
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

            // 동기화 성공 후 중기예보 기반 추천 정보 생성 트리거 (30분 후)
            if (result.successfulRegions() > 0) {
                triggerRecommendationGeneration("중기예보 동기화 후", 30, "중기예보");
            }

        } catch (Exception e) {
            log.error("중기 예보 동기화 스케줄러 실행 중 오류 발생", e);
        } finally {
            mediumTermSyncRunning = false;
        }
    }

    /**
     * 단기예보 기반 추천 정보 생성 스케줄러 (0-3일, 실제 단기예보 데이터 범위)
     * 매 시간 5분에 실행 - 단기예보는 1시간마다 업데이트
     */
    @Scheduled(cron = "${scheduler.weather.recommendation.short-term-cron:0 5 * * * *}")
    @Async("weatherTaskExecutor")
    public void scheduledShortTermRecommendationGeneration() {
        if (shortTermRecommendationRunning) {
            log.warn("단기예보 추천 생성이 이미 실행 중입니다. 스킵합니다.");
            return;
        }

        try {
            shortTermRecommendationRunning = true;
            log.info("단기예보 추천 생성 스케줄러 시작 (실제 단기예보 데이터 기반)");

            // 오늘부터 4일간만 처리 (실제 단기예보 데이터가 있는 범위)
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusDays(3);

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
     * 중기예보 기반 추천 정보 생성 스케줄러 (4-10일, 실제 중기예보 데이터 범위)
     * 매 6시간 30분에 실행 - 중기예보는 12시간마다 업데이트되므로 6시간마다 충분
     */
    @Scheduled(cron = "${scheduler.weather.recommendation.medium-term-cron:0 30 0,6,12,18 * * *}")
    @Async("weatherTaskExecutor")
    public void scheduledMediumTermRecommendationGeneration() {
        if (mediumTermRecommendationRunning) {
            log.warn("중기예보 추천 생성이 이미 실행 중입니다. 스킵합니다.");
            return;
        }

        try {
            mediumTermRecommendationRunning = true;
            log.info("중기예보 추천 생성 스케줄러 시작 (실제 중기예보 데이터 기반)");

            // 4일후부터 3일간만 처리 (일반적인 서비스 범위)
            LocalDate startDate = LocalDate.now().plusDays(4);
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
     * 전체 범위 추천 정보 생성 스케줄러 (0-6일 전체)
     * 하루에 한 번 실행해서 누락된 날짜가 없도록 보장
     * 실제 데이터 존재 여부에 따라 동적으로 처리
     */
    @Scheduled(cron = "${scheduler.weather.recommendation.complete-cron:0 0 4 * * *}")
    @Async("weatherTaskExecutor")
    public void scheduledCompleteRecommendationGeneration() {
        if (completeRecommendationRunning) {
            log.warn("전체 범위 추천 생성이 이미 실행 중입니다. 스킵합니다.");
            return;
        }

        try {
            completeRecommendationRunning = true;
            log.info("전체 범위 추천 생성 스케줄러 시작 (0-6일, 데이터 존재 여부 기반)");

            // 전체 7일간 처리 (실제 데이터 존재 여부에 따라 동적 처리)
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusDays(6);

            WeatherSyncResDTO.RecommendationGenerationResult result =
                    recommendationGenerationService.generateRecommendations(
                            null, startDate, endDate, false, "전체범위"); // 강제 재생성 X, 누락된 것만

            log.info("전체 범위 추천 생성 스케줄러 완료: 성공 {}/{} 지역, 신규 {} 건, 업데이트 {} 건",
                    result.successfulRegions(), result.totalRegions(),
                    result.newRecommendations(), result.updatedRecommendations());

            // 누락된 추천이 많으면 경고
            if (result.totalRecommendations() < result.totalRegions() * 7 * 0.8) {
                log.warn("전체 범위 추천 생성에서 상당수 누락 감지. 전체: {}, 예상: {}, 누락률: {}%",
                        result.totalRecommendations(),
                        result.totalRegions() * 7,
                        100 - (result.totalRecommendations() * 100.0 / (result.totalRegions() * 7)));
            }

        } catch (Exception e) {
            log.error("전체 범위 추천 생성 스케줄러 실행 중 오류 발생", e);
        } finally {
            completeRecommendationRunning = false;
        }
    }

    /**
     * 데이터 정리 스케줄러
     * 매일 새벽 3시에 실행
     */
    @Scheduled(cron = "${scheduler.weather.cleanup-cron:0 0 3 * * *}")
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
            log.debug("스케줄러 실행 상태 - 단기예보수집: {}, 중기예보수집: {}, 단기추천: {}, 중기추천: {}, 전체추천: {}, 정리작업: {}",
                    shortTermSyncRunning, mediumTermSyncRunning,
                    shortTermRecommendationRunning, mediumTermRecommendationRunning,
                    completeRecommendationRunning, cleanupRunning);

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
            if (completeRecommendationRunning) {
                log.warn("전체 범위 추천 생성이 장시간 실행 중입니다. 확인이 필요합니다.");
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
                    // 전체 기간 추천 정보 생성 (실제 데이터 존재 여부 기반)
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
     * 추천 정보 생성 트리거 (비동기, 지연 실행)
     */
    private void triggerRecommendationGeneration(String trigger, int delayMinutes, String recommendationType) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delayMinutes * 60 * 1000); // 지연 시간
                log.debug("{} 추천 정보 생성 트리거: {}", recommendationType, trigger);

                LocalDate startDate = LocalDate.now();
                LocalDate endDate;

                if ("단기예보".equals(recommendationType)) {
                    endDate = startDate.plusDays(3); // 실제 단기예보 범위
                } else if ("중기예보".equals(recommendationType)) {
                    startDate = LocalDate.now().plusDays(4); // 중기예보 시작점
                    endDate = LocalDate.now().plusDays(6);
                } else {
                    endDate = startDate.plusDays(6); // 전체 범위
                }

                recommendationGenerationService.generateRecommendations(
                        null, startDate, endDate, true, "트리거-" + trigger);
                log.debug("{} 추천 정보 생성 트리거 완료: {}", recommendationType, trigger);

            } catch (Exception e) {
                log.error("{} 추천 정보 생성 트리거 실패: {}", recommendationType, trigger, e);
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
                completeRecommendationRunning,
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
            boolean completeRecommendationRunning,
            boolean cleanupRunning,
            LocalDateTime statusTime
    ) {}
}
