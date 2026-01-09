package com.timxs.steam.client;

import com.timxs.steam.model.AchievementProgress;
import com.timxs.steam.model.BadgeInfo;
import com.timxs.steam.model.OwnedGame;
import com.timxs.steam.model.PlayerSummary;
import com.timxs.steam.model.RecentGame;
import com.timxs.steam.model.ValidationResult;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Steam Web API 客户端接口
 */
public interface SteamApiClient {

    /**
     * 获取用户基本信息
     * 调用 ISteamUser/GetPlayerSummaries/v2
     */
    Mono<PlayerSummary> getPlayerSummary(String steamId);

    /**
     * 获取用户拥有的游戏列表
     * 调用 IPlayerService/GetOwnedGames/v1
     */
    Mono<List<OwnedGame>> getOwnedGames(String steamId, boolean includeAppInfo, boolean includeFreeGames);

    /**
     * 获取最近游玩的游戏
     * 调用 IPlayerService/GetRecentlyPlayedGames/v1
     */
    Mono<List<RecentGame>> getRecentlyPlayedGames(String steamId, int count);

    /**
     * 获取用户 Steam 等级
     * 调用 IPlayerService/GetSteamLevel/v1
     */
    Mono<Integer> getSteamLevel(String steamId);

    /**
     * 获取玩家游戏成就进度
     * 调用 ISteamUserStats/GetPlayerAchievements/v1
     */
    Mono<AchievementProgress> getPlayerAchievements(String steamId, Long appId);

    /**
     * 获取用户徽章信息
     * 调用 IPlayerService/GetBadges/v1
     */
    Mono<BadgeInfo> getBadges(String steamId);

    /**
     * 验证 API Key 和 Steam ID 是否有效
     * @return 验证结果，包含详细错误信息
     */
    Mono<ValidationResult> validateApiKey(String apiKey, String steamId);
}
