package com.seaurl.platform.common.cache;

import com.seaurl.platform.common.model.TempCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TempCodeService 集成测试：连 dev mongo，验证 set/get/hmset/hmget/hasKey/del
 * 以及覆盖语义、过期判断、TTL 索引。
 */
@SpringBootTest
@ActiveProfiles("dev")
class TempCodeServiceIT {

    @Autowired
    TempCodeService tempCodeService;

    @Autowired
    MongoTemplate mongoTemplate;

    @Test
    void setGetHasKeyDelWorks() {
        String key = "it-test:string:key-001";
        try {
            assertThat(tempCodeService.hasKey(key)).isFalse();

            tempCodeService.set(key, "hello@example.com", 60);
            assertThat(tempCodeService.hasKey(key)).isTrue();
            assertThat(tempCodeService.get(key)).isEqualTo("hello@example.com");

            tempCodeService.del(key);
            assertThat(tempCodeService.hasKey(key)).isFalse();
            assertThat(tempCodeService.get(key)).isNull();
        } finally {
            tempCodeService.del(key);
        }
    }

    @Test
    void hmsetHmgetWorks() {
        String key = "it-test:hash:key-001";
        try {
            Map<String, String> hash = new HashMap<>();
            hash.put("username", "alice");
            hash.put("email", "alice@example.com");
            tempCodeService.hmset(key, hash, 60);

            Map<String, String> got = tempCodeService.hmget(key);
            assertThat(got.get("username")).isEqualTo("alice");
            assertThat(got.get("email")).isEqualTo("alice@example.com");
            assertThat(tempCodeService.hasKey(key)).isTrue();
        } finally {
            tempCodeService.del(key);
        }
    }

    @Test
    void hmgetReturnsEmptyMapForNonexistent() {
        Map<String, String> got = tempCodeService.hmget("it-test:nonexistent-xyz-001");
        assertThat(got).isEmpty();
    }

    @Test
    void setOverwritesExistingValue() {
        String key = "it-test:overwrite-001";
        try {
            tempCodeService.set(key, "first", 60);
            tempCodeService.set(key, "second", 60);
            assertThat(tempCodeService.get(key)).isEqualTo("second");
        } finally {
            tempCodeService.del(key);
        }
    }

    @Test
    void expiredReturnsFalseAndNull() {
        String key = "it-test:expired-001";
        try {
            tempCodeService.set(key, "stale", 60);
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(key)),
                    new Update().set("expireAt", new Date(System.currentTimeMillis() - 1000)),
                    TempCode.class
            );
            assertThat(tempCodeService.hasKey(key)).isFalse();
            assertThat(tempCodeService.get(key)).isNull();
            assertThat(tempCodeService.hmget(key)).isEmpty();
        } finally {
            tempCodeService.del(key);
        }
    }

    @Test
    void nonexistentReturnsFalseAndNull() {
        assertThat(tempCodeService.hasKey("it-test:nonexistent-xyz-002")).isFalse();
        assertThat(tempCodeService.get("it-test:nonexistent-xyz-002")).isNull();
    }

    @Test
    void delIsIdempotent() {
        tempCodeService.del("it-test:nonexistent-xyz-003");
        tempCodeService.del(null);
        tempCodeService.del("");
    }

    @Test
    void ttlIndexExists() {
        boolean hasTtl = false;
        for (org.springframework.data.mongodb.core.index.IndexInfo idx : mongoTemplate.indexOps(TempCode.class).getIndexInfo()) {
            if (idx.getIndexFields().stream().anyMatch(f -> f.getKey().equals("expireAt"))) {
                hasTtl = true;
                break;
            }
        }
        assertThat(hasTtl).as("tempCode 集合必须有 expireAt 索引").isTrue();
    }

    @Test
    void cleanupTestArtifacts() {
        List<TempCode> leftover = mongoTemplate.find(
                Query.query(Criteria.where("_id").regex("^it-test:")), TempCode.class);
        for (TempCode t : leftover) {
            mongoTemplate.remove(Query.query(Criteria.where("_id").is(t.getId())), TempCode.class);
        }
        assertThat(leftover.isEmpty()).as("测试残留清理后应为 0").isTrue();
    }
}
