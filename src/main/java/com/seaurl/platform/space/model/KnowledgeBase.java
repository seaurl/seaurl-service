package com.seaurl.platform.space.model;

import lombok.Data;

import java.util.Date;

@Data
public class KnowledgeBase {
    private String id;

    private String uid;

    private String name;

    private String desc;

    // 选用的模型
    private String embeddingModel;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
