package com.seaurl.platform.space.model;

import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Data
public class MailMessage {
    private String id;

    private String uid;

    private String email;

    // 邮件的唯一uid
    private Long mid;

    private String folderName;


    private List<HashMap> recipients;

    private List<HashMap> replyTo;

    private List<HashMap> from;

    private String subject;


    private String content;

    // 是否未读
    private Boolean isUnread;

    // 是否是红旗邮件
    private Boolean isFlagged;

    private Date sentDate;

    private Date receivedDate;

    private Integer count;

    // 附件列表
    private List<MailAttachment> attachments;

    private Boolean isDelete;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
