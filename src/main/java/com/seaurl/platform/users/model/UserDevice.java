package com.seaurl.platform.users.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class UserDevice {
    //  id
    private String id;
    private String uid;
    // 每个token对应一个设备
    private String tokenId;
    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
