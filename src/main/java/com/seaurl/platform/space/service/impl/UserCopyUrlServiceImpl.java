package com.seaurl.platform.space.service.impl;

import com.seaurl.platform.common.model.Url;
import com.seaurl.platform.common.model.UserCopyUrl;
import com.seaurl.platform.space.service.UserCopyUrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
public class UserCopyUrlServiceImpl implements UserCopyUrlService {

    private static Logger log = LoggerFactory.getLogger(UserCopyUrlServiceImpl.class);

    @Autowired
    MongoTemplate mongoTemplate;
    @Override
    public boolean addCopyUrl(UserCopyUrl userCopyUrl) {
        mongoTemplate.insert(userCopyUrl);
        return true;
    }

    @Override
    public List<HashMap> getUrlCopyList(String uid) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("uid").is(uid)),//条件
                // 千万不要加project，因为加了之后MostUrlsRank实体类就返回不了favicon字段，说明只有group才是真正要返回的字段
//                Aggregation.project("domain", "favicon", "count"),
                Aggregation.group("fromUrlId").count().as("count")
                        .first("fromUrlId").as("fromUrlId")
//                Aggregation.sort(Sort.Direction.DESC, "count"),//排序
//                Aggregation.limit(10)//页数
        );
        AggregationResults<HashMap> outputType = mongoTemplate.aggregate(agg, "userCopyUrl", HashMap.class);
        List<HashMap> list = outputType.getMappedResults();
        return list;
    }
}
