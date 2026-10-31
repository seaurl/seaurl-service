package com.seaurl.platform.space.response.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Setter
@Getter
// 可以忽略不匹配的字段
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class GithubData {

    private String name;

    private String full_name;

    private String language;

    private String description;

    private Integer stargazers_count;

    private String node_id;

    private String html_url;

    private String homepage;

    private String ssh_url;
    private String svn_url;

    private Integer watchers_count;
    private Integer forks_count;

    private  Integer size;

    private  Integer score;

    private Owner owner;

    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
