package com.seaurl.platform.space.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class CommonUrls {
    private String id;

    private String name;

    private String url;

    private String icon;

    // 说明
    private String description;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
