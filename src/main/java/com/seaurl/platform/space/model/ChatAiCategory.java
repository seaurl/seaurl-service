package com.seaurl.platform.space.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class ChatAiCategory {
    private String id;

    private String uid;

    private String chatAiId;

    private String title;

    private Boolean isTop;

    // 说明
    private String description;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
