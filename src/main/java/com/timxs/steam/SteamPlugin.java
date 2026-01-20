package com.timxs.steam;

import com.timxs.steam.cache.CacheService;
import com.timxs.steam.model.DailyPlaytimeRecord;
import com.timxs.steam.model.PlaytimeSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpec;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

import static run.halo.app.extension.index.IndexAttributeFactory.simpleAttribute;

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
@EnableScheduling
public class SteamPlugin extends BasePlugin {

    private final CacheService cacheService;
    private final SchemeManager schemeManager;

    public SteamPlugin(PluginContext pluginContext, CacheService cacheService, SchemeManager schemeManager) {
        super(pluginContext);
        this.cacheService = cacheService;
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        // 注册 PlaytimeSnapshot 并声明索引
        schemeManager.register(PlaytimeSnapshot.class, indexSpecs -> {
            indexSpecs.add(new IndexSpec()
                .setName("spec.steamId")
                .setIndexFunc(simpleAttribute(PlaytimeSnapshot.class,
                    snapshot -> snapshot.getSpec().getSteamId()))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.appId")
                .setIndexFunc(simpleAttribute(PlaytimeSnapshot.class,
                    snapshot -> snapshot.getSpec().getAppId().toString()))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.snapshotTime")
                .setOrder(IndexSpec.OrderType.DESC)
                .setIndexFunc(simpleAttribute(PlaytimeSnapshot.class,
                    snapshot -> snapshot.getSpec().getSnapshotTime().toString()))
            );
        });
        
        // 注册 DailyPlaytimeRecord 并声明索引
        schemeManager.register(DailyPlaytimeRecord.class, indexSpecs -> {
            indexSpecs.add(new IndexSpec()
                .setName("spec.steamId")
                .setIndexFunc(simpleAttribute(DailyPlaytimeRecord.class,
                    record -> record.getSpec().getSteamId()))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.date")
                .setIndexFunc(simpleAttribute(DailyPlaytimeRecord.class,
                    record -> record.getSpec().getDate()))
            );
            indexSpecs.add(new IndexSpec()
                .setName("spec.appId")
                .setIndexFunc(simpleAttribute(DailyPlaytimeRecord.class,
                    record -> record.getSpec().getAppId().toString()))
            );
        });
        
        log.info("Steam 插件启动成功！");
    }

    @Override
    public void stop() {
        // 注销 Custom Resource（使用官方推荐的写法）
        schemeManager.unregister(Scheme.buildFromType(PlaytimeSnapshot.class));
        schemeManager.unregister(Scheme.buildFromType(DailyPlaytimeRecord.class));

        // 清理缓存资源
        cacheService.evictAll().subscribe(
            unused -> log.debug("Steam 缓存已清理"),
            error -> log.warn("清理缓存失败: {}", error.getMessage())
        );
        log.info("Steam 插件已停止！");
    }
}
