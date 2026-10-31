package com.seaurl.platform.users.request;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UrlRequest {
    private String urlId;
    // 用户名
    private String userName;
    // 分类id
    private String categoryId;
    // 网址标题
    private String urlTitle;
    // 网址
    private String url;

    //是否特别关注
    private Boolean isAttention;
}
