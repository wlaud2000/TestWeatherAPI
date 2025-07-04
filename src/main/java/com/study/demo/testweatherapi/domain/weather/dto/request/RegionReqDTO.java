package com.study.demo.testweatherapi.domain.weather.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class RegionReqDTO {

    /**
     * 지역 등록 요청 (관리자용)
     */
    public record CreateRegion(
            @NotBlank(message = "지역명은 필수 입력값입니다.")
            String name,

            @NotNull(message = "위도는 필수 입력값입니다.")
            @DecimalMin(value = "33.0", message = "위도는 33.0 이상이어야 합니다.")
            @DecimalMax(value = "38.0", message = "위도는 38.0 이하여야 합니다.")
            BigDecimal latitude,

            @NotNull(message = "경도는 필수 입력값입니다.")
            @DecimalMin(value = "124.0", message = "경도는 124.0 이상이어야 합니다.")
            @DecimalMax(value = "132.0", message = "경도는 132.0 이하여야 합니다.")
            BigDecimal longitude,

            @NotBlank(message = "지역코드는 필수 입력값입니다.")
            String regCode
    ) {
    }

    /**
     * 지역 수정 요청 (관리자용)
     */
    public record UpdateRegion(
            @NotBlank(message = "지역명은 필수 입력값입니다.")
            String name,

            @NotNull(message = "위도는 필수 입력값입니다.")
            @DecimalMin(value = "33.0", message = "위도는 33.0 이상이어야 합니다.")
            @DecimalMax(value = "38.0", message = "위도는 38.0 이하여야 합니다.")
            BigDecimal latitude,

            @NotNull(message = "경도는 필수 입력값입니다.")
            @DecimalMin(value = "124.0", message = "경도는 124.0 이상이어야 합니다.")
            @DecimalMax(value = "132.0", message = "경도는 132.0 이하여야 합니다.")
            BigDecimal longitude,

            @NotBlank(message = "지역코드는 필수 입력값입니다.")
            String regCode
    ) {
    }
}
