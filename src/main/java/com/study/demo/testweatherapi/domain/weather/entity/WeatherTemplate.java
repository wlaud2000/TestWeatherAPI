package com.study.demo.testweatherapi.domain.weather.entity;

import com.study.demo.testweatherapi.domain.weather.entity.enums.PrecipCategory;
import com.study.demo.testweatherapi.domain.weather.entity.enums.TempCategory;
import com.study.demo.testweatherapi.domain.weather.entity.enums.WeatherType;
import com.study.demo.testweatherapi.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "weather_template")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class WeatherTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // weather_template_id

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeatherType weather;         // 맑음/흐림/눈

    @Enumerated(EnumType.STRING)
    @Column(name = "temp_category", nullable = false)
    private TempCategory tempCategory;   // 쌀쌀함/선선함/적당함/무더움

    @Enumerated(EnumType.STRING)
    @Column(name = "precip_category", nullable = false)
    private PrecipCategory precipCategory; // 없음/약간 비옴/강우 많음

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private String emoji;

    @OneToMany(mappedBy = "weatherTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TemplateKeyword> templateKeywords = new ArrayList<>();
}
