package com.seaurl.platform.space.request;

import com.seaurl.platform.space.model.ChatFile;
import com.seaurl.platform.common.model.File;
import lombok.Data;

@Data
public class ChatAiRequest {

    private String aiToolId;

    private String message;

    private String reasonMessage;

    private String chatAiId;

    private String type;

    // user bot
    private String role;

    private Integer pageSize;

    private Integer pageNum;

    // 第1页第1条数据的创建时间
    private String flagCreatedTime;

    private ChatFile file;
}
