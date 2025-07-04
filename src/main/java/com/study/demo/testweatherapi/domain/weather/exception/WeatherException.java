package com.study.demo.testweatherapi.domain.weather.exception;

import com.study.demo.testweatherapi.global.apiPayload.exception.CustomException;

public class WeatherException extends CustomException {

    public WeatherException(WeatherErrorCode errorCode) {
        super(errorCode);
    }
}
