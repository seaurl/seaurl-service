package com.seaurl.platform.space.request;

import lombok.Data;

import java.util.List;

@Data
public class NotifyRemindRequest {
    private String senderId;
    private String senderName;
    private String action;
    private String objectId;
    private String object;
    private String objectType;
    private String recipientId;
    private String message;

    private String userName;
    private Integer pageSize;
    private Integer pageNum;

    private Boolean isRead;

    private List<String> objectIds;

    private List<String> recipientIds;

}
