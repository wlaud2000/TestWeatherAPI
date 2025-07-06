package com.study.demo.testweatherapi.domain.weather.repository;

import com.study.demo.testweatherapi.domain.weather.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {

    /**
     * 지역명으로 조회
     */
    Optional<Region> findByName(String name);

    /**
     * 지역명 중복 체크
     */
    boolean existsByName(String name);

    /**
     * 격자 좌표로 조회 (단기예보 API용)
     */
    Optional<Region> findByGridXAndGridY(BigDecimal gridX, BigDecimal gridY);

    /**
     * 위경도 범위로 조회 (유사한 지역 체크용)
     */
    @Query("SELECT r FROM Region r WHERE " +
            "ABS(r.latitude - :latitude) < 0.001 AND " +
            "ABS(r.longitude - :longitude) < 0.001")
    List<Region> findByNearCoordinates(@Param("latitude") BigDecimal latitude,
                                       @Param("longitude") BigDecimal longitude);

    /**
     * 활성 상태인 모든 지역 조회 (RegionCode 함께 fetch)
     */
    @Query("SELECT r FROM Region r " +
            "JOIN FETCH r.regionCode " +
            "ORDER BY r.name ASC")
    List<Region> findAllActiveRegions();

    /**
     * 지역 이름 검색 (Like 검색, RegionCode 함께 fetch)
     */
    @Query("SELECT r FROM Region r " +
            "JOIN FETCH r.regionCode " +
            "WHERE r.name LIKE %:keyword% " +
            "ORDER BY r.name ASC")
    List<Region> searchByNameContaining(@Param("keyword") String keyword);

    /**
     * 특정 지역코드를 사용하는 모든 지역 조회
     */
    @Query("SELECT r FROM Region r " +
            "JOIN FETCH r.regionCode " +
            "WHERE r.regionCode.id = :regionCodeId " +
            "ORDER BY r.name ASC")
    List<Region> findByRegionCodeId(@Param("regionCodeId") Long regionCodeId);

    /**
     * 중기 육상 예보 지역코드로 모든 지역 조회
     */
    @Query("SELECT r FROM Region r " +
            "JOIN FETCH r.regionCode rc " +
            "WHERE rc.landRegCode = :landRegCode " +
            "ORDER BY r.name ASC")
    List<Region> findByLandRegCode(@Param("landRegCode") String landRegCode);

    /**
     * 중기 기온 예보 지역코드로 모든 지역 조회
     */
    @Query("SELECT r FROM Region r " +
            "JOIN FETCH r.regionCode rc " +
            "WHERE rc.tempRegCode = :tempRegCode " +
            "ORDER BY r.name ASC")
    List<Region> findByTempRegCode(@Param("tempRegCode") String tempRegCode);

    /**
     * 중기 육상 예보용 모든 지역 조회 (중복 제거)
     */
    @Query("SELECT DISTINCT r FROM Region r " +
            "JOIN FETCH r.regionCode rc " +
            "WHERE rc.landRegCode IS NOT NULL " +
            "ORDER BY r.name ASC")
    List<Region> findAllForLandForecast();

    /**
     * 중기 기온 예보용 모든 지역 조회 (중복 제거)
     */
    @Query("SELECT DISTINCT r FROM Region r " +
            "JOIN FETCH r.regionCode rc " +
            "WHERE rc.tempRegCode IS NOT NULL " +
            "ORDER BY r.name ASC")
    List<Region> findAllForTempForecast();

    /**
     * ID로 조회 (RegionCode 함께 fetch)
     */
    @Query("SELECT r FROM Region r " +
            "JOIN FETCH r.regionCode " +
            "WHERE r.id = :id")
    Optional<Region> findByIdWithRegionCode(@Param("id") Long id);

    /**
     * 여러 ID로 조회 (RegionCode 함께 fetch)
     */
    @Query("SELECT r FROM Region r " +
            "JOIN FETCH r.regionCode " +
            "WHERE r.id IN :ids " +
            "ORDER BY r.name ASC")
    List<Region> findByIdsWithRegionCode(@Param("ids") List<Long> ids);
}
