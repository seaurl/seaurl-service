package com.seaurl.platform.users.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LatestNewsRequest {
    private String userName;

    private Integer pageSize;
    private Integer pageNum;

    // 1：为你推荐，2：正在关注
    private String type;
}
