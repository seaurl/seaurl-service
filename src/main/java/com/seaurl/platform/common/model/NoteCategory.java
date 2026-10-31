package com.seaurl.platform.common.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class NoteCategory {
    private String id;
    private String uid;
    private String pid;
    private String name;
    private String icon;
    private Integer sort;
    private Boolean isTop;
    private Boolean isPublic;
    private Date createdDt;
    private Date updatedDt;
}
