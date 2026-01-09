package com.timxs.steam.service;

import com.timxs.steam.model.AchievementProgress;
import com.timxs.steam.model.BadgeInfo;
import com.timxs.steam.model.OwnedGame;
import com.timxs.steam.model.RecentGame;
import com.timxs.steam.model.SteamProfile;
import com.timxs.steam.model.SteamStats;
import com.timxs.steam.model.ValidationResult;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

import java.util.List;

/**
 * Steam 业务服务接口
 */
public interface SteamService {

    /**
     * 获取完整的用户资料（包含等级）
     */
    Mono<SteamProfile> getProfile();

    /**
     * 获取游戏库列表
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @param sortBy 排序字段 (playtime_forever, name)
     */
    Mono<ListResult<OwnedGame>> getOwnedGames(int page, int size, String sortBy);

    /**
     * 获取最近游玩的游戏
     * @param limit 返回数量限制
     */
    Mono<List<RecentGame>> getRecentGames(int limit);

    /**
     * 获取全量统计数据（不受 gamesLimit 限制）
     */
    Mono<SteamStats> getFullStats();

    /**
     * 获取指定游戏的成就进度
     * @param appId 游戏 ID
     */
    Mono<AchievementProgress> getAchievementProgress(Long appId);

    /**
     * 获取用户徽章信息
     */
    Mono<BadgeInfo> getBadges();

    /**
     * 刷新缓存
     */
    Mono<Void> refreshCache();

    /**
     * 验证 API Key 和 Steam ID 是否有效
     * @return 验证结果，包含详细错误信息
     */
    Mono<ValidationResult> validateApiKey(String apiKey, String steamId);
}
