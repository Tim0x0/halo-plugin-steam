package com.timxs.steam.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存缓存服务实现
 */
@Slf4j
@Service
public class InMemoryCacheService implements CacheService {

    private final ConcurrentHashMap<String, CachedData<?>> cache = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> Mono<T> get(String key, Class<T> type) {
        return Mono.fromCallable(() -> {
            CachedData<?> cachedData = cache.get(key);
            if (cachedData == null) {
                return null;
            }
            if (cachedData.isExpired()) {
                log.debug("缓存已过期: key={}", key);
                return null;
            }
            return (T) cachedData.getData();
        });
    }

    @Override
    public <T> Mono<Void> put(String key, T value, int ttlMinutes) {
        return Mono.fromRunnable(() -> {
            CachedData<T> cachedData = new CachedData<>(value, ttlMinutes);
            cache.put(key, cachedData);
            log.debug("缓存已存储: key={}, ttl={}min", key, ttlMinutes);
        });
    }

    @Override
    public Mono<Void> evict(String key) {
        return Mono.fromRunnable(() -> {
            cache.remove(key);
            log.debug("缓存已删除: key={}", key);
        });
    }

    @Override
    public Mono<Void> evictAll() {
        return Mono.fromRunnable(() -> {
            cache.clear();
            log.info("所有缓存已清空");
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Mono<T> getStale(String key, Class<T> type) {
        return Mono.fromCallable(() -> {
            CachedData<?> cachedData = cache.get(key);
            if (cachedData == null) {
                return null;
            }
            // 返回数据，即使已过期
            if (cachedData.isExpired()) {
                log.debug("返回过期缓存: key={}, age={}min", key, cachedData.getAgeMinutes());
            }
            return (T) cachedData.getData();
        });
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return Mono.fromCallable(() -> {
            CachedData<?> cachedData = cache.get(key);
            return cachedData != null && !cachedData.isExpired();
        });
    }
}
