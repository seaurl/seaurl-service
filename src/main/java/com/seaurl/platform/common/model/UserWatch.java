package com.seaurl.platform.common.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class UserWatch {
    private String id;
    private String uid;
    private String watchUid;
    private Date createdDt;
    private Date updatedDt;
}
