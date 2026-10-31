package com.seaurl.platform.space.service.impl;

import com.seaurl.platform.space.model.QueryResults2;
import com.seaurl.platform.space.response.github.GithubData;
import com.seaurl.platform.space.response.github.Response;
import com.seaurl.platform.space.service.GithubDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class GithubDataServiceImpl implements GithubDataService {
    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public boolean add(GithubData githubData) {
        mongoTemplate.save(githubData);
        return true;
    }

    @Override
    public void deleteAll() {
        Query query = new Query();
        mongoTemplate.remove(query, GithubData.class);
    }

    /**
     * @param param
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public QueryResults2 queryBy(String param, String language, Integer pageNum, Integer pageSize) {
        Criteria criteria1 = Criteria.where("language").is(language);

        if (StringUtils.hasLength(param)) {
            Pattern pattern = Pattern.compile("^.*" + Pattern.quote(param) + ".*$", Pattern.CASE_INSENSITIVE);
            criteria1.and("name").regex(pattern);
        }

        Query query1 = new Query(criteria1);
        Query query2 = new Query(criteria1);
        query1.with(Sort.by(Sort.Order.desc("stargazers_count")));
        query1.skip((long) pageSize * (pageNum - 1)).limit(pageSize);

        long total = mongoTemplate.count(query2, GithubData.class);
        List<GithubData> results = mongoTemplate.find(query1, GithubData.class);

        QueryResults2<GithubData> queryResults = new QueryResults2<>();
        queryResults.setPageNum(pageNum);
        queryResults.setPageSize(pageSize);
        queryResults.setTotal(total);
        queryResults.setRows(results);

        return queryResults;
    }
}
