package com.seaurl.platform.users.model;

import java.util.Date;

//参考订阅者模式
public class Notice {
    private String id;

    private String uid;

    // pending
    private String status;
    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
