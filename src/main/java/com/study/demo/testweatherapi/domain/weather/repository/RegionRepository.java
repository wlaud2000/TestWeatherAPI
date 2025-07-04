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
     * 지역코드로 조회 (중기예보 API용)
     */
    Optional<Region> findByRegCode(String regCode);

    /**
     * 지역명 중복 체크
     */
    boolean existsByName(String name);

    /**
     * 지역코드 중복 체크
     */
    boolean existsByRegCode(String regCode);

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
     * 활성 상태인 모든 지역 조회 (스케줄러용)
     */
    @Query("SELECT r FROM Region r ORDER BY r.name ASC")
    List<Region> findAllActiveRegions();

    /**
     * 지역 이름 검색 (Like 검색)
     */
    @Query("SELECT r FROM Region r WHERE r.name LIKE %:keyword% ORDER BY r.name ASC")
    List<Region> searchByNameContaining(@Param("keyword") String keyword);
}
