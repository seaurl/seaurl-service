package com.seaurl.platform.common.cache;

import com.seaurl.platform.common.model.TempCode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

@Service
public class TempCodeServiceImpl implements TempCodeService {
    private static final Logger log = LoggerFactory.getLogger(TempCodeServiceImpl.class);

    @Autowired
    MongoTemplate mongoTemplate;

    @PostConstruct
    public void ensureTtlIndex() {
        try {
            mongoTemplate.indexOps(TempCode.class)
                    .ensureIndex(new Index().on("expireAt", Sort.Direction.ASC).expire(0));
        } catch (Exception e) {
            log.warn("ensureTtlIndex tempCode (may already exist): {}", e.getMessage());
        }
    }

    @Override
    public void set(String key, String value, long ttlSeconds) {
        TempCode doc = newDoc(key, ttlSeconds);
        doc.setValue(value);
        mongoTemplate.save(doc);
    }

    @Override
    public String get(String key) {
        TempCode doc = findValid(key);
        return doc == null ? null : doc.getValue();
    }

    @Override
    public void hmset(String key, Map<String, String> hash, long ttlSeconds) {
        TempCode doc = newDoc(key, ttlSeconds);
        doc.setHash(hash);
        mongoTemplate.save(doc);
    }

    @Override
    public Map<String, String> hmget(String key) {
        TempCode doc = findValid(key);
        return doc == null || doc.getHash() == null ? Collections.emptyMap() : doc.getHash();
    }

    @Override
    public boolean hasKey(String key) {
        return findValid(key) != null;
    }

    @Override
    public void del(String key) {
        if (StringUtils.hasLength(key)) {
            mongoTemplate.remove(Query.query(Criteria.where("_id").is(key)), TempCode.class);
        }
    }

    private TempCode findValid(String key) {
        if (!StringUtils.hasLength(key)) return null;
        TempCode doc = mongoTemplate.findById(key, TempCode.class);
        if (doc == null) return null;
        if (doc.getExpireAt() == null || doc.getExpireAt().before(new Date())) return null;
        return doc;
    }

    private static TempCode newDoc(String key, long ttlSeconds) {
        Date now = new Date();
        TempCode doc = new TempCode();
        doc.setId(key);
        doc.setExpireAt(new Date(now.getTime() + ttlSeconds * 1000L));
        doc.setCreatedDt(now);
        return doc;
    }
}
