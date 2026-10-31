package com.seaurl.platform.common.auth;

import com.seaurl.platform.common.model.UserSession;
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

import java.util.Date;
import java.util.UUID;

@Service
public class UserSessionServiceImpl implements UserSessionService {
    private static final Logger log = LoggerFactory.getLogger(UserSessionServiceImpl.class);

    @Autowired
    MongoTemplate mongoTemplate;

    /**
     * 启动时确保 expireAt 上的 TTL 索引存在。
     * Mongo TTL 后台任务每 ~60 秒扫描并删除过期文档。
     * 容错：索引已存在时 ensureIndex 会抛错，catch + 日志，不阻塞启动。
     */
    @PostConstruct
    public void ensureTtlIndex() {
        try {
            mongoTemplate.indexOps(UserSession.class)
                    .ensureIndex(new Index().on("expireAt", Sort.Direction.ASC).expire(0));
        } catch (Exception e) {
            log.warn("ensureTtlIndex userSession (may already exist): {}", e.getMessage());
        }
    }

    @Override
    public UserSession create(String uid, String accessToken, long ttlSeconds) {
        Date now = new Date();
        UserSession s = new UserSession();
        s.setId(UUID.randomUUID().toString().replace("-", ""));
        s.setUid(uid);
        s.setAccessToken(accessToken);
        s.setExpireAt(new Date(now.getTime() + ttlSeconds * 1000L));
        s.setCreatedDt(now);
        return mongoTemplate.insert(s);
    }

    @Override
    public UserSession queryValid(String tokenId) {
        if (!StringUtils.hasLength(tokenId)) return null;
        UserSession s = mongoTemplate.findById(tokenId, UserSession.class);
        if (s == null) return null;
        // 业务侧主动判断过期，不依赖 Mongo TTL 清理的实时性
        if (s.getExpireAt() == null || s.getExpireAt().before(new Date())) return null;
        return s;
    }

    @Override
    public long remainingSeconds(String tokenId) {
        UserSession s = queryValid(tokenId);
        if (s == null || s.getExpireAt() == null) return -1;
        long ms = s.getExpireAt().getTime() - System.currentTimeMillis();
        return ms > 0 ? ms / 1000 : -1;
    }

    @Override
    public String renew(String oldTokenId, long newTtlSeconds) {
        UserSession old = queryValid(oldTokenId);
        if (old == null) return null;
        // 先创建新的再删除老的，保证过渡期用户始终有有效会话
        UserSession created = create(old.getUid(), old.getAccessToken(), newTtlSeconds);
        delete(oldTokenId);
        return created.getId();
    }

    @Override
    public void delete(String tokenId) {
        if (StringUtils.hasLength(tokenId)) {
            mongoTemplate.remove(Query.query(Criteria.where("_id").is(tokenId)), UserSession.class);
        }
    }
}
