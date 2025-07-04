package com.study.demo.testweatherapi.domain.weather.entity;

import com.study.demo.testweatherapi.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "template_keyword")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class TemplateKeyword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // template_keyword_id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weather_template_id", nullable = false)
    private WeatherTemplate weatherTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;
}
