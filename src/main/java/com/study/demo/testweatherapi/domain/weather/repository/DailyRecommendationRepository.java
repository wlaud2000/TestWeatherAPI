package com.study.demo.testweatherapi.domain.weather.repository;

import com.study.demo.testweatherapi.domain.weather.entity.DailyRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyRecommendationRepository extends JpaRepository<DailyRecommendation, Long> {

    /**
     * 특정 지역, 특정 날짜의 추천 정보 조회
     * WeatherTemplate, Keyword 정보까지 함께 fetch join
     */
    @Query("SELECT dr FROM DailyRecommendation dr " +
            "JOIN FETCH dr.weatherTemplate wt " +
            "JOIN FETCH wt.templateKeywords tk " +
            "JOIN FETCH tk.keyword k " +
            "WHERE dr.region.id = :regionId " +
            "AND dr.forecastDate = :date")
    Optional<DailyRecommendation> findByRegionIdAndDateWithTemplate(
            @Param("regionId") Long regionId,
            @Param("date") LocalDate date);

    /**
     * 특정 지역의 주간 추천 정보 조회 (7일치)
     * 시작 날짜부터 7일간의 데이터 조회
     */
    @Query("SELECT dr FROM DailyRecommendation dr " +
            "JOIN FETCH dr.weatherTemplate wt " +
            "JOIN FETCH wt.templateKeywords tk " +
            "JOIN FETCH tk.keyword k " +
            "WHERE dr.region.id = :regionId " +
            "AND dr.forecastDate >= :startDate " +
            "AND dr.forecastDate < :endDate " +
            "ORDER BY dr.forecastDate ASC")
    List<DailyRecommendation> findWeeklyRecommendations(
            @Param("regionId") Long regionId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 특정 지역의 특정 날짜 범위 추천 정보 조회
     */
    @Query("SELECT dr FROM DailyRecommendation dr " +
            "JOIN FETCH dr.weatherTemplate wt " +
            "WHERE dr.region.id = :regionId " +
            "AND dr.forecastDate BETWEEN :startDate AND :endDate " +
            "ORDER BY dr.forecastDate ASC")
    List<DailyRecommendation> findByRegionIdAndDateRange(
            @Param("regionId") Long regionId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 특정 지역의 가장 최근 추천 정보 조회
     */
    @Query("SELECT dr FROM DailyRecommendation dr " +
            "JOIN FETCH dr.weatherTemplate wt " +
            "WHERE dr.region.id = :regionId " +
            "ORDER BY dr.forecastDate DESC")
    List<DailyRecommendation> findLatestByRegionId(@Param("regionId") Long regionId);

    /**
     * 특정 날짜의 모든 지역 추천 정보 조회
     */
    @Query("SELECT dr FROM DailyRecommendation dr " +
            "JOIN FETCH dr.region r " +
            "JOIN FETCH dr.weatherTemplate wt " +
            "WHERE dr.forecastDate = :date " +
            "ORDER BY r.name ASC")
    List<DailyRecommendation> findAllByDate(@Param("date") LocalDate date);

    /**
     * 특정 지역에 해당 날짜의 추천 정보가 존재하는지 확인
     */
    boolean existsByRegionIdAndForecastDate(Long regionId, LocalDate forecastDate);

    /**
     * 오래된 추천 데이터 삭제용 (스케줄러에서 사용)
     */
    @Query("DELETE FROM DailyRecommendation dr WHERE dr.forecastDate < :cutoffDate")
    void deleteOldRecommendations(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * 특정 지역의 추천 데이터 개수 조회
     */
    @Query("SELECT COUNT(dr) FROM DailyRecommendation dr WHERE dr.region.id = :regionId")
    Long countByRegionId(@Param("regionId") Long regionId);

    /**
     * 날씨 타입별 추천 통계 (관리자용)
     */
    @Query("SELECT wt.weather, COUNT(dr) FROM DailyRecommendation dr " +
            "JOIN dr.weatherTemplate wt " +
            "WHERE dr.forecastDate >= :startDate " +
            "GROUP BY wt.weather")
    List<Object[]> getWeatherTypeStatistics(@Param("startDate") LocalDate startDate);
}
