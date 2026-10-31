package com.seaurl.platform.space.model;

import lombok.Data;

import java.util.List;

@Data
public class DeepSeekRequest {
    private String model; // 模型名称，如 "deepseek-chat"
    private List<DeepSeekMessage> messages; // 消息列表
    private boolean stream; // 是否启用流式响应
}
