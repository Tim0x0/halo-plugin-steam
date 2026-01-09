package com.timxs.steam.cache;

import reactor.core.publisher.Mono;

/**
 * 缓存服务接口
 */
public interface CacheService {

    /**
     * 获取缓存数据
     */
    <T> Mono<T> get(String key, Class<T> type);

    /**
     * 存储缓存数据
     * @param ttlMinutes 过期时间（分钟）
     */
    <T> Mono<Void> put(String key, T value, int ttlMinutes);

    /**
     * 删除指定缓存
     */
    Mono<Void> evict(String key);

    /**
     * 清空所有缓存
     */
    Mono<Void> evictAll();

    /**
     * 获取缓存数据（即使已过期）
     */
    <T> Mono<T> getStale(String key, Class<T> type);

    /**
     * 检查缓存是否存在且未过期
     */
    Mono<Boolean> exists(String key);
}
