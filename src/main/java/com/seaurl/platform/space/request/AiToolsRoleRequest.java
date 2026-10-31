package com.seaurl.platform.space.request;

import lombok.Data;

@Data
public class AiToolsRoleRequest {
    private String id;

    private String uid;

    // 所属分组id
    private String groupId;

    private String name;

    // 描述
    private String desc;

    // 类型角色：通义千问、文心一言、Ai老师、Gemini、Llama、通义万相、计算器、翻译、生成PPT、文档阅读助理
    private String type;

    // 真实ai模型
    private String model;

    // 调用的url，如豆包下的deepseek就需要这个字段
    private String invoke;

    // 豆包传属 字段
    private String endpoint;

    // 源自，如豆包是火山，所以"origin": "volcengine"
    private String origin;

    // chat image
//    private String model;
    private String avatar;

    // 背景颜色
    private String background;

    // 指令：如，我是一名编剧，有什么问题尽管问
    // 注意只有AI需要，普通工具不需要，比如：计算器
    private String command;

    private Boolean show = true;

    private Integer sort;
}
