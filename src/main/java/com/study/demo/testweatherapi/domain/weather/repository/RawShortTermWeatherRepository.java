package com.study.demo.testweatherapi.domain.weather.repository;

import com.study.demo.testweatherapi.domain.weather.entity.RawShortTermWeather;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RawShortTermWeatherRepository extends JpaRepository<RawShortTermWeather, Long> {

    /**
     * 특정 지역, 기준시각, 예보시각의 단기 예보 데이터 조회 (Upsert 용)
     */
    Optional<RawShortTermWeather> findByRegionIdAndBaseDateAndBaseTimeAndFcstDateAndFcstTime(
            Long regionId, LocalDate baseDate, String baseTime, LocalDate fcstDate, String fcstTime);

    /**
     * 특정 지역의 특정 날짜 예보 데이터 조회 (분류용)
     */
    @Query("SELECT rstw FROM RawShortTermWeather rstw " +
            "WHERE rstw.region.id = :regionId " +
            "AND rstw.fcstDate = :fcstDate " +
            "ORDER BY rstw.baseDate DESC, rstw.baseTime DESC")
    List<RawShortTermWeather> findLatestByRegionIdAndFcstDate(
            @Param("regionId") Long regionId,
            @Param("fcstDate") LocalDate fcstDate);

    /**
     * 특정 지역의 최신 기준시각 데이터 조회
     */
    @Query("SELECT rstw FROM RawShortTermWeather rstw " +
            "WHERE rstw.region.id = :regionId " +
            "AND rstw.baseDate = :baseDate " +
            "AND rstw.baseTime = :baseTime " +
            "ORDER BY rstw.fcstDate ASC, rstw.fcstTime ASC")
    List<RawShortTermWeather> findByRegionIdAndBaseDateAndBaseTime(
            @Param("regionId") Long regionId,
            @Param("baseDate") LocalDate baseDate,
            @Param("baseTime") String baseTime);

    /**
     * 오래된 단기 예보 데이터 삭제 (cutoffDate 이전)
     * @return 삭제된 레코드 수
     */
    @Modifying
    @Query("DELETE FROM RawShortTermWeather rstw WHERE rstw.baseDate < :cutoffDate")
    int deleteOldData(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * 오래된 단기 예보 데이터 개수 조회 (삭제 대상 확인용)
     * @param cutoffDate 기준 날짜 (이 날짜 이전 데이터가 삭제 대상)
     * @return 삭제 대상 레코드 수
     */
    @Query("SELECT COUNT(rstw) FROM RawShortTermWeather rstw WHERE rstw.baseDate < :cutoffDate")
    long countOldData(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * 오래된 단기 예보 데이터 상세 정보 조회 (통계용)
     * @param cutoffDate 기준 날짜
     * @return [최오래된날짜, 최신날짜, 레코드수]
     */
    @Query("SELECT MIN(rstw.baseDate), MAX(rstw.baseDate), COUNT(rstw) " +
            "FROM RawShortTermWeather rstw WHERE rstw.baseDate < :cutoffDate")
    Object[] getOldDataStatistics(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * 지역별 오래된 데이터 개수 조회 (상세 통계용)
     */
    @Query("SELECT r.name, COUNT(rstw) " +
            "FROM RawShortTermWeather rstw " +
            "JOIN rstw.region r " +
            "WHERE rstw.baseDate < :cutoffDate " +
            "GROUP BY r.id, r.name " +
            "ORDER BY COUNT(rstw) DESC")
    List<Object[]> countOldDataByRegion(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * 특정 날짜 범위의 예보 데이터 조회
     */
    @Query("SELECT rstw FROM RawShortTermWeather rstw " +
            "WHERE rstw.region.id = :regionId " +
            "AND rstw.fcstDate BETWEEN :startDate AND :endDate " +
            "ORDER BY rstw.fcstDate ASC, rstw.fcstTime ASC")
    List<RawShortTermWeather> findByRegionIdAndFcstDateBetween(
            @Param("regionId") Long regionId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 모든 활성 지역의 최신 단기 예보 데이터 조회 (스케줄러용)
     */
    @Query("SELECT rstw FROM RawShortTermWeather rstw " +
            "JOIN FETCH rstw.region r " +
            "WHERE rstw.baseDate >= :recentDate " +
            "ORDER BY r.id, rstw.fcstDate ASC")
    List<RawShortTermWeather> findRecentDataForAllRegions(@Param("recentDate") LocalDate recentDate);
}
