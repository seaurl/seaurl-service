package com.seaurl.platform.space.dto;

import lombok.Data;

import java.util.Date;

@Data
public class UrlIndexDto {
    private String id;

    private String uid;

    private String userName;

    // 分类id
    private String categoryId;

    private String categoryName;

    private String categoryParentId;

    private String categoryParentName;

    //    @Field(analyzer="ik_max_word")
    private String title;

    //    @Field(analyzer="ik_max_word")
    private String url;

    //    @Field(analyzer="ik_max_word")
    private String domain;

    //如果没获取前端显示domain首字母
    private String favicon;

    private String likedUrlCount;

    private String copiedUrlCount;

    private Date createdDt;//创建时间

    private Date updatedDt;//更新时间
}
