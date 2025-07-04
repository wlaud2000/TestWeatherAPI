package com.study.demo.testweatherapi.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class WeatherWebClientConfig {

    @Value("${weather.api.base-url}")
    private String baseUrl;

    @Value("${weather.api.timeout.connect}")
    private int connectTimeout;

    @Value("${weather.api.timeout.read}")
    private int readTimeout;

    /**
     * 기상청 API 전용 WebClient 설정
     */
    @Bean
    public WebClient weatherWebClient() {
        // HTTP 클라이언트 타임아웃 설정
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .responseTimeout(Duration.ofMillis(readTimeout))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(connectTimeout, TimeUnit.MILLISECONDS))
                );

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .filter(handleErrors())
                .build();
    }

    /**
     * 요청 로깅 필터
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("기상청 API 요청: {} {}", clientRequest.method(), clientRequest.url());
            log.debug("요청 헤더: {}", clientRequest.headers());
            return Mono.just(clientRequest);
        });
    }

    /**
     * 응답 로깅 필터
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("기상청 API 응답: {}", clientResponse.statusCode());
            log.debug("응답 헤더: {}", clientResponse.headers().asHttpHeaders());
            return Mono.just(clientResponse);
        });
    }

    /**
     * 에러 처리 필터
     */
    private ExchangeFilterFunction handleErrors() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().isError()) {
                log.error("기상청 API 오류 응답: {} {}",
                        clientResponse.statusCode().value(),
                        clientResponse.statusCode());
            }
            return Mono.just(clientResponse);
        });
    }
}
