package com.seaurl.platform.space.request;

import lombok.Data;

import java.util.Date;

@Data
public class KnowledgeCategoryRequest {
    private String id;

    private String name;

    private String desc;

    private Integer sort;

    private Boolean isDelete;
}
