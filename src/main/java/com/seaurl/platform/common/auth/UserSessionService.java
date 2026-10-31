package com.seaurl.platform.common.auth;

import com.seaurl.platform.common.model.UserSession;

/**
 * 用户登录会话服务。替代原 Redis Hash 存储。
 */
public interface UserSessionService {

    /** 登录时创建会话，返回新 UserSession（含生成的 tokenId）。 */
    UserSession create(String uid, String accessToken, long ttlSeconds);

    /** 按 tokenId 查询有效会话；不存在或已过期返回 null。 */
    UserSession queryValid(String tokenId);

    /** 按 tokenId 查询剩余有效秒数；不存在或已过期返回 -1。 */
    long remainingSeconds(String tokenId);

    /**
     * 续期：读取老会话 → 创建新会话 → 删除老会话，返回新 tokenId。
     * 老会话不存在或已过期返回 null（业务侧应让用户重新登录）。
     */
    String renew(String oldTokenId, long newTtlSeconds);

    /** 立即删除（登出场景）。tokenId 为空或不存在静默返回。 */
    void delete(String tokenId);
}
