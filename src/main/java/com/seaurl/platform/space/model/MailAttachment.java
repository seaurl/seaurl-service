package com.seaurl.platform.space.model;

import lombok.Data;

@Data
public class MailAttachment {
    private String fileId;

    private String fileName;
    private String contentType;
    private String url;
    private Long size;
    // 无参构造函数
    public MailAttachment() {
    }
    public MailAttachment(String fileId, String fileName, String contentType, String url, Long size) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.url = url;
        this.size = size;
    }

    public MailAttachment(String fileId, String fileName, String contentType) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.contentType = contentType;
    }

    public MailAttachment(String fileId, String fileName, String contentType, Long size) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
    }
}
