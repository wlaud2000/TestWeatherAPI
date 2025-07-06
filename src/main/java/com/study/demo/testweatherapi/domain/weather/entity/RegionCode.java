package com.study.demo.testweatherapi.domain.weather.entity;

import com.study.demo.testweatherapi.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "region_code")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class RegionCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // region_code_id

    @Column(name = "land_reg_code", nullable = false, unique = true)
    private String landRegCode;    // 중기 육상 예보용 지역 코드

    @Column(name = "temp_reg_code", nullable = false, unique = true)
    private String tempRegCode;    // 중기 기온 예보용 지역 코드

    @Column(nullable = false)
    private String name;           // 지역코드명 (예: "서울, 인천, 경기도")

    @OneToMany(mappedBy = "regionCode", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Region> regions = new ArrayList<>();

    /**
     * 지역코드 업데이트 메서드
     */
    public void updateRegionCode(String landRegCode, String tempRegCode, String name, String description) {
        this.landRegCode = landRegCode;
        this.tempRegCode = tempRegCode;
        this.name = name;
    }
}
