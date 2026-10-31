package com.seaurl.platform.common.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.HashMap;

@Setter
@Getter
public class UserEvent {
    private String id;
    private String uid;
    private String action;
    private String objectId;
    private String objectType;
    private HashMap objectOthers;
    private Integer valid;
    private Date createdDt;
    private Date updatedDt;
}
