package com.study.demo.testweatherapi.domain.weather.service;

import com.study.demo.testweatherapi.domain.weather.dto.response.KmaApiResDTO;
import com.study.demo.testweatherapi.domain.weather.exception.WeatherErrorCode;
import com.study.demo.testweatherapi.domain.weather.exception.WeatherException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherApiService {

    private final WebClient webClient;

    @Value("${weather.api.service-key}")
    private String serviceKey;

    @Value("${weather.api.base-url}")
    private String baseUrl;

    @Value("${weather.api.timeout:30}")
    private int timeoutSeconds;

    @Value("${weather.api.retry.max-attempts:3}")
    private int maxRetryAttempts;

    /**
     * 격자 데이터 위경도 조회 API (수정됨 - authKey 파라미터 사용)
     */
    public Mono<KmaApiResDTO.GridConversionResponse> convertCoordinatesToGrid(
            BigDecimal latitude, BigDecimal longitude) {

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/typ01/cgi-bin/url/nph-dfs_xy_lonlat")
                .queryParam("authKey", serviceKey)  // authKey 파라미터로 변경
                .queryParam("lat", latitude.toString())
                .queryParam("lon", longitude.toString())
                .build()
                .toUriString();

        log.info("기상청 격자 변환 API 호출 시작: lat={}, lon={}", latitude, longitude);
        log.debug("API URL: {}", url);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(response -> log.debug("격자 변환 API 원본 응답: {}", response))
                .map(KmaApiResDTO.GridConversionResponse::fromTextResponse)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(Retry.fixedDelay(maxRetryAttempts, Duration.ofSeconds(2)))
                .doOnSuccess(response -> {
                    if ("SUCCESS".equals(response.result())) {
                        log.info("격자 변환 성공: gridX={}, gridY={}", response.gridX(), response.gridY());
                    } else {
                        log.error("격자 변환 실패: {}", response.message());
                    }
                })
                .doOnError(error -> {
                    log.error("격자 변환 API 호출 실패: lat={}, lon={}", latitude, longitude, error);
                })
                .onErrorMap(throwable -> new WeatherException(WeatherErrorCode.GRID_CONVERSION_ERROR));
    }

    /**
     * 단기 예보 조회 API (수정됨 - authKey 파라미터 사용)
     */
    public Mono<KmaApiResDTO.ShortTermForecastResponse> getShortTermForecast(
            BigDecimal gridX, BigDecimal gridY, LocalDate baseDate, String baseTime) {

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/typ02/openApi/VilageFcstInfoService_2.0/getVilageFcst")
                .queryParam("authKey", serviceKey)  // authKey 파라미터로 변경
                .queryParam("pageNo", "1")
                .queryParam("numOfRows", "1000")
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .queryParam("base_time", baseTime)
                .queryParam("nx", gridX.intValue())
                .queryParam("ny", gridY.intValue())
                .build()
                .toUriString();

        log.info("단기 예보 API 호출: gridX={}, gridY={}, baseDate={}, baseTime={}",
                gridX, gridY, baseDate, baseTime);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(KmaApiResDTO.ShortTermForecastResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(Retry.fixedDelay(maxRetryAttempts, Duration.ofSeconds(2)))
                .doOnSuccess(response -> {
                    if ("00".equals(response.response().header().resultCode())) {
                        log.info("단기 예보 조회 성공: {} 건",
                                response.response().body().items().item().size());
                    } else {
                        log.error("단기 예보 조회 실패: {}", response.response().header().resultMsg());
                    }
                })
                .doOnError(error -> log.error("단기 예보 API 호출 실패", error))
                .onErrorMap(throwable -> new WeatherException(WeatherErrorCode.WEATHER_API_ERROR));
    }

    /**
     * 중기 기온 예보 조회 API (수정됨 - authKey 파라미터, 시간 파라미터 제거)
     */
    public Mono<KmaApiResDTO.MediumTermTemperatureResponse> getMediumTermTemperature(String regCode) {

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/typ01/url/fct_afs_wc.php")
                .queryParam("authKey", serviceKey)  // authKey 파라미터로 변경
                .queryParam("reg", regCode)
                // 시간 파라미터 제거 - 가장 최근 발표자료 사용
                .build()
                .toUriString();

        log.info("중기 기온 예보 API 호출: regCode={}", regCode);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(response -> log.debug("중기 기온 예보 API 원본 응답: {}", response))
                .map(KmaApiResDTO.MediumTermTemperatureResponse::fromTextResponse)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(Retry.fixedDelay(maxRetryAttempts, Duration.ofSeconds(2)))
                .doOnSuccess(response -> log.info("중기 기온 예보 조회 성공: {} 건",
                        response.temperatureInfos().size()))
                .doOnError(error -> log.error("중기 기온 예보 API 호출 실패", error))
                .onErrorMap(throwable -> new WeatherException(WeatherErrorCode.WEATHER_API_ERROR));
    }

    /**
     * 중기 육상 예보 조회 API (수정됨 - authKey 파라미터, 시간 파라미터 제거)
     */
    public Mono<KmaApiResDTO.MediumTermLandForecastResponse> getMediumTermLandForecast(String regCode) {

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/typ01/url/fct_afs_wl.php")
                .queryParam("authKey", serviceKey)  // authKey 파라미터로 변경
                .queryParam("reg", regCode)
                // 시간 파라미터 제거 - 가장 최근 발표자료 사용
                .build()
                .toUriString();

        log.info("중기 육상 예보 API 호출: regCode={}", regCode);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(response -> log.debug("중기 육상 예보 API 원본 응답: {}", response))
                .map(KmaApiResDTO.MediumTermLandForecastResponse::fromTextResponse)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(Retry.fixedDelay(maxRetryAttempts, Duration.ofSeconds(2)))
                .doOnSuccess(response -> log.info("중기 육상 예보 조회 성공: {} 건",
                        response.landForecastInfos().size()))
                .doOnError(error -> log.error("중기 육상 예보 API 호출 실패", error))
                .onErrorMap(throwable -> new WeatherException(WeatherErrorCode.WEATHER_API_ERROR));
    }

    /**
     * API 상태 확인 (헬스체크용)
     */
    public Mono<Boolean> checkApiHealth() {
        log.info("기상청 API 헬스체크 시작");

        return convertCoordinatesToGrid(
                new BigDecimal("37.5665"), new BigDecimal("126.9780"))
                .map(response -> "SUCCESS".equals(response.result()))
                .doOnSuccess(isHealthy -> {
                    if (isHealthy) {
                        log.info("기상청 API 헬스체크 성공");
                    } else {
                        log.warn("기상청 API 헬스체크 실패");
                    }
                })
                .onErrorReturn(false);
    }
}
