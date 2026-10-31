package com.seaurl.platform.space.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class Hotline {

    private String id;

    // 名称，如：招商银行客服电话
    private String name;

    // 说明
    private String note;

    // 热线电话
    private String hotline;

    // 招商的logo
    private String icon;

    // 工作时间：9:00~20:00
    private String workTime;

    // 备注
    private String memo;

    //创建时间
    private Date createdDt;

    //更新时间
    private Date updatedDt;
}
