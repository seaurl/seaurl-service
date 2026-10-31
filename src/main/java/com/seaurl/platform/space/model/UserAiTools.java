package com.seaurl.platform.space.model;

import lombok.Data;

import java.util.Date;

@Data
public class UserAiTools {
    private String id;

    // 使用ID
    private String usedId;

    private String uid;

    private String aiToolId;

    private Boolean isTop = false;

    private Date createdDt;//创建时间

    private Date updatedDt;//更新时间
}
