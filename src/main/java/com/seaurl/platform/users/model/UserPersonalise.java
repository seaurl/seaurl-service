package com.seaurl.platform.users.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * 用户首页个性化
 */
@Setter
@Getter
public class UserPersonalise {
    private String id;

    private String img;

    private String color;

    // img or color
    private String type;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
