package com.study.demo.testweatherapi.domain.weather.repository;

import com.study.demo.testweatherapi.domain.weather.entity.WeatherTemplate;
import com.study.demo.testweatherapi.domain.weather.entity.enums.PrecipCategory;
import com.study.demo.testweatherapi.domain.weather.entity.enums.TempCategory;
import com.study.demo.testweatherapi.domain.weather.entity.enums.WeatherType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WeatherTemplateRepository extends JpaRepository<WeatherTemplate, Long> {

    /**
     * 날씨 조건으로 템플릿 조회 (분류 후 매칭용)
     */
    Optional<WeatherTemplate> findByWeatherAndTempCategoryAndPrecipCategory(
            WeatherType weather, TempCategory tempCategory, PrecipCategory precipCategory);

    /**
     * 모든 템플릿과 키워드 함께 조회
     */
    @Query("SELECT wt FROM WeatherTemplate wt " +
            "JOIN FETCH wt.templateKeywords tk " +
            "JOIN FETCH tk.keyword k")
    List<WeatherTemplate> findAllWithKeywords();

    /**
     * 특정 날씨 타입의 템플릿 조회
     */
    @Query("SELECT wt FROM WeatherTemplate wt " +
            "WHERE wt.weather = :weatherType")
    List<WeatherTemplate> findByWeatherType(@Param("weatherType") WeatherType weatherType);

    /**
     * 템플릿 중복 체크
     */
    boolean existsByWeatherAndTempCategoryAndPrecipCategory(
            WeatherType weather, TempCategory tempCategory, PrecipCategory precipCategory);
}
