package com.seaurl.platform.space.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class Software {
    private String id;

    private String groupId;

    private String name;

    private String url;

    private String download;

    private String icon;

    private String desc;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
