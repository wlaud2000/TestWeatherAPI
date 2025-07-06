package com.study.demo.testweatherapi.domain.weather.repository;

import com.study.demo.testweatherapi.domain.weather.entity.RegionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegionCodeRepository extends JpaRepository<RegionCode, Long> {

    /**
     * 중기 육상 예보 지역코드로 조회
     */
    Optional<RegionCode> findByLandRegCode(String landRegCode);

    /**
     * 중기 기온 예보 지역코드로 조회
     */
    Optional<RegionCode> findByTempRegCode(String tempRegCode);

    /**
     * 육상 예보 코드와 기온 예보 코드로 조회
     */
    Optional<RegionCode> findByLandRegCodeAndTempRegCode(String landRegCode, String tempRegCode);

    /**
     * 중기 육상 예보 지역코드 중복 체크
     */
    boolean existsByLandRegCode(String landRegCode);

    /**
     * 중기 기온 예보 지역코드 중복 체크
     */
    boolean existsByTempRegCode(String tempRegCode);

    /**
     * 코드명으로 검색
     */
    @Query("SELECT rc FROM RegionCode rc WHERE rc.name LIKE %:keyword% ORDER BY rc.name ASC")
    List<RegionCode> searchByNameContaining(@Param("keyword") String keyword);

    /**
     * 모든 지역코드를 지역수와 함께 조회 (관리자용)
     */
    @Query("SELECT rc, COUNT(r) FROM RegionCode rc " +
            "LEFT JOIN rc.regions r " +
            "GROUP BY rc " +
            "ORDER BY rc.name ASC")
    List<Object[]> findAllWithRegionCount();

    /**
     * 사용되지 않는 지역코드 조회 (지역이 없는 코드)
     */
    @Query("SELECT rc FROM RegionCode rc " +
            "WHERE rc.id NOT IN (SELECT DISTINCT r.regionCode.id FROM Region r)")
    List<RegionCode> findUnusedRegionCodes();

    /**
     * 특정 지역코드를 사용하는 지역 수 조회
     */
    @Query("SELECT COUNT(r) FROM Region r WHERE r.regionCode.id = :regionCodeId")
    long countRegionsByRegionCodeId(@Param("regionCodeId") Long regionCodeId);
}
