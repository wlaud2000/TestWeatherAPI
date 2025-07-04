package com.study.demo.testweatherapi.domain.weather.entity;

import com.study.demo.testweatherapi.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "region")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class Region extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;           // region_id

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "grid_x", nullable = false, precision = 5, scale = 2)
    private BigDecimal gridX;

    @Column(name = "grid_y", nullable = false, precision = 5, scale = 2)
    private BigDecimal gridY;

    @Column(name = "land_reg_code", nullable = false)
    private String landRegCode;    // 중기 육상 예보용 지역 코드

    @Column(name = "temp_reg_code", nullable = false)
    private String tempRegCode;    // 중기 기온 예보용 지역 코드

    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RawShortTermWeather> shortTermWeathers = new ArrayList<>();

    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RawMediumTermWeather> mediumTermWeathers = new ArrayList<>();

    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DailyRecommendation> dailyRecommendations = new ArrayList<>();
}
