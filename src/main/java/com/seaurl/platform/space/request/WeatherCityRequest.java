package com.seaurl.platform.space.request;

import lombok.Data;

@Data
public class WeatherCityRequest {
    private String adm1;
    private String adm2;
    private String country;
    private String fxLink;
    private String id;
    private String isDst;
    private String lat;
    private String lon;
    private String name;
    private String rank;
    private String type;
    private String tz;
    private String utcOffset;
}
