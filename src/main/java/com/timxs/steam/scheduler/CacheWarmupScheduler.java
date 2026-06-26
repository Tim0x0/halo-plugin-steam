package com.timxs.steam.scheduler;

import com.timxs.steam.service.SteamService;
import com.timxs.steam.service.SteamSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 缓存定时预热任务
 *
 * <p>后台定时把页面要展示的数据拉回来填充缓存，前端只读缓存（秒开、不超时）。
 * 分两组、各自频率：「活跃」组（资料 + 最近游玩，变化快）、「库藏」组（游戏库 + 徽章，变化慢且重）。
 *
 * <p>用每分钟一次的“心跳”轮询 + 按配置间隔判断是否真正刷新，从而让刷新间隔可在后台配置
 * （@Scheduled 的固定值无法直接读取插件配置）。首次在启动后很快触发，解决冷启动空窗。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmupScheduler {

    private static final long MINUTE_MS = 60_000L;

    private final SteamService steamService;
    private final SteamSettingService settingService;

    /** 上次「活跃」组预热时间戳（0 表示从未，启动后第一次心跳即触发） */
    private volatile long lastActivityMs = 0L;
    /** 上次「库藏」组预热时间戳 */
    private volatile long lastLibraryMs = 0L;

    /**
     * 心跳：启动 15 秒后开始，之后每 60 秒一次。
     * 每次读取配置的刷新间隔，到点才真正预热。
     */
    @Scheduled(initialDelay = 15_000, fixedDelay = 60_000)
    public void heartbeat() {
        long now = System.currentTimeMillis();
        // 两组串联：若同一心跳两组都到点，则先活跃后库藏，避免并发叠加触发 Steam 限流；
        // 未到点的组返回 Mono.empty()，不影响另一组照常判断与执行。
        maybeWarmActivity(now)
                .then(maybeWarmLibrary(now))
                .subscribe();
    }

    /** 「活跃」组（资料 + 最近游玩）：到点才预热，否则返回空 */
    private Mono<Void> maybeWarmActivity(long now) {
        return settingService.getRecentRefreshMinutes()
                .filter(minutes -> now - lastActivityMs >= minutes * MINUTE_MS)
                .flatMap(minutes -> {
                    lastActivityMs = now;
                    log.debug("触发「活跃」组预热（资料 + 最近游玩）");
                    return steamService.warmUpActivity();
                })
                .onErrorResume(e -> {
                    log.warn("「活跃」组预热失败: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    /** 「库藏」组（游戏库 + 徽章）：到点才预热，否则返回空 */
    private Mono<Void> maybeWarmLibrary(long now) {
        return settingService.getGamesRefreshMinutes()
                .filter(minutes -> now - lastLibraryMs >= minutes * MINUTE_MS)
                .flatMap(minutes -> {
                    lastLibraryMs = now;
                    log.debug("触发「库藏」组预热（游戏库 + 徽章）");
                    return steamService.warmUpLibrary();
                })
                .onErrorResume(e -> {
                    log.warn("「库藏」组预热失败: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}
