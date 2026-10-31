package com.seaurl.platform.users.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 用户积分历史表
 * 注意：谁的积分数据变了，uid就代表谁
 */
@Setter
@Getter
public class UserPointsHistory {
    private String id;

    private String uid;

    private String objectId;

    private String objectType;

    // 用户积分增减量
    private Integer changePoints;

    // 真实积分
    private Integer points;

    // 操作：add-url,copy-url,like-url,following-user
    // delete-url,unlike-url,cancel-following-user
    private String action;

    private Date createdDt;//创建时间
    private Date updatedDt;
}
