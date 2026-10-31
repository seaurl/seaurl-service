package com.seaurl.platform.space.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class Report {
    private String id;

    private String uid;

    private String urlId;

    // 被举报用户id
    private String reportedUid;

    private Integer type;

    private String content;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
