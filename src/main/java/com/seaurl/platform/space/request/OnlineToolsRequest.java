package com.seaurl.platform.space.request;

import lombok.Data;

@Data
public class OnlineToolsRequest {

    private String groupId;

    private String name;

    private String url;

    private Integer sort;

    private String icon;

    private String type;

    private String desc;

    // 首字符
    private String character;
}
