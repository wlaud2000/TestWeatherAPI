package com.study.demo.testweatherapi.domain.weather.dto.response;

import com.study.demo.testweatherapi.domain.weather.entity.enums.WeatherType;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class WeatherSyncResDTO {

    /**
     * 단기 예보 동기화 결과 DTO
     */
    @Builder
    public record ShortTermSyncResult(
            int totalRegions,           // 처리된 지역 수
            int successfulRegions,      // 성공한 지역 수
            int failedRegions,          // 실패한 지역 수
            int totalDataPoints,        // 전체 데이터 포인트 수
            int newDataPoints,          // 새로 추가된 데이터 포인트 수
            int updatedDataPoints,      // 업데이트된 데이터 포인트 수
            LocalDate baseDate,         // 기준 날짜
            String baseTime,            // 기준 시간
            LocalDateTime processingStartTime,  // 처리 시작 시간
            LocalDateTime processingEndTime,    // 처리 종료 시간
            long processingDurationMs,          // 처리 소요 시간 (밀리초)
            List<RegionSyncResult> regionResults,  // 지역별 결과
            List<String> errorMessages,         // 오류 메시지들
            String message                      // 전체 결과 메시지
    ) {
    }

    /**
     * 중기 예보 동기화 결과 DTO
     */
    @Builder
    public record MediumTermSyncResult(
            int totalRegions,           // 처리된 지역 수
            int successfulRegions,      // 성공한 지역 수
            int failedRegions,          // 실패한 지역 수
            int totalDataPoints,        // 전체 데이터 포인트 수
            int newDataPoints,          // 새로 추가된 데이터 포인트 수
            int updatedDataPoints,      // 업데이트된 데이터 포인트 수
            LocalDate tmfc,             // 발표 시각
            LocalDateTime processingStartTime,  // 처리 시작 시간
            LocalDateTime processingEndTime,    // 처리 종료 시간
            long processingDurationMs,          // 처리 소요 시간 (밀리초)
            List<RegionSyncResult> regionResults,  // 지역별 결과
            List<String> errorMessages,         // 오류 메시지들
            String message                      // 전체 결과 메시지
    ) {
    }

    /**
     * 지역별 동기화 결과
     */
    @Builder
    public record RegionSyncResult(
            Long regionId,
            String regionName,
            boolean success,
            int dataPointsProcessed,
            int newDataPoints,
            int updatedDataPoints,
            String errorMessage,
            long processingTimeMs
    ) {
    }

    /**
     * 추천 정보 생성 결과 DTO
     */
    @Builder
    public record RecommendationGenerationResult(
            int totalRegions,                   // 처리된 지역 수
            int successfulRegions,              // 성공한 지역 수
            int failedRegions,                  // 실패한 지역 수
            int totalRecommendations,           // 전체 생성된 추천 수
            int newRecommendations,             // 새로 생성된 추천 수
            int updatedRecommendations,         // 업데이트된 추천 수
            LocalDate startDate,                // 시작 날짜
            LocalDate endDate,                  // 종료 날짜
            LocalDateTime processingStartTime,  // 처리 시작 시간
            LocalDateTime processingEndTime,    // 처리 종료 시간
            long processingDurationMs,          // 처리 소요 시간 (밀리초)
            List<RegionRecommendationResult> regionResults,  // 지역별 결과
            WeatherTypeStatistics weatherStats, // 날씨별 통계
            List<String> errorMessages,         // 오류 메시지들
            String message                      // 전체 결과 메시지
    ) {
    }

    /**
     * 지역별 추천 생성 결과
     */
    @Builder
    public record RegionRecommendationResult(
            Long regionId,
            String regionName,
            boolean success,
            int recommendationsGenerated,
            int newRecommendations,
            int updatedRecommendations,
            List<String> processedDates,  // 처리된 날짜들
            String errorMessage,
            long processingTimeMs
    ) {
    }

    /**
     * 날씨 타입별 통계
     */
    @Builder
    public record WeatherTypeStatistics(
            int clearWeatherCount,      // 맑음
            int cloudyWeatherCount,     // 흐림
            int snowWeatherCount,       // 눈
            Map<WeatherType, Integer> detailedStats  // 상세 통계
    ) {
    }

    /**
     * 데이터 정리 결과 DTO
     */
    @Builder
    public record CleanupResult(
            boolean dryRun,                     // Dry run 여부
            int retentionDays,                  // 보관 기간
            LocalDate cutoffDate,               // 삭제 기준 날짜
            CleanupStats shortTermStats,        // 단기 예보 정리 결과
            CleanupStats mediumTermStats,       // 중기 예보 정리 결과
            CleanupStats recommendationStats,   // 추천 정보 정리 결과
            LocalDateTime processingStartTime,  // 처리 시작 시간
            LocalDateTime processingEndTime,    // 처리 종료 시간
            long processingDurationMs,          // 처리 소요 시간 (밀리초)
            List<String> errorMessages,         // 오류 메시지들
            String message                      // 전체 결과 메시지
    ) {
    }

    /**
     * 정리 작업별 통계
     */
    @Builder
    public record CleanupStats(
            String dataType,            // 데이터 타입 (단기/중기/추천)
            boolean executed,           // 실행 여부
            int recordsFound,           // 발견된 레코드 수
            int recordsDeleted,         // 삭제된 레코드 수
            long spaceSavedMB           // 절약된 공간 (MB, 추정치)
    ) {
    }

    /**
     * 전체 동기화 결과 DTO (모든 작업 포함)
     */
    @Builder
    public record CompleteSyncResult(
            ShortTermSyncResult shortTermResult,        // 단기 예보 결과
            MediumTermSyncResult mediumTermResult,      // 중기 예보 결과
            RecommendationGenerationResult recommendationResult,  // 추천 생성 결과
            CleanupResult cleanupResult,                // 정리 작업 결과
            LocalDateTime overallStartTime,             // 전체 시작 시간
            LocalDateTime overallEndTime,               // 전체 종료 시간
            long overallDurationMs,                     // 전체 소요 시간 (밀리초)
            boolean allSuccessful,                      // 모든 작업 성공 여부
            List<String> summaryMessages,               // 요약 메시지들
            String overallStatus                        // 전체 상태
    ) {
    }

    /**
     * 수동 트리거 결과 DTO
     */
    @Builder
    public record ManualTriggerResult(
            String jobType,                     // 작업 타입
            boolean triggered,                  // 트리거 성공 여부
            boolean asyncExecution,             // 비동기 실행 여부
            String executionId,                 // 실행 ID (비동기인 경우)
            LocalDateTime triggerTime,          // 트리거 시간
            String status,                      // 상태
            String message,                     // 메시지
            Object result                       // 결과 (동기 실행인 경우)
    ) {
    }

    /**
     * 시스템 상태 정보 DTO (관리자 대시보드용)
     */
    @Builder
    public record WeatherSystemStatus(
            LastSyncInfo lastShortTermSync,     // 마지막 단기 예보 동기화
            LastSyncInfo lastMediumTermSync,    // 마지막 중기 예보 동기화
            LastSyncInfo lastRecommendationGeneration,  // 마지막 추천 생성
            SystemHealth systemHealth,          // 시스템 건강도
            DataStatistics dataStatistics,      // 데이터 통계
            LocalDateTime statusGeneratedAt     // 상태 생성 시간
    ) {
    }

    /**
     * 마지막 동기화 정보
     */
    @Builder
    public record LastSyncInfo(
            LocalDateTime lastExecutionTime,    // 마지막 실행 시간
            boolean lastExecutionSuccess,       // 마지막 실행 성공 여부
            int processedItems,                 // 처리된 항목 수
            String status,                      // 상태
            LocalDateTime nextScheduledTime     // 다음 예정 시간
    ) {
    }

    /**
     * 시스템 건강도
     */
    @Builder
    public record SystemHealth(
            String overallStatus,               // 전체 상태 (HEALTHY/WARNING/ERROR)
            List<String> activeIssues,          // 활성 이슈들
            double dataFreshnessScore,          // 데이터 신선도 점수 (0-100)
            double systemPerformanceScore       // 시스템 성능 점수 (0-100)
    ) {
    }

    /**
     * 데이터 통계
     */
    @Builder
    public record DataStatistics(
            int totalRegions,                   // 전체 지역 수
            int activeRegions,                  // 활성 지역 수
            int totalShortTermRecords,          // 전체 단기 예보 레코드 수
            int totalMediumTermRecords,         // 전체 중기 예보 레코드 수
            int totalRecommendations,           // 전체 추천 정보 수
            LocalDate oldestDataDate,           // 가장 오래된 데이터 날짜
            LocalDate newestDataDate            // 가장 최신 데이터 날짜
    ) {
    }

    /**
     * 상세한 데이터 정리 통계 (관리자용)
     */
    @Builder
    public record DetailedCleanupStats(
            LocalDate cutoffDate,                           // 기준 날짜
            int shortTermRecordsFound,                      // 단기예보 삭제 대상
            int mediumTermRecordsFound,                     // 중기예보 삭제 대상
            int recommendationRecordsFound,                 // 추천정보 삭제 대상
            int totalRecordsFound,                          // 전체 삭제 대상
            List<RegionCleanupStats> regionStats,           // 지역별 통계
            long estimatedSpaceSavingMB,                    // 예상 절약 공간
            LocalDateTime previewGeneratedAt                // 통계 생성 시간
    ) {
    }

    /**
     * 지역별 정리 통계
     */
    @Builder
    public record RegionCleanupStats(
            String regionName,                              // 지역명
            String dataType,                                // 데이터 타입 (단기예보/중기예보/추천정보)
            int recordCount                                 // 해당 지역의 삭제 대상 레코드 수
    ) {
    }

    /**
     * 데이터 정리 작업 진행 상황 (실시간 모니터링용)
     */
    @Builder
    public record CleanupProgress(
            String currentTask,                             // 현재 작업 (단기예보정리/중기예보정리/추천정보정리)
            int currentProgress,                            // 현재 진행률 (0-100)
            int totalSteps,                                 // 전체 단계 수
            int completedSteps,                             // 완료된 단계 수
            LocalDateTime startTime,                        // 시작 시간
            LocalDateTime estimatedEndTime,                 // 예상 완료 시간
            String statusMessage                            // 상태 메시지
    ) {
    }

    /**
     * 데이터 정리 실행 계획 (사전 확인용)
     */
    @Builder
    public record CleanupExecutionPlan(
            LocalDate cutoffDate,                           // 기준 날짜
            int retentionDays,                              // 보관 기간
            List<CleanupTask> plannedTasks,                 // 계획된 작업들
            long estimatedDurationMs,                       // 예상 소요 시간
            long estimatedSpaceSavingMB,                    // 예상 절약 공간
            List<String> warnings,                          // 경고 메시지
            boolean safeToExecute,                          // 실행 안전성
            LocalDateTime planGeneratedAt                   // 계획 생성 시간
    ) {
    }

    /**
     * 개별 정리 작업 정보
     */
    @Builder
    public record CleanupTask(
            String taskName,                                // 작업명 (단기예보정리/중기예보정리/추천정보정리)
            String dataType,                                // 데이터 타입
            int recordsToDelete,                            // 삭제 예정 레코드 수
            long estimatedDurationMs,                       // 예상 소요 시간
            String description                              // 작업 설명
    ) {
    }
}
