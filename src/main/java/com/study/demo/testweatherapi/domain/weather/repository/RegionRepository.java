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
     * 지역명은 유니크하므로 중복 체크 및 조회에 사용
     */
    Optional<Region> findByName(String name);

    /**
     * 지역명 존재 여부 확인
     * 지역 등록 시 중복 체크용
     */
    boolean existsByName(String name);

    /**
     * 지역코드로 조회
     * 중기예보 API 호출 시 사용
     */
    Optional<Region> findByRegCode(String regCode);

    /**
     * 지역코드 존재 여부 확인
     * 지역 등록 시 중복 체크용
     */
    boolean existsByRegCode(String regCode);

    /**
     * 격자 좌표로 조회
     * 단기예보 API 호출 시 사용
     */
    Optional<Region> findByGridXAndGridY(BigDecimal gridX, BigDecimal gridY);

    /**
     * 위도/경도로 가장 가까운 지역 조회
     * GPS 좌표 기반 서비스용
     */
    @Query("SELECT r FROM Region r WHERE " +
            "ABS(r.latitude - :latitude) + ABS(r.longitude - :longitude) = " +
            "(SELECT MIN(ABS(r2.latitude - :latitude) + ABS(r2.longitude - :longitude)) FROM Region r2)")
    Optional<Region> findNearestRegion(@Param("latitude") BigDecimal latitude,
                                       @Param("longitude") BigDecimal longitude);

    /**
     * 특정 반경 내의 지역들 조회
     * 위도/경도 기반으로 일정 거리 내 지역 검색
     */
    @Query("SELECT r FROM Region r WHERE " +
            "ABS(r.latitude - :latitude) <= :latRange AND " +
            "ABS(r.longitude - :longitude) <= :lngRange")
    List<Region> findRegionsWithinRange(@Param("latitude") BigDecimal latitude,
                                        @Param("longitude") BigDecimal longitude,
                                        @Param("latRange") BigDecimal latRange,
                                        @Param("lngRange") BigDecimal lngRange);

    /**
     * 모든 지역 조회 (이름 순 정렬)
     * 지역 목록 API용
     */
    List<Region> findAllByOrderByName();

    /**
     * 활성 지역 조회 (삭제되지 않은 지역)
     * 실제 서비스에서는 soft delete 적용 시 사용
     */
    @Query("SELECT r FROM Region r WHERE r.id IS NOT NULL ORDER BY r.name")
    List<Region> findActiveRegions();

    /**
     * 지역 검색 (이름 부분 일치)
     * 사용자 편의 기능 - 지역명 자동완성 등
     */
    List<Region> findByNameContainingIgnoreCaseOrderByName(String keyword);

    /**
     * 좌표 범위로 지역 조회
     * 특정 영역 내 지역 조회
     */
    @Query("SELECT r FROM Region r WHERE " +
            "r.latitude BETWEEN :minLat AND :maxLat AND " +
            "r.longitude BETWEEN :minLng AND :maxLng " +
            "ORDER BY r.name")
    List<Region> findRegionsInBounds(@Param("minLat") BigDecimal minLat,
                                     @Param("maxLat") BigDecimal maxLat,
                                     @Param("minLng") BigDecimal minLng,
                                     @Param("maxLng") BigDecimal maxLng);
}
