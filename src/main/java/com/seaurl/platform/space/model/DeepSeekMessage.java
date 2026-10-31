package com.seaurl.platform.space.model;

import lombok.Data;

@Data
public class DeepSeekMessage {
    private String role; // "system", "user", "assistant"
    private String content;

    public DeepSeekMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
