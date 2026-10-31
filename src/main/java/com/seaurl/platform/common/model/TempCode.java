package com.seaurl.platform.common.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;

/**
 * 临时验证码 / 防重 key 存储。原 Redis String + Hash 两种形态统一到单集合。
 * id 直接复用业务 key（UUID code 或 "username:resend:email"）。
 * value 与 hash 互斥：String 模式用 value，Hash 模式用 hash。
 */
@Document(collection = "tempCode")
@Getter
@Setter
public class TempCode {
    @Id
    private String id;
    private String value;
    private Map<String, String> hash;
    private Date expireAt;
    private Date createdDt;
}
