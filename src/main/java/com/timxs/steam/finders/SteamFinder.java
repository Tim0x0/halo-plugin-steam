package com.timxs.steam.finders;

import com.timxs.steam.model.BadgeInfo;
import com.timxs.steam.model.OwnedGame;
import com.timxs.steam.model.RecentGame;
import com.timxs.steam.model.SteamProfile;
import com.timxs.steam.model.SteamStats;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

import java.util.List;

/**
 * Steam Finder 接口，供主题模板使用
 * 
 * 错误处理约定：
 * - API 请求失败：返回 null（Mono.empty()）
 * - 成功但没数据：返回空对象/空列表
 */
public interface SteamFinder {

    /**
     * 获取 Steam 用户资料
     * @return 用户资料，失败返回 null
     */
    Mono<SteamProfile> getProfile();

    /**
     * 获取最近游玩的游戏
     * @param limit 返回数量限制
     * @return 游戏列表，失败返回 null，没数据返回空列表
     */
    Mono<List<RecentGame>> getRecentGames(int limit);

    /**
     * 获取游戏库列表（分页）
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 游戏列表，失败返回 null，没数据返回空 ListResult
     */
    Mono<ListResult<OwnedGame>> getOwnedGames(int page, int size);

    /**
     * 获取统计数据
     * @return 统计数据，失败返回 null
     */
    Mono<SteamStats> getStats();

    /**
     * 获取徽章信息
     * @return 徽章信息，失败返回 null，没徽章返回空 BadgeInfo
     */
    Mono<BadgeInfo> getBadges();
}
