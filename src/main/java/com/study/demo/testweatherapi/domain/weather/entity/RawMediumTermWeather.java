package com.study.demo.testweatherapi.domain.weather.entity;

import com.study.demo.testweatherapi.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "raw_medium_term_weather")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class RawMediumTermWeather extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // raw_medium_term_weather_id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(nullable = false)
    private LocalDate tmfc;    // 발표시각

    @Column(nullable = false)
    private LocalDate tmef;    // 발효시각

    @Column(nullable = false)
    private String sky;        // 맑음, 구름 조금…

    @Column(nullable = false)
    private Double pop;        // %

    @Column(name = "min_tmp", nullable = false)
    private Double minTmp;     // 최저기온 ℃

    @Column(name = "max_tmp", nullable = false)
    private Double maxTmp;     // 최고기온 ℃
}
