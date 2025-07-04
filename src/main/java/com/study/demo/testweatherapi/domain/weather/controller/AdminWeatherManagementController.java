package com.study.demo.testweatherapi.domain.weather.controller;

import com.study.demo.testweatherapi.domain.weather.dto.request.WeatherSyncReqDTO;
import com.study.demo.testweatherapi.domain.weather.dto.response.WeatherSyncResDTO;
import com.study.demo.testweatherapi.domain.weather.service.WeatherDataCleanupService;
import com.study.demo.testweatherapi.domain.weather.service.WeatherDataCollectionService;
import com.study.demo.testweatherapi.domain.weather.service.WeatherRecommendationGenerationService;
import com.study.demo.testweatherapi.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/admin/weather/management")
@RequiredArgsConstructor
@Tag(name = "날씨 관리 API", description = "관리자용 날씨 데이터 관리 및 수동 트리거 API")
public class AdminWeatherManagementController {

    private final WeatherDataCollectionService dataCollectionService;
    private final WeatherRecommendationGenerationService recommendationGenerationService;
    private final WeatherDataCleanupService dataCleanupService;

    /**
     * 수동 트리거 - 통합 관리
     */
    @PostMapping("/trigger")
    @Operation(summary = "수동 트리거",
            description = "관리자가 수동으로 날씨 데이터 동기화 작업을 트리거합니다.")
    public ResponseEntity<CustomResponse<WeatherSyncResDTO.ManualTriggerResult>> manualTrigger(
            @Valid @RequestBody WeatherSyncReqDTO.ManualTrigger request) {

        log.info("수동 트리거 요청: jobType={}, targetRegionIds={}, forceExecution={}, asyncExecution={}",
                request.jobType(), request.targetRegionIds(), request.forceExecution(), request.asyncExecution());

        LocalDateTime triggerTime = LocalDateTime.now();
        String executionId = "manual_" + System.currentTimeMillis();

        try {
            if (request.asyncExecution()) {
                // 비동기 실행
                CompletableFuture.runAsync(() -> executeJob(request, executionId));

                WeatherSyncResDTO.ManualTriggerResult result = WeatherSyncResDTO.ManualTriggerResult.builder()
                        .jobType(request.jobType())
                        .triggered(true)
                        .asyncExecution(true)
                        .executionId(executionId)
                        .triggerTime(triggerTime)
                        .status("STARTED")
                        .message("비동기 작업이 시작되었습니다.")
                        .result(null)
                        .build();

                return ResponseEntity.ok(CustomResponse.onSuccess(result));

            } else {
                // 동기 실행
                Object jobResult = executeJob(request, executionId);

                WeatherSyncResDTO.ManualTriggerResult result = WeatherSyncResDTO.ManualTriggerResult.builder()
                        .jobType(request.jobType())
                        .triggered(true)
                        .asyncExecution(false)
                        .executionId(executionId)
                        .triggerTime(triggerTime)
                        .status("COMPLETED")
                        .message("작업이 완료되었습니다.")
                        .result(jobResult)
                        .build();

                return ResponseEntity.ok(CustomResponse.onSuccess(result));
            }

        } catch (Exception e) {
            log.error("수동 트리거 실행 실패: jobType={}", request.jobType(), e);

            WeatherSyncResDTO.ManualTriggerResult result = WeatherSyncResDTO.ManualTriggerResult.builder()
                    .jobType(request.jobType())
                    .triggered(false)
                    .asyncExecution(request.asyncExecution())
                    .executionId(executionId)
                    .triggerTime(triggerTime)
                    .status("FAILED")
                    .message("작업 실행 실패: " + e.getMessage())
                    .result(null)
                    .build();

            return ResponseEntity.ok(CustomResponse.onSuccess(result));
        }
    }

    /**
     * 데이터 정리 (관리자 전용)
     */
    @PostMapping("/cleanup")
    @Operation(summary = "데이터 정리",
            description = "오래된 날씨 데이터를 정리합니다.")
    public ResponseEntity<CustomResponse<WeatherSyncResDTO.CleanupResult>> cleanupWeatherData(
            @Valid @RequestBody WeatherSyncReqDTO.CleanupWeatherData request) {

        log.info("데이터 정리 요청: retentionDays={}, dryRun={}", request.retentionDays(), request.dryRun());

        WeatherSyncResDTO.CleanupResult result = dataCleanupService.cleanupOldWeatherData(
                request.retentionDays(),
                request.cleanupShortTerm(),
                request.cleanupMediumTerm(),
                request.cleanupRecommendations(),
                request.dryRun());

        return ResponseEntity.ok(CustomResponse.onSuccess(result));
    }

    /**
     * 시스템 상태 조회
     */
    @GetMapping("/status")
    @Operation(summary = "시스템 상태 조회",
            description = "날씨 데이터 시스템의 전반적인 상태를 조회합니다.")
    public ResponseEntity<CustomResponse<WeatherSyncResDTO.WeatherSystemStatus>> getSystemStatus() {

        log.info("시스템 상태 조회 요청");

        // 시스템 상태 정보 수집
        WeatherSyncResDTO.LastSyncInfo lastShortTermSync = WeatherSyncResDTO.LastSyncInfo.builder()
                .lastExecutionTime(LocalDateTime.now().minusHours(2))  // 실제로는 DB에서 조회
                .lastExecutionSuccess(true)
                .processedItems(15)
                .status("SUCCESS")
                .nextScheduledTime(LocalDateTime.now().plusHours(1))
                .build();

        WeatherSyncResDTO.LastSyncInfo lastMediumTermSync = WeatherSyncResDTO.LastSyncInfo.builder()
                .lastExecutionTime(LocalDateTime.now().minusHours(6))
                .lastExecutionSuccess(true)
                .processedItems(15)
                .status("SUCCESS")
                .nextScheduledTime(LocalDateTime.now().plusHours(6))
                .build();

        WeatherSyncResDTO.LastSyncInfo lastRecommendationGeneration = WeatherSyncResDTO.LastSyncInfo.builder()
                .lastExecutionTime(LocalDateTime.now().minusMinutes(30))
                .lastExecutionSuccess(true)
                .processedItems(105)
                .status("SUCCESS")
                .nextScheduledTime(LocalDateTime.now().plusHours(3))
                .build();

        WeatherSyncResDTO.SystemHealth systemHealth = WeatherSyncResDTO.SystemHealth.builder()
                .overallStatus("HEALTHY")
                .activeIssues(List.of())
                .dataFreshnessScore(95.0)
                .systemPerformanceScore(88.0)
                .build();

        WeatherSyncResDTO.DataStatistics dataStatistics = WeatherSyncResDTO.DataStatistics.builder()
                .totalRegions(15)  // 실제로는 DB에서 조회
                .activeRegions(15)
                .totalShortTermRecords(25000)
                .totalMediumTermRecords(5000)
                .totalRecommendations(1050)
                .oldestDataDate(LocalDate.now().minusDays(7))
                .newestDataDate(LocalDate.now().plusDays(6))
                .build();

        WeatherSyncResDTO.WeatherSystemStatus status = WeatherSyncResDTO.WeatherSystemStatus.builder()
                .lastShortTermSync(lastShortTermSync)
                .lastMediumTermSync(lastMediumTermSync)
                .lastRecommendationGeneration(lastRecommendationGeneration)
                .systemHealth(systemHealth)
                .dataStatistics(dataStatistics)
                .statusGeneratedAt(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(CustomResponse.onSuccess(status));
    }

    /**
     * 긴급 전체 재동기화
     */
    @PostMapping("/emergency-resync")
    @Operation(summary = "긴급 전체 재동기화",
            description = "모든 데이터를 강제로 재동기화합니다. (주의: 시간이 오래 걸림)")
    public ResponseEntity<CustomResponse<WeatherSyncResDTO.ManualTriggerResult>> emergencyResync(
            @Parameter(description = "강제 실행 확인", required = true)
            @RequestParam boolean confirmForceExecution) {

        if (!confirmForceExecution) {
            throw new IllegalArgumentException("긴급 재동기화는 confirmForceExecution=true로 확인이 필요합니다.");
        }

        log.warn("긴급 전체 재동기화 시작 - 관리자 요청");

        String executionId = "emergency_" + System.currentTimeMillis();
        LocalDateTime triggerTime = LocalDateTime.now();

        // 비동기로 실행 (시간이 오래 걸리므로)
        CompletableFuture.runAsync(() -> {
            try {
                log.info("긴급 재동기화 실행 시작: executionId={}", executionId);

                // 모든 지역, 강제 업데이트로 전체 동기화
                LocalDateTime now = LocalDateTime.now();
                LocalDate baseDate = now.toLocalDate();
                String baseTime = calculateNearestBaseTime(now.getHour());

                dataCollectionService.collectShortTermWeatherData(null, baseDate, baseTime, true);
                dataCollectionService.collectMediumTermWeatherData(null, LocalDate.now(), true);

                LocalDate startDate = LocalDate.now();
                LocalDate endDate = startDate.plusDays(6);
                recommendationGenerationService.generateRecommendations(null, startDate, endDate, true);

                log.info("긴급 재동기화 완료: executionId={}", executionId);

            } catch (Exception e) {
                log.error("긴급 재동기화 실패: executionId={}", executionId, e);
            }
        });

        WeatherSyncResDTO.ManualTriggerResult result = WeatherSyncResDTO.ManualTriggerResult.builder()
                .jobType("EMERGENCY_RESYNC")
                .triggered(true)
                .asyncExecution(true)
                .executionId(executionId)
                .triggerTime(triggerTime)
                .status("STARTED")
                .message("긴급 전체 재동기화가 시작되었습니다. 완료까지 시간이 걸릴 수 있습니다.")
                .result(null)
                .build();

        return ResponseEntity.ok(CustomResponse.onSuccess(result));
    }

    // ==== 내부 유틸리티 메서드들 ====

    /**
     * 작업 타입에 따른 실제 작업 실행
     */
    private Object executeJob(WeatherSyncReqDTO.ManualTrigger request, String executionId) {
        log.info("작업 실행 시작: jobType={}, executionId={}", request.jobType(), executionId);

        return switch (request.jobType()) {
            case "SHORT_TERM" -> {
                LocalDateTime now = LocalDateTime.now();
                LocalDate baseDate = now.toLocalDate();
                String baseTime = calculateNearestBaseTime(now.getHour());
                yield dataCollectionService.collectShortTermWeatherData(
                        request.targetRegionIds(), baseDate, baseTime, request.forceExecution());
            }

            case "MEDIUM_TERM" -> dataCollectionService.collectMediumTermWeatherData(
                    request.targetRegionIds(), LocalDate.now(), request.forceExecution());

            case "RECOMMENDATION" -> {
                LocalDate startDate = LocalDate.now();
                LocalDate endDate = startDate.plusDays(6);
                yield recommendationGenerationService.generateRecommendations(
                        request.targetRegionIds(), startDate, endDate, request.forceExecution());
            }

            case "CLEANUP" -> dataCleanupService.cleanupOldWeatherData(
                    7, true, true, true, false);

            case "ALL" -> {
                // 전체 동기화 실행
                LocalDateTime now = LocalDateTime.now();
                LocalDate baseDate = now.toLocalDate();
                String baseTime = calculateNearestBaseTime(now.getHour());

                var shortResult = dataCollectionService.collectShortTermWeatherData(
                        request.targetRegionIds(), baseDate, baseTime, request.forceExecution());

                var mediumResult = dataCollectionService.collectMediumTermWeatherData(
                        request.targetRegionIds(), LocalDate.now(), request.forceExecution());

                LocalDate startDate = LocalDate.now();
                LocalDate endDate = startDate.plusDays(6);
                var recommendationResult = recommendationGenerationService.generateRecommendations(
                        request.targetRegionIds(), startDate, endDate, request.forceExecution());

                yield WeatherSyncResDTO.CompleteSyncResult.builder()
                        .shortTermResult(shortResult)
                        .mediumTermResult(mediumResult)
                        .recommendationResult(recommendationResult)
                        .cleanupResult(null)
                        .overallStartTime(LocalDateTime.now())
                        .overallEndTime(LocalDateTime.now())
                        .overallDurationMs(0L)
                        .allSuccessful(true)
                        .summaryMessages(List.of("전체 동기화 완료"))
                        .overallStatus("SUCCESS")
                        .build();
            }

            default -> throw new IllegalArgumentException("지원하지 않는 작업 타입: " + request.jobType());
        };
    }

    /**
     * 현재 시각을 기준으로 가장 가까운 기준시각 계산
     */
    private String calculateNearestBaseTime(int currentHour) {
        int[] baseTimes = {2, 5, 8, 11, 14, 17, 20, 23};

        for (int i = baseTimes.length - 1; i >= 0; i--) {
            if (currentHour >= baseTimes[i]) {
                return String.format("%02d00", baseTimes[i]);
            }
        }

        return "2300";
    }
}
