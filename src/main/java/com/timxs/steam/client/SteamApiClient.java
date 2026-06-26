package com.timxs.steam.client;

import com.timxs.steam.model.AchievementProgress;
import com.timxs.steam.model.BadgeInfo;
import com.timxs.steam.model.GameDetail;
import com.timxs.steam.model.OwnedGame;
import com.timxs.steam.model.PlayerSummary;
import com.timxs.steam.model.RecentGame;
import com.timxs.steam.model.StoreItem;
import com.timxs.steam.model.ValidationResult;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

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

    /**
     * 获取游戏详情（来自 Steam Store API）
     * @param appId 游戏 ID
     * @param language Steam 语言代码（如 schinese, english）
     */
    Mono<GameDetail> getGameDetail(Long appId, String language);

    /**
     * 批量获取商店条目信息（本地化名称 + 真实封面地址）
     * 调用 IStoreBrowseService/GetItems/v1（免 API Key，支持批量）
     *
     * <p>用于补全新游戏缺失的名称和封面 —— 新游戏的封面只存在于带哈希的
     * store_item_assets 路径，无法靠 appId 拼接。内部按批分片请求。
     *
     * @param appIds 游戏 ID 列表
     * @param language Steam 语言代码（如 schinese, english），决定名称语言
     * @param countryCode 国家/地区码（如 cn, us），决定数据可见性，不影响名称
     * @return appId -> StoreItem 映射；查询失败或无数据时为空 Map
     */
    Mono<Map<Long, StoreItem>> getStoreItems(List<Long> appIds, String language, String countryCode);
}
