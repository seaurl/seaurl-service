package com.seaurl.platform.users.utils;

import com.alibaba.fastjson.JSON;
import com.aliyun.captcha20230305.models.VerifyIntelligentCaptchaRequest;
import com.aliyun.captcha20230305.models.VerifyIntelligentCaptchaResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.seaurl.platform.users.controller.RegisterController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AliyunVerify {
    private static Logger log = LoggerFactory.getLogger(AliyunVerify.class);

    /**
     *
     * @param accessId
     * @param accessSecret
     * @param sceneId
     * @param captchaVerifyParam
     * @return
     */
    public Boolean verify(String accessId, String accessSecret, String sceneId, String captchaVerifyParam) {
        // ====================== 1. 初始化配置 ======================
        Config config = new Config();
        // 设置您的AccessKey ID 和 AccessKey Secret。
        // getEnvProperty只是个示例方法，需要您自己实现AccessKey ID 和 AccessKey Secret安全的获取方式。
        config.accessKeyId = accessId;
        config.accessKeySecret = accessSecret;
        //设置请求地址
        config.endpoint = "captcha.cn-shanghai.aliyuncs.com";
        // 设置连接超时为5000毫秒
        config.connectTimeout = 5000;
        // 设置读超时为5000毫秒
        config.readTimeout = 5000;

        try {
            // ====================== 2. 初始化客户端（实际生产代码中建议复用client） ======================
            com.aliyun.captcha20230305.Client client = new com.aliyun.captcha20230305.Client(config);
            // 创建APi请求
            VerifyIntelligentCaptchaRequest request = new VerifyIntelligentCaptchaRequest();
            // 本次验证的场景ID，建议传入，防止前端被篡改场景
            request.sceneId = sceneId;
            // 前端传来的验证参数 CaptchaVerifyParam
            request.captchaVerifyParam = captchaVerifyParam;
            // ====================== 3. 发起请求） ======================


            VerifyIntelligentCaptchaResponse resp = client.verifyIntelligentCaptcha(request);
            log.info("verifyIntelligentCaptcha resp={}", JSON.toJSONString(resp));
            // 建议使用您系统中的日志组件，打印返回
            // 获取验证码验证结果（请注意判空），将结果返回给前端。出现异常建议认为验证通过，优先保证业务可用，然后尽快排查异常原因。
            return resp.body.result.verifyResult;
        } catch (TeaException error) {
            // 建议使用您系统中的日志组件，打印异常
            // 出现异常建议认为验证通过，优先保证业务可用，然后尽快排查异常原因。
//            Boolean captchaVerifyResult = true;
            log.error("拖动校验服务端失败：{}", error.getMessage());
            return false;
        } catch (Exception _error) {
//            TeaException error = new TeaException(_error.getMessage(), _error);
            // 建议使用您系统中的日志组件，打印异常
            // 出现异常建议认为验证通过，优先保证业务可用，然后尽快排查异常原因。
//            Boolean captchaVerifyResult = true;
            log.error("拖动校验服务端失败：{}", _error.getMessage());
            return false;
        }
    }
}
