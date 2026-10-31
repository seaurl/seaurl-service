package com.seaurl.platform.space.response.aiSpace;

import lombok.Data;

@Data
public class AiMessageResponse {

    // 是否结束
    private Boolean isEnd = false;


    // 内容
    private String content = "";


    // 思维链内容
    private String reasonContent = "";

}
