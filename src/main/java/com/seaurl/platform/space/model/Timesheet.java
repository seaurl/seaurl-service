package com.seaurl.platform.space.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
@Document(collection = "timesheet")
public class Timesheet {
    @Id
    private String id;

    private String uid;

    private String name;

    private Integer year;

    private Integer month;

    private Map<String, Double> hours = new HashMap<>();

    /**
     * 预估总工时（小时）：添加任务时未选日期但填写了工时，该值作为任务的总工时预算
     */
    private Double totalHours;

    /**
     * 任务状态：0=未开始, 1=进行中, 2=已完成, 3=已取消
     */
    private Integer status = 0;

    /**
     * 产值（元）
     */
    private Double outputValue = 0.0;

    /**
     * 每日产值（元）: key=day, value=产值
     */
    private Map<String, Double> outputValues = new HashMap<>();

    /**
     * 任务分类
     */
    private String category = "";

    private Date createdDt;

    private Date updatedDt;
}
