package com.seaurl.platform.space.model;

import lombok.Data;

import java.util.Date;

@Data
public class ImageAi {
    private String id;

    private String uid;

    // aitool 1:n ImageAi
    private String aiToolId;

    // 没有废弃，是有用的，查询，保存
    private String imageAiId;

    private String name;

    private String aiModel;

    private String imageSrc;

    private String imagePath;

    private String uploadImageSrc;

    private String prompt;

    // 文件大小
    private Long size;

    // 长宽比
    private String ratio;

    private Integer count;

    // 2正在生成，1:成功，0:失败(生成失败会删除，可能永远也没这个值)
    private String status;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
