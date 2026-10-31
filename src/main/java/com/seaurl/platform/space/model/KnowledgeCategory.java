package com.seaurl.platform.space.model;

import lombok.Data;

import java.util.Date;

@Data
public class KnowledgeCategory {
    private String id;

    private String name;

    private String desc;

    private Integer sort;

    private Boolean isDelete;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
