package com.seaurl.platform.space.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class ChatAiMessage {
    private String id;

    // 废弃
//    private String chatAiId;

    // 使用的工具id，没有使用usedId是因为，这个更直观，usedId给前端用户看的
    private String aiToolId;

    private String uid;

    // 聊天内容
    private String message;

    private String reasonMessage;

    // user bot system
    private String role;

    // 定义ai的角色 数学老师 chatgpt 通义千问
    private String type;

    private ChatFile file;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
