package com.seaurl.platform.space.request;

import lombok.Data;

@Data
public class UserPointsRequest {
    private String userName;

    private String uid;

    private String objectId;

    private String objectType;

    private Integer points;

    // 操作：add-url,copy-url,like-url,following-user
    // delete-url,unlike-url,cancel-following-user
    private String action;

}
