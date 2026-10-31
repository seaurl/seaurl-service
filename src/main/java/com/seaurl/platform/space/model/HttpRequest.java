package com.seaurl.platform.space.model;

import lombok.Data;

import java.util.Date;

@Data
public class HttpRequest {
    private String id;

    private String uid;

    private String method;

    private String url;

    private String headers;

    private String contentType;

    private String body;

    private Boolean isDelete;


    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
