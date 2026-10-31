package com.seaurl.platform.space.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class OnlineTools {
    private String id;

    private String uid;

    private String groupId;

    private String name;

    private String url;

    private Integer sort;

    private String icon;

    // system:系统的，user:用户的
    private String type;

    private String desc;

    // 首字符
    private String character;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
