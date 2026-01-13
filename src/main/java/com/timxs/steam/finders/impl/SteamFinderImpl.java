package com.timxs.steam.finders.impl;

import com.timxs.steam.finders.SteamFinder;
import com.timxs.steam.model.BadgeInfo;
import com.timxs.steam.model.OwnedGame;
import com.timxs.steam.model.RecentGame;
import com.timxs.steam.model.SteamProfile;
import com.timxs.steam.model.SteamStats;
import com.timxs.steam.service.SteamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.app.theme.finders.Finder;

import java.time.Duration;
import java.util.List;

/**
 * Steam Finder 实现，供主题模板使用
 * 在模板中通过 steamFinder 访问，例如：steamFinder.getProfile()
 * 
 * 错误处理约定：
 * - API 请求失败：返回 null（Mono.empty()）
 * - 成功但没数据：返回空对象/空列表
 * 
 * 注意：Finder 层固定 9 秒超时，比 Halo 框架的 10 秒 blocking read 超时更短，
 * 确保在框架超时前返回结果，避免抛出异常导致页面渲染失败。
 */
@Slf4j
@Finder("steamFinder")
@RequiredArgsConstructor
public class SteamFinderImpl implements SteamFinder {

    /**
     * Finder 层固定超时时间（9秒）
     * 必须小于 Halo 框架的 blocking read 超时（10秒）
     */
    private static final Duration FINDER_TIMEOUT = Duration.ofSeconds(9);

    private final SteamService steamService;

    @Override
    public Mono<SteamProfile> getProfile() {
        return steamService.getProfile()
                .timeout(FINDER_TIMEOUT)
                .onErrorResume(e -> {
                    log.warn("获取 Steam 资料失败: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<List<RecentGame>> getRecentGames(int limit) {
        return steamService.getRecentGames(limit)
                .timeout(FINDER_TIMEOUT)
                .onErrorResume(e -> {
                    log.warn("获取最近游玩失败: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<ListResult<OwnedGame>> getOwnedGames(int page, int size) {
        return steamService.getOwnedGames(page, size, "playtime_forever")
                .timeout(FINDER_TIMEOUT)
                .onErrorResume(e -> {
                    log.warn("获取游戏库失败: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<SteamStats> getStats() {
        return steamService.getFullStats()
                .timeout(FINDER_TIMEOUT)
                .onErrorResume(e -> {
                    log.warn("获取统计数据失败: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<BadgeInfo> getBadges() {
        return steamService.getBadges()
                .timeout(FINDER_TIMEOUT)
                .onErrorResume(e -> {
                    log.warn("获取徽章失败: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}
