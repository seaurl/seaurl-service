package com.seaurl.platform.common.auth;

import com.seaurl.platform.common.model.UserSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserSessionService 集成测试：连 dev mongo，验证 create/queryValid/expire/renew/delete
 * 以及 TTL 索引是否存在。
 */
@SpringBootTest
@ActiveProfiles("dev")
class UserSessionServiceIT {

    @Autowired
    UserSessionService userSessionService;

    @Autowired
    MongoTemplate mongoTemplate;

    @Test
    void createAndQueryValidWorks() {
        UserSession created = userSessionService.create("uid-it-test", "access-token-fake", 60);
        assertThat(created.getId()).isNotBlank();
        assertThat(created.getAccessToken()).isEqualTo("access-token-fake");
        assertThat(created.getExpireAt()).isAfter(new Date());

        try {
            UserSession queried = userSessionService.queryValid(created.getId());
            assertThat(queried).isNotNull();
            assertThat(queried.getAccessToken()).isEqualTo("access-token-fake");
        } finally {
            userSessionService.delete(created.getId());
        }
    }

    @Test
    void queryValidReturnsNullForExpired() {
        UserSession created = userSessionService.create("uid-it-test", "tok", 60);
        // 手动改成过去时间，模拟过期
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(created.getId())),
                new org.springframework.data.mongodb.core.query.Update().set("expireAt", new Date(System.currentTimeMillis() - 1000)),
                UserSession.class
        );

        try {
            // 业务侧主动判断过期（不依赖 Mongo TTL 清理）
            assertThat(userSessionService.queryValid(created.getId())).isNull();
            assertThat(userSessionService.remainingSeconds(created.getId())).isEqualTo(-1L);
        } finally {
            userSessionService.delete(created.getId());
        }
    }

    @Test
    void queryValidReturnsNullForNonexistent() {
        assertThat(userSessionService.queryValid("nonexistent-token-id-xyz-001")).isNull();
        assertThat(userSessionService.remainingSeconds("nonexistent-token-id-xyz-001")).isEqualTo(-1L);
    }

    @Test
    void renewCreatesNewAndDeletesOld() {
        UserSession old = userSessionService.create("uid-it-test", "renew-token", 60);
        try {
            String newId = userSessionService.renew(old.getId(), 120);
            assertThat(newId).isNotBlank();
            assertThat(newId).isNotEqualTo(old.getId());

            // 老的已删除
            assertThat(userSessionService.queryValid(old.getId())).isNull();
            // 新的有效
            UserSession renewed = userSessionService.queryValid(newId);
            assertThat(renewed).isNotNull();
            assertThat(renewed.getAccessToken()).isEqualTo("renew-token");
            // uid 保持一致
            assertThat(renewed.getUid()).isEqualTo("uid-it-test");

            userSessionService.delete(newId);
        } finally {
            userSessionService.delete(old.getId());
        }
    }

    @Test
    void renewReturnsNullForExpired() {
        UserSession expired = userSessionService.create("uid-it-test", "tok", 60);
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(expired.getId())),
                new org.springframework.data.mongodb.core.query.Update().set("expireAt", new Date(System.currentTimeMillis() - 1000)),
                UserSession.class
        );
        try {
            assertThat(userSessionService.renew(expired.getId(), 60)).isNull();
        } finally {
            userSessionService.delete(expired.getId());
        }
    }

    @Test
    void deleteIsIdempotent() {
        // 删不存在的 tokenId 不报错
        userSessionService.delete("nonexistent-token-id-xyz-002");
        userSessionService.delete(null);
        userSessionService.delete("");
    }

    @Test
    void ttlIndexExists() {
        // 验证 expireAt 字段有 TTL 索引（由 @PostConstruct 创建）
        boolean hasTtl = false;
        for (org.springframework.data.mongodb.core.index.IndexInfo idx : mongoTemplate.indexOps(UserSession.class).getIndexInfo()) {
            if (idx.getIndexFields().stream().anyMatch(f -> f.getKey().equals("expireAt"))) {
                hasTtl = true;
                break;
            }
        }
        assertThat(hasTtl).as("userSession 集合必须有 expireAt 索引").isTrue();
    }

    @Test
    void cleanupTestArtifacts() {
        // 兜底清理本次测试残留的数据，避免污染 dev 库
        List<UserSession> leftover = mongoTemplate.find(
                Query.query(Criteria.where("uid").is("uid-it-test")), UserSession.class);
        for (UserSession s : leftover) {
            mongoTemplate.remove(Query.query(Criteria.where("_id").is(s.getId())), UserSession.class);
        }
        assertThat(leftover.size()).as("测试残留清理后应为 0").isZero();
    }
}
