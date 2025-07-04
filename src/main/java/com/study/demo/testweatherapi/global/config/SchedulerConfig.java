package com.study.demo.testweatherapi.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulerConfig {

    /**
     * 날씨 데이터 처리 전용 스레드 풀
     */
    @Bean("weatherTaskExecutor")
    public Executor weatherTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 스레드 풀 설정
        executor.setCorePoolSize(3);        // 기본 스레드 수
        executor.setMaxPoolSize(10);        // 최대 스레드 수
        executor.setQueueCapacity(25);      // 큐 용량
        executor.setKeepAliveSeconds(60);   // 유휴 스레드 유지 시간

        // 스레드 이름 설정
        executor.setThreadNamePrefix("weather-task-");

        // 거부 정책 설정 (큐가 가득 찬 경우)
        executor.setRejectedExecutionHandler((runnable, executor1) -> {
            log.warn("날씨 작업 큐가 가득 찼습니다. 작업을 거부합니다: {}", runnable.toString());
            throw new RuntimeException("날씨 작업 큐가 가득 참");
        });

        // 애플리케이션 종료 시 작업 완료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("날씨 작업 전용 스레드 풀 초기화 완료: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * 일반 비동기 작업용 스레드 풀
     */
    @Bean("asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-task-");

        executor.setRejectedExecutionHandler((runnable, executor1) -> {
            log.warn("비동기 작업 큐가 가득 찼습니다: {}", runnable.toString());
        });

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);

        executor.initialize();
        return executor;
    }
}
