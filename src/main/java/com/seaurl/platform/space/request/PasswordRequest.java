package com.seaurl.platform.space.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordRequest {
    private String id;
    private String title;
    private String username;
    private String password;
    private String url;
    private String repoUrl;
    private String repoBranch;
    private String notes;
    private String category;
    private String categoryId;
    private String oldCategory;
    private String newCategory;
    private String parentId;
    private String color;
    private String keyword;
}
