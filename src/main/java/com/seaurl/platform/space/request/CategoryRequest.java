package com.seaurl.platform.space.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CategoryRequest {
    private String id;
    private String cid;
    private String pid;
    private String name;
    private String userName;

    private Boolean isTop;

    private Integer sort;

    private String targetId;
    private String dragType;
}
