package com.seaurl.platform.users.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

// 用户积分表
@Setter
@Getter
public class UserPoints {
    private String id;

    private String uid;

    // 用户积分
   private Integer points;

    private Date createdDt;//创建时间
    private Date updatedDt;
}
