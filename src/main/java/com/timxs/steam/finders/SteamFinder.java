package com.timxs.steam.finders;

import com.timxs.steam.model.BadgeInfo;
import com.timxs.steam.model.OwnedGame;
import com.timxs.steam.model.RecentGame;
import com.timxs.steam.model.SteamProfile;
import com.timxs.steam.model.SteamStats;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 * Steam Finder 接口，供主题模板使用
 */
public interface SteamFinder {

    /**
     * 获取 Steam 用户资料
     */
    Mono<SteamProfile> getProfile();

    /**
     * 获取最近游玩的游戏
     * @param limit 返回数量限制
     */
    Flux<RecentGame> getRecentGames(int limit);

    /**
     * 获取游戏库列表（分页）
     * @param page 页码（从1开始）
     * @param size 每页数量
     */
    Mono<ListResult<OwnedGame>> getOwnedGames(int page, int size);

    /**
     * 获取统计数据
     */
    Mono<SteamStats> getStats();

    /**
     * 获取徽章信息
     */
    Mono<BadgeInfo> getBadges();
}
