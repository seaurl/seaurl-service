package com.seaurl.platform.space.response.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;

@Setter
@Getter
@ToString
// 可以忽略不匹配的字段
@JsonIgnoreProperties(ignoreUnknown = true)
public class Response {
    private Boolean incomplete_results;

    private Integer total_count;


    private ArrayList<GithubData> items;

}
