package com.seaurl.platform.common.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class Note {
    private String id;
    private String uid;
    private String categoryId;
    private String title;
    private String content;
    private Boolean isPrivate;
    private String isPublic;
    private Integer sort;
    private Boolean isFavorite;
    private Boolean isDelete;
    private Date createdDt;
    private Date updatedDt;
}
