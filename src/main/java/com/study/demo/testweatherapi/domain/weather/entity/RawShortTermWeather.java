package com.study.demo.testweatherapi.domain.weather.entity;

import com.study.demo.testweatherapi.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "raw_short_term_weather")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class RawShortTermWeather extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // raw_short_term_weather_id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    @Column(name = "base_time", nullable = false)
    private String baseTime;

    @Column(name = "fcst_date", nullable = false)
    private LocalDate fcstDate;

    @Column(name = "fcst_time", nullable = false)
    private String fcstTime;

    @Column(nullable = false)
    private Double tmp;   // ℃

    @Column(nullable = false)
    private String sky;   // 맑음, 구름 조금…

    @Column(nullable = false)
    private Double pop;   // %

    @Column(nullable = false)
    private String pty;   // 없음, 비, 눈, 비/눈…

    @Column(nullable = false)
    private Double pcp;   // mm
}
