package com.study.demo.testweatherapi.domain.weather.converter;

import com.study.demo.testweatherapi.domain.weather.dto.response.WeatherResDTO;
import com.study.demo.testweatherapi.domain.weather.entity.DailyRecommendation;
import com.study.demo.testweatherapi.domain.weather.entity.Region;
import com.study.demo.testweatherapi.domain.weather.entity.WeatherTemplate;
import com.study.demo.testweatherapi.domain.weather.entity.enums.PrecipCategory;
import com.study.demo.testweatherapi.domain.weather.entity.enums.TempCategory;
import com.study.demo.testweatherapi.domain.weather.entity.enums.WeatherType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WeatherConverter {

    /**
     * DailyRecommendation 엔티티를 WeatherRecommendation DTO로 변환
     */
    public static WeatherResDTO.WeatherRecommendation toWeatherRecommendation(DailyRecommendation recommendation) {
        Region region = recommendation.getRegion();
        WeatherTemplate template = recommendation.getWeatherTemplate();

        return WeatherResDTO.WeatherRecommendation.builder()
                .recommendationId(recommendation.getId())
                .forecastDate(recommendation.getForecastDate())
                .region(toRegionInfo(region))
                .weather(toWeatherInfo(template))
                .recommendation(toRecommendationInfo(template))
                .updatedAt(recommendation.getUpdatedAt())
                .build();
    }

    /**
     * Region 엔티티를 RegionInfo DTO로 변환
     */
    public static WeatherResDTO.RegionInfo toRegionInfo(Region region) {
        return WeatherResDTO.RegionInfo.builder()
                .regionId(region.getId())
                .regionName(region.getName())
                .landRegCode(region.getLandRegCode())
                .tempRegCode(region.getTempRegCode())
                .build();
    }

    /**
     * WeatherTemplate을 WeatherInfo DTO로 변환
     */
    public static WeatherResDTO.WeatherInfo toWeatherInfo(WeatherTemplate template) {
        return WeatherResDTO.WeatherInfo.builder()
                .weatherType(template.getWeather())
                .tempCategory(template.getTempCategory())
                .precipCategory(template.getPrecipCategory())
                .weatherDescription(getWeatherDescription(template.getWeather()))
                .tempDescription(getTempDescription(template.getTempCategory()))
                .precipDescription(getPrecipDescription(template.getPrecipCategory()))
                .build();
    }

    /**
     * WeatherTemplate을 RecommendationInfo DTO로 변환
     */
    public static WeatherResDTO.RecommendationInfo toRecommendationInfo(WeatherTemplate template) {
        List<String> keywords = template.getTemplateKeywords().stream()
                .map(tk -> tk.getKeyword().getName())
                .collect(Collectors.toList());

        return WeatherResDTO.RecommendationInfo.builder()
                .message(template.getMessage())
                .emoji(template.getEmoji())
                .keywords(keywords)
                .build();
    }

    /**
     * DailyRecommendation 리스트를 WeeklyRecommendation DTO로 변환
     */
    public static WeatherResDTO.WeeklyRecommendation toWeeklyRecommendation(
            List<DailyRecommendation> recommendations, Long regionId, String regionName,
            LocalDate startDate, LocalDate endDate) {

        // 추천 데이터를 날짜별로 매핑
        Map<LocalDate, DailyRecommendation> recommendationMap = recommendations.stream()
                .collect(Collectors.toMap(
                        DailyRecommendation::getForecastDate,
                        rec -> rec
                ));

        // 7일간의 데이터 생성 (데이터가 없는 날짜는 hasRecommendation=false)
        List<WeatherResDTO.DailyWeatherRecommendation> dailyRecommendations =
                startDate.datesUntil(endDate.plusDays(1))
                        .map(date -> {
                            DailyRecommendation rec = recommendationMap.get(date);
                            if (rec != null) {
                                return toDailyWeatherRecommendation(rec, true);
                            } else {
                                return createEmptyDailyRecommendation(date);
                            }
                        })
                        .collect(Collectors.toList());

        // 첫 번째 추천 데이터에서 지역 정보 가져오기 (있는 경우)
        WeatherResDTO.RegionInfo regionInfo;
        if (!recommendations.isEmpty()) {
            Region region = recommendations.get(0).getRegion();
            regionInfo = toRegionInfo(region);
        } else {
            // 추천 데이터가 없는 경우 기본 정보만
            regionInfo = WeatherResDTO.RegionInfo.builder()
                    .regionId(regionId)
                    .regionName(regionName)
                    .landRegCode(null)
                    .tempRegCode(null)
                    .build();
        }

        return WeatherResDTO.WeeklyRecommendation.builder()
                .region(regionInfo)
                .startDate(startDate)
                .endDate(endDate)
                .dailyRecommendations(dailyRecommendations)
                .totalDays(dailyRecommendations.size())
                .message(String.format("%s 지역의 %s부터 %s까지 주간 날씨 추천입니다.",
                        regionName, startDate, endDate))
                .build();
    }

    /**
     * DailyRecommendation을 DailyWeatherRecommendation DTO로 변환
     */
    public static WeatherResDTO.DailyWeatherRecommendation toDailyWeatherRecommendation(
            DailyRecommendation recommendation, boolean hasRecommendation) {

        if (!hasRecommendation) {
            return createEmptyDailyRecommendation(recommendation.getForecastDate());
        }

        WeatherTemplate template = recommendation.getWeatherTemplate();
        List<String> keywords = template.getTemplateKeywords().stream()
                .map(tk -> tk.getKeyword().getName())
                .collect(Collectors.toList());

        return WeatherResDTO.DailyWeatherRecommendation.builder()
                .forecastDate(recommendation.getForecastDate())
                .weatherType(template.getWeather())
                .tempCategory(template.getTempCategory())
                .precipCategory(template.getPrecipCategory())
                .message(template.getMessage())
                .emoji(template.getEmoji())
                .keywords(keywords)
                .hasRecommendation(true)
                .build();
    }

    /**
     * 데이터가 없는 날짜용 빈 DailyWeatherRecommendation 생성
     */
    private static WeatherResDTO.DailyWeatherRecommendation createEmptyDailyRecommendation(LocalDate date) {
        return WeatherResDTO.DailyWeatherRecommendation.builder()
                .forecastDate(date)
                .weatherType(null)
                .tempCategory(null)
                .precipCategory(null)
                .message("해당 날짜의 날씨 추천 정보가 없습니다.")
                .emoji("❓")
                .keywords(List.of("정보없음"))
                .hasRecommendation(false)
                .build();
    }

    /**
     * 추천 정보 없음 응답 생성
     */
    public static WeatherResDTO.WeatherRecommendationNotFound toRecommendationNotFound(
            Long regionId, String regionName, LocalDate requestedDate, List<String> suggestions) {
        return WeatherResDTO.WeatherRecommendationNotFound.builder()
                .regionId(regionId)
                .regionName(regionName)
                .requestedDate(requestedDate)
                .message("요청하신 날짜의 날씨 추천 정보를 찾을 수 없습니다.")
                .suggestions(suggestions)
                .build();
    }

    /**
     * DailyRecommendation을 WeatherRecommendationSummary로 변환 (목록용)
     */
    public static WeatherResDTO.WeatherRecommendationSummary toWeatherRecommendationSummary(
            DailyRecommendation recommendation) {
        WeatherTemplate template = recommendation.getWeatherTemplate();
        String shortMessage = template.getMessage().length() > 50
                ? template.getMessage().substring(0, 50) + "..."
                : template.getMessage();

        return WeatherResDTO.WeatherRecommendationSummary.builder()
                .recommendationId(recommendation.getId())
                .forecastDate(recommendation.getForecastDate())
                .regionName(recommendation.getRegion().getName())
                .weatherType(template.getWeather())
                .emoji(template.getEmoji())
                .shortMessage(shortMessage)
                .build();
    }

    // ==== Enum 한글 변환 유틸리티 메서드들 ====

    /**
     * WeatherType을 한글 설명으로 변환
     */
    private static String getWeatherDescription(WeatherType weatherType) {
        return switch (weatherType) {
            case CLEAR -> "맑음";
            case CLOUDY -> "흐림";
            case SNOW -> "눈";
        };
    }

    /**
     * TempCategory를 한글 설명으로 변환
     */
    private static String getTempDescription(TempCategory tempCategory) {
        return switch (tempCategory) {
            case CHILLY -> "쌀쌀함";
            case COOL -> "선선함";
            case MILD -> "적당함";
            case HOT -> "무더움";
        };
    }

    /**
     * PrecipCategory를 한글 설명으로 변환
     */
    private static String getPrecipDescription(PrecipCategory precipCategory) {
        return switch (precipCategory) {
            case NONE -> "없음";
            case LIGHT -> "약간 비옴";
            case HEAVY -> "강우 많음";
        };
    }
}
