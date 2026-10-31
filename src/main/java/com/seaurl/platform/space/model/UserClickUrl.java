package com.seaurl.platform.space.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class UserClickUrl {
    private String id;

    private String uid;

    private String urlId;
    // 我的访问次数
    private Integer clickCount;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
