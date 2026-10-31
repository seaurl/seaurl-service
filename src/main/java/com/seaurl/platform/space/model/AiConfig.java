package com.seaurl.platform.space.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "ai_config")
public class AiConfig {
    @Id
    private String id;

    private String uid;

    private String model;

    private String apiKey;

    private String baseUrl;

    private Date createdDt;

    private Date updatedDt;
}
