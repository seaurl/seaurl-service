package com.seaurl.platform.space.response.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
// 可以忽略不匹配的字段
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class Owner {
    private String id;

    // github原始值
    private String avatar_url;

    // 上传到阿里云的图片
    private String avatar;

    private String html_url;
}
