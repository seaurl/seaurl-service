package com.seaurl.platform.space.request;

import com.seaurl.platform.space.model.MailAttachment;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Data
public class MailMessageRequest {
    private String id;

    private String email;

    private String folderName;

    private Long mid;

    // 文件id
    private String fileId;

    // 文件名
    private String name;

    private String fileType;

    private String nickName;

    private Integer pageSize;
    private Integer pageNum;

    private Boolean isUnread;

    private String messageId;
}
