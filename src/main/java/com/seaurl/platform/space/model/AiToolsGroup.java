package com.seaurl.platform.space.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
//@Document(collection = "aiToolsGroup")
public class AiToolsGroup {
//    @Id
    private String id;

    // 分组：如：聊天、图像、音频、视频、语言、计算、写作、办公、设计、编程、指令等等，角色扮演
    private String name;

    private Boolean show;

    private Integer sort;

    //创建时间
    private Date createdDt;
    //更新时间
    private Date updatedDt;
}
