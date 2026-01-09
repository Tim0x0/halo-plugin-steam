package com.timxs.steam;

import com.timxs.steam.cache.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

/**
 * <p>Plugin main class to manage the lifecycle of the plugin.</p>
 * <p>This class must be public and have a public constructor.</p>
 * <p>Only one main class extending {@link BasePlugin} is allowed per plugin.</p>
 *
 * @author Tim0x0
 * @since 1.0.0
 */
@Slf4j
@Component
public class SteamPlugin extends BasePlugin {

    private final CacheService cacheService;

    public SteamPlugin(PluginContext pluginContext, CacheService cacheService) {
        super(pluginContext);
        this.cacheService = cacheService;
    }

    @Override
    public void start() {
        log.info("Steam 插件启动成功！");
    }

    @Override
    public void stop() {
        // 清理缓存资源
        cacheService.evictAll().subscribe(
            unused -> log.debug("Steam 缓存已清理"),
            error -> log.warn("清理缓存失败: {}", error.getMessage())
        );
        log.info("Steam 插件已停止！");
    }
}
