package com.seaurl.platform.space.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NoteRequest {
    private String noteId;
    // 用户名
    private String userName;
    // 分类id
    private String categoryId;
    // 网址标题
    private String title;

    private String content;

    private Boolean isPrivate = false;

    // 是否收藏
    private Boolean isFavorite;
}
