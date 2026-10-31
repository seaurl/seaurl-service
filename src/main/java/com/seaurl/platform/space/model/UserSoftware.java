package com.seaurl.platform.space.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class UserSoftware {
    private String id;

    private String uid;

    private String softwareId;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
