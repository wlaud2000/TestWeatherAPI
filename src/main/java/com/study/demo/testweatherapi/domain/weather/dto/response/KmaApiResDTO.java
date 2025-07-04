package com.study.demo.testweatherapi.domain.weather.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class KmaApiResDTO {

    /**
     * 격자 데이터 위경도 조회 응답 (실제 API 응답에 맞춤)
     * 실제 응답: "#START7777 # LON, LAT, X, Y 126.986069, 37.571712, 60, 127"
     */
    @Builder
    public record GridConversionResponse(
            BigDecimal gridX,
            BigDecimal gridY,
            String result,
            String message
    ) {
        /**
         * 실제 기상청 API 응답 파싱
         * 응답 예시: "#START7777 # LON, LAT, X, Y 126.986069, 37.571712, 60, 127"
         */
        public static GridConversionResponse fromTextResponse(String textResponse) {
            if (textResponse == null || textResponse.trim().isEmpty()) {
                return GridConversionResponse.builder()
                        .result("FAILED")
                        .message("응답 데이터가 없습니다.")
                        .build();
            }

            try {
                // #START7777이 포함된 응답 처리
                String[] lines = textResponse.split("\n");
                for (String line : lines) {
                    line = line.trim();

                    // "# LON, LAT, X, Y 126.986069, 37.571712, 60, 127" 형태 파싱
                    if (line.contains("LON, LAT, X, Y") || line.matches(".*\\d+\\.\\d+.*\\d+.*\\d+.*")) {
                        String[] parts = line.split("\\s+");

                        // 숫자 부분만 추출
                        List<String> numbers = new ArrayList<>();
                        for (String part : parts) {
                            if (part.matches("^[0-9]+(\\.[0-9]+)?$")) {
                                numbers.add(part);
                            }
                        }

                        if (numbers.size() >= 4) {
                            // LON, LAT, X, Y 순서이므로 X=numbers[2], Y=numbers[3]
                            return GridConversionResponse.builder()
                                    .gridX(new BigDecimal(numbers.get(2)))  // X 좌표
                                    .gridY(new BigDecimal(numbers.get(3)))  // Y 좌표
                                    .result("SUCCESS")
                                    .message("격자 변환 성공")
                                    .build();
                        }
                    }

                    // 단순한 4개 숫자 형태인 경우 (위의 조건에 맞지 않을 때)
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 4) {
                        try {
                            // 연속된 4개 숫자 찾기
                            List<BigDecimal> coords = new ArrayList<>();
                            for (String part : parts) {
                                try {
                                    coords.add(new BigDecimal(part));
                                    if (coords.size() == 4) break;
                                } catch (NumberFormatException e) {
                                    // 숫자가 아니면 건너뛰기
                                }
                            }

                            if (coords.size() == 4) {
                                return GridConversionResponse.builder()
                                        .gridX(coords.get(2))  // X 좌표
                                        .gridY(coords.get(3))  // Y 좌표
                                        .result("SUCCESS")
                                        .message("격자 변환 성공")
                                        .build();
                            }
                        } catch (Exception e) {
                            // 이 라인에서 파싱 실패하면 다음 라인 시도
                            continue;
                        }
                    }
                }

                return GridConversionResponse.builder()
                        .result("FAILED")
                        .message("응답에서 격자 좌표를 찾을 수 없습니다.")
                        .build();

            } catch (Exception e) {
                return GridConversionResponse.builder()
                        .result("FAILED")
                        .message("격자 좌표 파싱 실패: " + e.getMessage())
                        .build();
            }
        }
    }

    /**
     * 단기 예보 조회 응답 (JSON 형태 - 수정사항 없음)
     */
    @Builder
    public record ShortTermForecastResponse(
            Response response
    ) {
        @Builder
        public record Response(
                Header header,
                Body body
        ) {
        }

        @Builder
        public record Header(
                String resultCode,
                String resultMsg
        ) {
        }

        @Builder
        public record Body(
                String dataType,
                Items items,
                Integer pageNo,
                Integer numOfRows,
                Integer totalCount
        ) {
        }

        @Builder
        public record Items(
                List<Item> item
        ) {
        }

        @Builder
        public record Item(
                String baseDate,
                String baseTime,
                String category,
                String fcstDate,
                String fcstTime,
                String fcstValue,
                Integer nx,
                Integer ny
        ) {
        }
    }

    /**
     * 중기 기온 예보 응답 (실제 API 응답에 맞춤)
     * 실제 응답: "#START7777 # REG_ID TM_FC TM_EF ... 11B10101 202507020600 202507060000 A01 109 2 25 31 1 1 1 1 #7777END"
     */
    @Builder
    public record MediumTermTemperatureResponse(
            String regId,
            LocalDate tmfc,
            List<TemperatureInfo> temperatureInfos
    ) {
        public static MediumTermTemperatureResponse fromTextResponse(String textResponse) {
            if (textResponse == null || textResponse.trim().isEmpty()) {
                throw new RuntimeException("중기 기온 예보 응답이 비어있습니다.");
            }

            try {
                String dataSection = extractDataSection(textResponse);
                String[] lines = dataSection.split("\n");

                List<TemperatureInfo> temperatureInfos = new ArrayList<>();
                String regId = null;
                LocalDate tmfc = null;

                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    String[] parts = line.split("\\s+");
                    if (parts.length >= 7) {
                        if (regId == null) {
                            regId = parts[0];  // REG_ID
                            String tmfcStr = parts[1];
                            // yyyyMMddHHmm 형태에서 날짜만 추출
                            if (tmfcStr.length() >= 8) {
                                tmfc = LocalDate.parse(tmfcStr.substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd"));
                            }
                        }

                        String tmefStr = parts[2];
                        if (tmefStr.length() >= 8) {
                            LocalDate tmef = LocalDate.parse(tmefStr.substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd"));
                            Double minTemp = Double.parseDouble(parts[5]);  // MIN
                            Double maxTemp = Double.parseDouble(parts[6]);  // MAX

                            TemperatureInfo tempInfo = TemperatureInfo.builder()
                                    .tmef(tmef)
                                    .taMin(minTemp)
                                    .taMax(maxTemp)
                                    .build();
                            temperatureInfos.add(tempInfo);
                        }
                    }
                }

                return MediumTermTemperatureResponse.builder()
                        .regId(regId)
                        .tmfc(tmfc)
                        .temperatureInfos(temperatureInfos)
                        .build();

            } catch (Exception e) {
                throw new RuntimeException("중기 기온 예보 응답 파싱 실패: " + e.getMessage(), e);
            }
        }
    }

    @Builder
    public record TemperatureInfo(
            LocalDate tmef,
            Double taMin,
            Double taMax
    ) {
    }

    /**
     * 중기 육상 예보 응답 (실제 API 응답에 맞춤)
     * 실제 응답: "#START7777 # REG_ID TM_FC TM_EF ... 11B00000 202507020600 202507060000 A02 109 2 WB04 WB00 없음 "흐림" 40 #7777END"
     */
    @Builder
    public record MediumTermLandForecastResponse(
            String regId,
            LocalDate tmfc,
            List<LandForecastInfo> landForecastInfos
    ) {
        public static MediumTermLandForecastResponse fromTextResponse(String textResponse) {
            if (textResponse == null || textResponse.trim().isEmpty()) {
                throw new RuntimeException("중기 육상 예보 응답이 비어있습니다.");
            }

            try {
                String dataSection = extractDataSection(textResponse);
                String[] lines = dataSection.split("\n");

                List<LandForecastInfo> landForecastInfos = new ArrayList<>();
                String regId = null;
                LocalDate tmfc = null;

                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    String[] parts = line.split("\\s+");
                    if (parts.length >= 11) {
                        if (regId == null) {
                            regId = parts[0];  // REG_ID
                            String tmfcStr = parts[1];
                            if (tmfcStr.length() >= 8) {
                                tmfc = LocalDate.parse(tmfcStr.substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd"));
                            }
                        }

                        String tmefStr = parts[2];
                        if (tmefStr.length() >= 8) {
                            LocalDate tmef = LocalDate.parse(tmefStr.substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd"));
                            String skyCode = parts[6];  // SKY (WB04, WB03 등)
                            String rnSt = parts[10];    // RN_ST (강수확률)

                            Double precipProb = 0.0;
                            try {
                                precipProb = Double.parseDouble(rnSt);
                            } catch (NumberFormatException e) {
                                precipProb = 0.0;
                            }

                            LandForecastInfo landInfo = LandForecastInfo.builder()
                                    .tmef(tmef)
                                    .wfAm(skyCode)
                                    .wfPm(skyCode)
                                    .rnStAm(precipProb)
                                    .rnStPm(precipProb)
                                    .build();
                            landForecastInfos.add(landInfo);
                        }
                    }
                }

                return MediumTermLandForecastResponse.builder()
                        .regId(regId)
                        .tmfc(tmfc)
                        .landForecastInfos(landForecastInfos)
                        .build();

            } catch (Exception e) {
                throw new RuntimeException("중기 육상 예보 응답 파싱 실패: " + e.getMessage(), e);
            }
        }
    }

    @Builder
    public record LandForecastInfo(
            LocalDate tmef,
            String wfAm,
            String wfPm,
            Double rnStAm,
            Double rnStPm
    ) {
    }

    /**
     * #START7777와 #7777END 사이의 데이터 섹션 추출
     */
    private static String extractDataSection(String textResponse) {
        int startIndex = textResponse.indexOf("#START7777");
        int endIndex = textResponse.indexOf("#7777END");

        if (startIndex == -1 || endIndex == -1) {
            // 마커가 없는 경우 전체 텍스트 반환
            return textResponse;
        }

        return textResponse.substring(startIndex + "#START7777".length(), endIndex).trim();
    }
}
