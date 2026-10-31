package com.seaurl.platform.space.model;

import lombok.Data;

import java.util.Date;

@Data
public class WeatherCity {

    private String id;
    private String wid;
    private String uid;

    private String adm1;
    private String adm2;
    private String country;
    private String fxLink;
    private String isDst;
    private String lat;
    private String lon;
    private String name;
    private String rank;
    private String type;
    private String tz;
    private String utcOffset;
    private Boolean isDelete;
    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
