package com.seaurl.platform.users.request;

import lombok.Data;

@Data
public class PaginationRequest {
    private Integer pageNum;

    private Integer pageSize;
}
