package com.seaurl.platform.space.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class KnowledgeFile {

    private String id;

    private String uid;

    // 文件名
    private String name;

    // 文件类型
    private String type;

    // 文件大小b，单个文件不超过100M
    private Long size;

    private String charCount;

    // 文件存储的OSS地址
    private String url;

    // -1错误，0，禁用，1可用
    private String status;

    // 是否删除文件
    private Boolean isDelete;

    // 分段方式
    private String segmentation;

    // 是否彻底删除文件
//    private Boolean isRealDelete;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
