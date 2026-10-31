package com.seaurl.platform.space.model;

import lombok.Data;

import java.util.Date;

@Data
public class ChatFile {

    private String name;

    private String content;

    private String type;

    private Long size;

    private String lastModified;

    private Date lastModifiedDate;

    private String webkitRelativePath;
}
