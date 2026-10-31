package com.seaurl.platform.space.request;

import lombok.Data;

@Data
public class ImageAiRequest {

    String aiModel;

    // 查询内容
    String prompt;

    String ratio;

    // 图片大小
    String size;

    // 图片数量，最多4张
    Integer count;

    String imageAiId;

    String uid;

    private Integer pageSize;

    private Integer pageNum;

    // 第1页第1条数据的创建时间
    private String flagCreatedTime;
}
