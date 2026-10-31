package com.seaurl.platform.space.model;

import lombok.Data;

import java.util.Date;

@Data
public class FileTransferConfig {
    private String id;

    private String uid;

    // 文件传输助手文件保存的文件夹id
    private String pid;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
