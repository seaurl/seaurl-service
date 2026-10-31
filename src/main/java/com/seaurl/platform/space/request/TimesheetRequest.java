package com.seaurl.platform.space.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimesheetRequest {
    private String taskId;
    private String name;
    private Integer year;
    private Integer month;
    private Integer day;
    private Double hours;
    private Double totalHours;
    private Integer status;
    private Double outputValue;
    private String category;
    private String keyword;
    private Boolean global;
}
