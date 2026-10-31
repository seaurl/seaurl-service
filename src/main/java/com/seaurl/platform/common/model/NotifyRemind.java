package com.seaurl.platform.common.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.HashMap;

@Setter
@Getter
public class NotifyRemind {
    private String id;
    private String remindId;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String senderAction;
    private String objectId;
    private String object;
    private String objectType;
    private HashMap objectOthers;
    private String recipientId;
    private String message;
    private Boolean isRead;
    private String readDt;
    private Date createdDt;
    private Date updatedDt;
}
