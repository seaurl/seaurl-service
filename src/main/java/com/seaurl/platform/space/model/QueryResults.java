package com.seaurl.platform.space.model;

import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class QueryResults {
    private long total;
    private long pageNum;
    private long pageSize;
    private List<HashMap> rows;
}

