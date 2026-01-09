package com.timxs.steam.cache;

import lombok.Data;

/**
 * 缓存数据包装类
 */
@Data
public class CachedData<T> {
    
    private T data;
    private long cachedAt;
    private long expiresAt;

    public CachedData() {
    }

    public CachedData(T data, int ttlMinutes) {
        this.data = data;
        this.cachedAt = System.currentTimeMillis();
        this.expiresAt = this.cachedAt + (ttlMinutes * 60 * 1000L);
    }

    /**
     * 判断缓存是否已过期
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    /**
     * 获取缓存年龄（分钟）
     */
    public long getAgeMinutes() {
        return (System.currentTimeMillis() - cachedAt) / (60 * 1000);
    }
}
