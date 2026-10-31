package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.WeatherCity;

import java.util.List;

public interface WeatherCityService {

    Boolean add(WeatherCity weatherCity);

    Boolean top(String id);

    List<WeatherCity> queryAll(String uid);

    Boolean delete(String uid, String id);

    Boolean isExit(String uid, String id);
}
