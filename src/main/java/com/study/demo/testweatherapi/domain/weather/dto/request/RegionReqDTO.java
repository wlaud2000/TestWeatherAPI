package com.study.demo.testweatherapi.domain.weather.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class RegionReqDTO {

    /**
     * 지역 생성 요청 DTO (Option 1: 기존 지역코드 사용)
     * 관리자가 새로운 지역을 등록할 때 사용
     */
    public record CreateRegion(
            @NotBlank(message = "지역명은 필수 입력값입니다.")
            String name,

            @NotNull(message = "위도는 필수 입력값입니다.")
            BigDecimal latitude,

            @NotNull(message = "경도는 필수 입력값입니다.")
            BigDecimal longitude,

            @NotNull(message = "지역코드 ID는 필수 입력값입니다.")
            @Positive(message = "지역코드 ID는 양수여야 합니다.")
            Long regionCodeId
    ) {
    }

    /**
     * 지역 생성 요청 DTO (Option 2: 새 지역코드와 함께 생성)
     */
    public record CreateRegionWithNewCode(
            @NotBlank(message = "지역명은 필수 입력값입니다.")
            String name,

            @NotNull(message = "위도는 필수 입력값입니다.")
            BigDecimal latitude,

            @NotNull(message = "경도는 필수 입력값입니다.")
            BigDecimal longitude,

            @NotBlank(message = "중기 육상 예보 지역코드는 필수 입력값입니다.")
            String landRegCode,

            @NotBlank(message = "중기 기온 예보 지역코드는 필수 입력값입니다.")
            String tempRegCode,

            @NotBlank(message = "지역코드명은 필수 입력값입니다.")
            String regionCodeName,

            String regionCodeDescription
    ) {
    }

    /**
     * 지역 정보 수정 요청 DTO
     */
    public record UpdateRegion(
            @NotBlank(message = "지역명은 필수 입력값입니다.")
            String name,

            @NotNull(message = "위도는 필수 입력값입니다.")
            BigDecimal latitude,

            @NotNull(message = "경도는 필수 입력값입니다.")
            BigDecimal longitude,

            @NotNull(message = "지역코드 ID는 필수 입력값입니다.")
            @Positive(message = "지역코드 ID는 양수여야 합니다.")
            Long regionCodeId
    ) {
    }

    /**
     * 좌표 변환 요청 DTO (격자 좌표 조회용)
     */
    public record CoordinateConversion(
            @NotNull(message = "위도는 필수 입력값입니다.")
            BigDecimal latitude,

            @NotNull(message = "경도는 필수 입력값입니다.")
            BigDecimal longitude
    ) {
    }

    /**
     * 지역코드 생성 요청 DTO
     */
    public record CreateRegionCode(
            @NotBlank(message = "중기 육상 예보 지역코드는 필수 입력값입니다.")
            String landRegCode,

            @NotBlank(message = "중기 기온 예보 지역코드는 필수 입력값입니다.")
            String tempRegCode,

            @NotBlank(message = "지역코드명은 필수 입력값입니다.")
            String name,

            String description
    ) {
    }

    /**
     * 지역코드 수정 요청 DTO
     */
    public record UpdateRegionCode(
            @NotBlank(message = "중기 육상 예보 지역코드는 필수 입력값입니다.")
            String landRegCode,

            @NotBlank(message = "중기 기온 예보 지역코드는 필수 입력값입니다.")
            String tempRegCode,

            @NotBlank(message = "지역코드명은 필수 입력값입니다.")
            String name,

            String description
    ) {
    }
}
