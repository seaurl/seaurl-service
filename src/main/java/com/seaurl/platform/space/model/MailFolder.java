package com.seaurl.platform.space.model;

import lombok.Data;

import java.util.Date;

@Data
public class MailFolder {
    private String id;

    private String uid;

    // 提供商id
    private String email;

    // inbox:收到的，sent:发送的，Drafts：草稿的，Trash：已删除的
    private String name;

    // 原始名称：inbox、sent、Drafts、Trash
    private String realName;

    private Integer sort;

    // 邮件数量
    private Integer messageCount;

    private Integer newMessageCount;

    private Integer unreadMessageCount;

    private Integer deletedMessageCount;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
