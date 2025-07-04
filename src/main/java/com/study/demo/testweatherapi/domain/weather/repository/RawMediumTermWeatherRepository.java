package com.study.demo.testweatherapi.domain.weather.repository;

import com.study.demo.testweatherapi.domain.weather.entity.RawMediumTermWeather;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RawMediumTermWeatherRepository extends JpaRepository<RawMediumTermWeather, Long> {

    /**
     * 특정 지역, 발표시각, 발효시각의 중기 예보 데이터 조회 (Upsert 용)
     */
    Optional<RawMediumTermWeather> findByRegionIdAndTmfcAndTmef(
            Long regionId, LocalDate tmfc, LocalDate tmef);

    /**
     * 특정 지역의 특정 날짜 중기 예보 데이터 조회 (분류용)
     */
    @Query("SELECT rmtw FROM RawMediumTermWeather rmtw " +
            "WHERE rmtw.region.id = :regionId " +
            "AND rmtw.tmef = :tmef " +
            "ORDER BY rmtw.tmfc DESC")
    List<RawMediumTermWeather> findLatestByRegionIdAndTmef(
            @Param("regionId") Long regionId,
            @Param("tmef") LocalDate tmef);

    /**
     * 특정 지역의 최신 발표시각 데이터 조회
     */
    @Query("SELECT rmtw FROM RawMediumTermWeather rmtw " +
            "WHERE rmtw.region.id = :regionId " +
            "AND rmtw.tmfc = :tmfc " +
            "ORDER BY rmtw.tmef ASC")
    List<RawMediumTermWeather> findByRegionIdAndTmfc(
            @Param("regionId") Long regionId,
            @Param("tmfc") LocalDate tmfc);

    /**
     * 오래된 중기 예보 데이터 삭제 (7일 이전)
     */
    @Modifying
    @Query("DELETE FROM RawMediumTermWeather rmtw WHERE rmtw.tmfc < :cutoffDate")
    void deleteOldData(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * 특정 날짜 범위의 중기 예보 데이터 조회
     */
    @Query("SELECT rmtw FROM RawMediumTermWeather rmtw " +
            "WHERE rmtw.region.id = :regionId " +
            "AND rmtw.tmef BETWEEN :startDate AND :endDate " +
            "ORDER BY rmtw.tmef ASC")
    List<RawMediumTermWeather> findByRegionIdAndTmefBetween(
            @Param("regionId") Long regionId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 모든 활성 지역의 최신 중기 예보 데이터 조회 (스케줄러용)
     */
    @Query("SELECT rmtw FROM RawMediumTermWeather rmtw " +
            "JOIN FETCH rmtw.region r " +
            "WHERE rmtw.tmfc >= :recentDate " +
            "ORDER BY r.id, rmtw.tmef ASC")
    List<RawMediumTermWeather> findRecentDataForAllRegions(@Param("recentDate") LocalDate recentDate);
}
