package com.seaurl.platform.common.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class Url {
    private String id;
    private String uid;
    private String categoryId;
    private String title;
    private String url;
    private String domain;
    private String host;
    private String protocol;
    private String keywords;
    private String description;
    private String favicon;
    private Integer sort;
    private Boolean isFavorite;
    private Boolean isCopy;
    private Boolean isDelete;
    private String isPrivate;
    private Boolean isTop;
    private Date createdDt;
    private Date updatedDt;

    @JsonSetter("isPrivate")
    public void setIsPrivate(Object value) {
        this.isPrivate = value == null ? null : value.toString();
    }
}
