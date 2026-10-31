package com.seaurl.platform.common.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class UserFollow {
    private String id;
    private String uid;
    private String followedId;
    private Integer status;
    private Date createdDt;
    private Date updatedDt;
}
