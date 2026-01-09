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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.app.theme.finders.Finder;

import java.util.List;

/**
 * Steam Finder 实现，供主题模板使用
 * 在模板中通过 steamFinder 访问，例如：steamFinder.getProfile()
 */
@Slf4j
@Finder("steamFinder")
@RequiredArgsConstructor
public class SteamFinderImpl implements SteamFinder {

    private final SteamService steamService;

    @Override
    public Mono<SteamProfile> getProfile() {
        return steamService.getProfile()
                .onErrorResume(e -> {
                    log.warn("获取 Steam 资料失败: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    @Override
    public Flux<RecentGame> getRecentGames(int limit) {
        return steamService.getRecentGames(limit)
                .flatMapMany(Flux::fromIterable)
                .onErrorResume(e -> {
                    log.warn("获取最近游玩失败: {}", e.getMessage());
                    return Flux.empty();
                });
    }

    @Override
    public Mono<ListResult<OwnedGame>> getOwnedGames(int page, int size) {
        return steamService.getOwnedGames(page, size, "playtime_forever")
                .onErrorResume(e -> {
                    log.warn("获取游戏库失败: {}", e.getMessage());
                    return Mono.just(new ListResult<>(page, size, 0, List.of()));
                });
    }

    @Override
    public Mono<SteamStats> getStats() {
        return steamService.getFullStats()
                .onErrorResume(e -> {
                    log.warn("获取统计数据失败: {}", e.getMessage());
                    return Mono.just(SteamStats.builder()
                            .totalGames(0)
                            .totalPlaytimeMinutes(0)
                            .recentPlaytimeMinutes(0)
                            .build());
                });
    }

    @Override
    public Mono<BadgeInfo> getBadges() {
        return steamService.getBadges()
                .onErrorResume(e -> {
                    log.warn("获取徽章失败: {}", e.getMessage());
                    return Mono.just(BadgeInfo.builder()
                            .badges(List.of())
                            .playerXp(0)
                            .playerLevel(0)
                            .xpNeededToLevelUp(0)
                            .xpNeededCurrentLevel(0)
                            .build());
                });
    }
}
