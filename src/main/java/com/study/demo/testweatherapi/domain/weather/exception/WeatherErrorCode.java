package com.study.demo.testweatherapi.domain.weather.exception;

import com.study.demo.testweatherapi.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WeatherErrorCode implements BaseErrorCode {

    // ==== 지역 관련 에러 (404) ====
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "WEATHER404_0", "지역을 찾을 수 없습니다."),
    WEATHER_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "WEATHER404_1", "날씨 데이터를 찾을 수 없습니다."),
    WEATHER_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "WEATHER404_2", "날씨 템플릿을 찾을 수 없습니다."),
    DAILY_RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "WEATHER404_3", "일일 추천 정보를 찾을 수 없습니다."),
    KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "WEATHER404_4", "키워드를 찾을 수 없습니다."),

    // ==== 데이터 중복/충돌 에러 (400) ====
    REGION_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "WEATHER400_0", "이미 존재하는 지역입니다."),
    WEATHER_TEMPLATE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "WEATHER400_1", "이미 존재하는 날씨 템플릿입니다."),
    INVALID_COORDINATES(HttpStatus.BAD_REQUEST, "WEATHER400_2", "올바르지 않은 좌표입니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "WEATHER400_3", "올바르지 않은 날짜 범위입니다."),
    INVALID_WEATHER_DATA(HttpStatus.BAD_REQUEST, "WEATHER400_4", "올바르지 않은 날씨 데이터입니다."),
    INVALID_REGION_CODE(HttpStatus.BAD_REQUEST, "WEATHER400_5", "올바르지 않은 지역코드입니다."),
    INVALID_ENUM_VALUE(HttpStatus.BAD_REQUEST, "WEATHER400_6", "올바르지 않은 열거형 값입니다."),

    // ==== 권한/인증 에러 (403) ====
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "WEATHER403_0", "접근 권한이 없습니다."),
    ADMIN_ONLY_ACCESS(HttpStatus.FORBIDDEN, "WEATHER403_1", "관리자만 접근할 수 있습니다."),

    // ==== 기상청 API 관련 에러 (500) ====
    WEATHER_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "WEATHER500_0", "기상청 API 호출 중 오류가 발생했습니다."),
    GRID_CONVERSION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "WEATHER500_1", "격자 좌표 변환 중 오류가 발생했습니다."),
    SHORT_TERM_FORECAST_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "WEATHER500_2", "단기 예보 조회 중 오류가 발생했습니다."),
    MEDIUM_TERM_FORECAST_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "WEATHER500_3", "중기 예보 조회 중 오류가 발생했습니다."),
    API_RESPONSE_PARSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "WEATHER500_4", "API 응답 파싱 중 오류가 발생했습니다."),

    // ==== 데이터 처리 관련 에러 (500) ====
    WEATHER_DATA_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "WEATHER500_10", "날씨 데이터 처리 중 오류가 발생했습니다."),
    WEATHER_CLASSIFICATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "WEATHER500_11", "날씨 분류 중 오류가 발생했습니다."),
    TEMPLATE_MATCHING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "WEATHER500_12", "템플릿 매칭 중 오류가 발생했습니다."),
    RECOMMENDATION_GENERATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "WEATHER500_13", "추천 생성 중 오류가 발생했습니다."),

    // ==== 스케줄러 관련 에러 (500) ====
    SCHEDULER_EXECUTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "WEATHER500_20", "스케줄러 실행 중 오류가 발생했습니다."),
    DATA_COLLECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "WEATHER500_21", "데이터 수집 중 오류가 발생했습니다."),
    DATA_CLEANUP_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "WEATHER500_22", "데이터 정리 중 오류가 발생했습니다."),

    // ==== 외부 서비스 에러 (502, 503) ====
    EXTERNAL_API_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "WEATHER503_0", "외부 API 서비스를 사용할 수 없습니다."),
    API_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "WEATHER429_0", "API 호출 횟수 제한에 도달했습니다."),
    API_TIMEOUT_ERROR(HttpStatus.GATEWAY_TIMEOUT, "WEATHER504_0", "API 응답 시간이 초과되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
