package com.seaurl.platform.users.request;

import lombok.Data;

@Data
public class VerifyRequest {
    // 验证的参数
    private String captchaVerifyParam;

    // 场景id
    private String sceneId;
}
