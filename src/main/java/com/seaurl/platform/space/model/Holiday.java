package com.seaurl.platform.space.model;

import lombok.Data;

import java.util.Date;

@Data
public class Holiday {
    // 2020-12-12
    private String date;
    // 清明节
    private String name;
    // 0:休，1：班
    private Integer type;

    // 高速是否免费，0：免费
    private Integer highway;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
