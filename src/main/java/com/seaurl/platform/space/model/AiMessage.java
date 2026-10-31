package com.seaurl.platform.space.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "ai_message")
public class AiMessage {
    @Id
    private String id;

    private String uid;

    private String conversationId;

    private String role;

    private String content;

    private String title;

    private Date createdDt;

    private Date updatedDt;
}
