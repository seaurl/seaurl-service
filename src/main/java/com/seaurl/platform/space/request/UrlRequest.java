package com.seaurl.platform.space.request;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UrlRequest {

    private String uid;

    private String urlId;
    // 用户名
    private String userName;

    private String loginUserName;

    // 分类id
    private String categoryId;

    // 搜索内容
    private String search;
    // 网址标题
    private String urlTitle;

    private String description;

    private String keywords;
    // 网址
    private String url;

    // flag=add like copy
    private String flag;

    // 是否收藏
    private Boolean isFavorite;

    //是否特别关注
    private Boolean isAttention;

    private Boolean isTop;

    private String sortType;

    private String isPrivate;

    private Integer pageNum;

    private Integer pageSize;
}
