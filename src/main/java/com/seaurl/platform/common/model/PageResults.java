package com.seaurl.platform.common.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

@Setter
@Getter
public class PageResults {
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private List<HashMap> rows;
}
