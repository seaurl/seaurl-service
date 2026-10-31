package com.seaurl.platform.users.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class UserFileSpace {
    private String id;

    private String uid;

    // 免费1G? 500M？
    private Integer freeSize;

    //创建时间
    private Date createdDt;

    //更新时间
    private Date updatedDt;
}
