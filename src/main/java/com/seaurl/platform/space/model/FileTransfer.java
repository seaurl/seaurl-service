package com.seaurl.platform.space.model;

import lombok.Data;

import java.util.Date;

@Data
public class FileTransfer {
    private String id;

    private String uid;

    // 使用的工具id，没有使用usedId是因为，这个更直观，usedId给前端用户看的
    private String aiToolId;

    // type=text的文本内容
    private String content;

    // 类型，text file
    private String type;

    private String fileId;

    private Date actionDate;

    private Boolean isPrivate;

    private Boolean isDelete;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
