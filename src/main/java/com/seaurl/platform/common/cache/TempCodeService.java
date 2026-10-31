package com.seaurl.platform.common.cache;

import java.util.Map;

/**
 * 临时验证码 / 防重 key 存储。替代原 Redis 的 String + Hash 用法。
 * 方法名对齐 Redis 语义以最小化调用端改动。
 */
public interface TempCodeService {

    /** String 模式：写入单值，ttlSeconds 秒后过期。id 相同则覆盖。 */
    void set(String key, String value, long ttlSeconds);

    /** String 模式：读取单值；不存在或已过期返回 null。 */
    String get(String key);

    /** Hash 模式：写入多字段，ttlSeconds 秒后过期。id 相同则覆盖。 */
    void hmset(String key, Map<String, String> hash, long ttlSeconds);

    /** Hash 模式：读取多字段；不存在或已过期返回空 Map。 */
    Map<String, String> hmget(String key);

    /** 是否存在且未过期。 */
    boolean hasKey(String key);

    /** 删除；key 为空或不存在静默返回。 */
    void del(String key);
}
