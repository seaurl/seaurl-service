package com.seaurl.platform.space.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.aliyun.alimt20181012.models.GetDetectLanguageRequest;
import com.aliyun.alimt20181012.models.GetDetectLanguageResponse;
import com.aliyun.alimt20181012.models.TranslateGeneralResponse;
import com.aliyun.tea.TeaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.seaurl.platform.common.config.AliyunConfig;
import com.seaurl.platform.space.service.TranslatorService;
import com.seaurl.platform.space.utils.TranslatorHelp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class TranslatorServiceImpl implements TranslatorService {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    AliyunConfig aliyunConfig;

    public com.aliyun.alimt20181012.Client createClient() throws Exception {
        String accessKeyId = aliyunConfig.getAccessKeyId();
        String accessKeySecret = aliyunConfig.getAccessKeySecret();
        // 工程代码泄露可能会导致 AccessKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考。
        // 建议使用更安全的 STS 方式，更多鉴权访问方式请参见：https://help.aliyun.com/document_detail/378657.html。
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                // 必填，请确保代码运行环境设置了环境变量 ALIBABA_CLOUD_ACCESS_KEY_ID。
                .setAccessKeyId(accessKeyId)
                // 必填，请确保代码运行环境设置了环境变量 ALIBABA_CLOUD_ACCESS_KEY_SECRET。
                .setAccessKeySecret(accessKeySecret);
        // Endpoint 请参考 https://api.aliyun.com/product/alimt
        config.endpoint = "mt.cn-hangzhou.aliyuncs.com";
        return new com.aliyun.alimt20181012.Client(config);
    }

    @Override
    public String startTranslation(String sourceLanguage, String targetLanguage, String sourceTxt) {
        try {
            com.aliyun.alimt20181012.Client client = createClient();
            com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
            com.aliyun.alimt20181012.models.TranslateGeneralRequest translateGeneralRequest = new com.aliyun.alimt20181012.models.TranslateGeneralRequest()
                    .setFormatType("text")
                    .setSourceLanguage(sourceLanguage)
                    .setTargetLanguage(targetLanguage)
                    .setSourceText(sourceTxt);

            TranslateGeneralResponse response = client.translateGeneralWithOptions(translateGeneralRequest, runtime);
            log.info("startTranslation ={}", JSONArray.toJSON(response));
            return JSONArray.toJSONString(response);
        } catch (Exception exception) {
            log.info("startTranslation={}", exception.getMessage());
//            throw new RuntimeException("转换JSON报错：" + exception.getMessage());
            return "";
        }
    }

    @Override
    public String detectLanguage(String sourceTxt) {
        try {
            com.aliyun.alimt20181012.Client client = createClient();
            com.aliyun.alimt20181012.models.GetDetectLanguageRequest getDetectLanguageRequest = new com.aliyun.alimt20181012.models.GetDetectLanguageRequest().setSourceText(sourceTxt);
            com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
            // 复制代码运行请自行打印 API 的返回值
            GetDetectLanguageResponse response = client.getDetectLanguageWithOptions(getDetectLanguageRequest, runtime);
            log.info("detectLanguage ={}", JSONArray.toJSON(response));
            return JSONArray.toJSONString(response);
        } catch (Exception exception) {
            log.info("TeaException={}", exception.getMessage());
            return "";
        }
    }

}
