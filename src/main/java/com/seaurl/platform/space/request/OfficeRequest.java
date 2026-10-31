package com.seaurl.platform.space.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OfficeRequest {

    // 文档类型：word excel ppt txt
    private String fileType;

    private String fileName;

    // 所属分类
    private String fullPath;

    private String pid;
}
