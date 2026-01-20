package com.timxs.steam.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

/**
 * Steam 配置服务 - 从 ConfigMap 读取配置
 */
@Service
@RequiredArgsConstructor
public class SteamSettingService {

    private static final String GROUP_BASIC = "basic";
    private static final String GROUP_PAGE = "page";
    private static final String GROUP_PROXY = "proxy";
    private static final String GROUP_BADGE = "badge";
    private static final String GROUP_STATS = "stats";
    
    // 图片 URL 模板常量（公开供其他类使用）
    public static final String DEFAULT_HEADER_TEMPLATE = "https://cdn.cloudflare.steamstatic.com/steam/apps/{appid}/header.jpg";
    public static final String DEFAULT_ICON_TEMPLATE = "https://media.steampowered.com/steamcommunity/public/images/apps/{appid}/{hash}.jpg";

    private final ReactiveSettingFetcher settingFetcher;

    /**
     * 获取基本配置
     */
    public Mono<SteamConfig> getConfig() {
        return settingFetcher.fetch(GROUP_BASIC, SteamConfig.class)
                .switchIfEmpty(Mono.just(new SteamConfig()));
    }

    /**
     * 获取页面配置
     */
    public Mono<PageConfig> getPageConfig() {
        return settingFetcher.fetch(GROUP_PAGE, PageConfig.class)
                .switchIfEmpty(Mono.just(new PageConfig()));
    }

    /**
     * 获取 API Key
     */
    public Mono<String> getApiKey() {
        return getConfig()
                .map(SteamConfig::getApiKey)
                .filter(key -> key != null && !key.isBlank());
    }

    /**
     * 获取 Steam ID
     */
    public Mono<String> getSteamId() {
        return getConfig()
                .map(SteamConfig::getSteamId)
                .filter(id -> id != null && !id.isBlank());
    }

    /**
     * 获取缓存过期时间（分钟）
     */
    public Mono<Integer> getCacheTtlMinutes() {
        return getConfig()
                .map(config -> config.getCacheTtlMinutes() != null ? config.getCacheTtlMinutes() : 10);
    }

    /**
     * 获取游戏库显示数量限制
     */
    public Mono<Integer> getGamesLimit() {
        return getPageConfig()
                .map(config -> config.getGamesLimit() != null ? config.getGamesLimit() : 50);
    }

    /**
     * 获取最近游玩显示数量
     */
    public Mono<Integer> getRecentGamesLimit() {
        return getPageConfig()
                .map(config -> config.getRecentGamesLimit() != null ? config.getRecentGamesLimit() : 5);
    }

    /**
     * 获取 API 请求超时时间（秒）
     */
    public Mono<Integer> getApiTimeoutSeconds() {
        return getConfig()
                .map(config -> config.getApiTimeoutSeconds() != null 
                        ? config.getApiTimeoutSeconds() 
                        : 8);
    }

    /**
     * 获取页面标题
     */
    public Mono<String> getPageTitle() {
        return getPageConfig()
                .map(config -> config.getPageTitle() != null ? config.getPageTitle() : "Steam 游戏库");
    }

    /**
     * 获取每页显示数量
     */
    public Mono<Integer> getPageSize() {
        return getPageConfig()
                .map(config -> config.getPageSize() != null ? config.getPageSize() : 12);
    }

    /**
     * 是否显示最近游玩的成就进度
     */
    public Mono<Boolean> isShowRecentAchievements() {
        return getPageConfig()
                .map(config -> config.getShowRecentAchievements() != null && config.getShowRecentAchievements());
    }

    /**
     * 是否启用游戏卡片跳转链接
     */
    public Mono<Boolean> isEnableGameLink() {
        return getPageConfig()
                .map(config -> config.getEnableGameLink() != null && config.getEnableGameLink());
    }

    /**
     * 是否包含免费游戏
     */
    public Mono<Boolean> isIncludeFreeGames() {
        return getPageConfig()
                .map(config -> config.getIncludeFreeGames() == null || config.getIncludeFreeGames());
    }

    /**
     * Steam 基本配置类
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SteamConfig {
        private String apiKey;
        private String steamId;
        private Integer cacheTtlMinutes = 10;
        private Integer apiTimeoutSeconds = 8;
    }

    /**
     * 页面配置类
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PageConfig {
        private String pageTitle = "Steam 游戏库";
        private Integer pageSize = 12;
        private Integer gamesLimit = 50;
        private Integer recentGamesLimit = 5;
        private Boolean showRecentAchievements = false;
        private Boolean enableGameLink = false;
        private Boolean includeFreeGames = true;
        private java.util.List<HiddenGameEntry> hiddenGames;
    }

    /**
     * 隐藏游戏条目
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HiddenGameEntry {
        private String game;
    }

    /**
     * 获取隐藏的游戏 ID 集合
     * 支持直接输入游戏 ID 或 Steam 商店链接
     */
    public Mono<java.util.Set<Long>> getHiddenGameIds() {
        return getPageConfig()
                .map(config -> parseHiddenGames(config.getHiddenGames()));
    }

    /**
     * 解析隐藏游戏配置
     */
    private java.util.Set<Long> parseHiddenGames(java.util.List<HiddenGameEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return java.util.Set.of();
        }

        return entries.stream()
                .map(HiddenGameEntry::getGame)
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::extractAppId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 从输入中提取游戏 ID
     * 支持直接输入数字或 Steam 商店链接
     * 支持的链接格式：
     * - https://store.steampowered.com/app/730/
     * - https://store.steampowered.com/app/10/CounterStrike/
     * - https://store.steampowered.com/agecheck/app/578080/
     */
    private Long extractAppId(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        // 如果包含 /app/ 路径，提取后面的数字（支持各种 Steam 链接格式）
        if (input.contains("/app/")) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("/app/(\\d+)").matcher(input);
            if (matcher.find()) {
                try {
                    return Long.parseLong(matcher.group(1));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        // 否则尝试直接解析为数字
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 代理配置类
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProxyConfig {
        private ImageProxyConfig imageProxy;
        private ApiProxyConfig apiProxy;
    }

    /**
     * 图片代理配置
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ImageProxyConfig {
        private String headerImageTemplate;
        private String iconImageTemplate;
    }

    /**
     * API 代理配置
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ApiProxyConfig {
        private Boolean enabled = false;
        private String proxyType = "http";  // http 或 custom
        private String httpHost;
        private Integer httpPort;
        private String customApiUrl;
    }

    /**
     * 获取代理配置
     */
    public Mono<ProxyConfig> getProxyConfig() {
        return settingFetcher.fetch(GROUP_PROXY, ProxyConfig.class)
                .switchIfEmpty(Mono.just(new ProxyConfig()));
    }

    /**
     * 获取 API 代理配置
     */
    public Mono<ApiProxyConfig> getApiProxyConfig() {
        return getProxyConfig()
                .map(config -> config.getApiProxy() != null ? config.getApiProxy() : new ApiProxyConfig());
    }

    /**
     * 获取封面图 URL 模板
     */
    public Mono<String> getHeaderImageTemplate() {
        return getProxyConfig()
                .map(config -> {
                    if (config.getImageProxy() != null 
                            && config.getImageProxy().getHeaderImageTemplate() != null 
                            && !config.getImageProxy().getHeaderImageTemplate().isBlank()) {
                        return config.getImageProxy().getHeaderImageTemplate();
                    }
                    return DEFAULT_HEADER_TEMPLATE;
                });
    }

    /**
     * 获取图标 URL 模板
     */
    public Mono<String> getIconImageTemplate() {
        return getProxyConfig()
                .map(config -> {
                    if (config.getImageProxy() != null 
                            && config.getImageProxy().getIconImageTemplate() != null 
                            && !config.getImageProxy().getIconImageTemplate().isBlank()) {
                        return config.getImageProxy().getIconImageTemplate();
                    }
                    return DEFAULT_ICON_TEMPLATE;
                });
    }

    /**
     * 获取徽章配置
     */
    public Mono<BadgeConfig> getBadgeConfig() {
        return settingFetcher.fetch(GROUP_BADGE, BadgeConfig.class)
                .switchIfEmpty(Mono.just(new BadgeConfig()));
    }

    /**
     * 获取徽章图片 URL
     * @param badgeId 徽章 ID
     * @return 图片 URL，如果没有匹配的映射则返回 null
     */
    public Mono<String> getBadgeImageUrl(Integer badgeId) {
        return getBadgeConfig()
                .map(config -> {
                    if (config.getBadgeMappings() == null || badgeId == null) {
                        return null;
                    }
                    return config.getBadgeMappings().stream()
                            .filter(m -> badgeId.equals(m.getBadgeId()))
                            .map(BadgeMapping::getImageUrl)
                            .filter(url -> url != null && !url.isBlank())
                            .findFirst()
                            .orElse(null);
                });
    }

    /**
     * 徽章配置类
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BadgeConfig {
        private java.util.List<BadgeMapping> badgeMappings;
    }

    /**
     * 徽章映射配置（简化版：badgeId + name + imageUrl）
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BadgeMapping {
        /** 徽章 ID */
        private Integer badgeId;
        /** 徽章名称（便于识别） */
        private String name;
        /** 图片 URL */
        private String imageUrl;
    }

    /**
     * 热力图配置类（已废弃，合并到 StatsConfig）
     * @deprecated 使用 StatsConfig 代替
     */
    @Deprecated
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HeatmapConfig {
        /** 是否启用热力图功能 */
        private Boolean enabled = false;
        /** 数据保留天数 */
        private Integer retentionDays = 365;
    }

    /**
     * 获取热力图配置（已废弃）
     * @deprecated 使用 getStatsConfig() 代替
     */
    @Deprecated
    public Mono<HeatmapConfig> getHeatmapConfig() {
        return getStatsConfig()
                .map(stats -> {
                    HeatmapConfig config = new HeatmapConfig();
                    config.setEnabled(stats.getEnableTracking());
                    config.setRetentionDays(stats.getRetentionDays());
                    return config;
                });
    }

    /**
     * 热力图功能是否启用
     */
    public Mono<Boolean> isHeatmapEnabled() {
        return getStatsConfig()
                .map(config -> config.getEnableTracking() != null && config.getEnableTracking());
    }

    /**
     * 获取热力图数据保留天数
     */
    public Mono<Integer> getHeatmapRetentionDays() {
        return getStatsConfig()
                .map(config -> config.getRetentionDays() != null ? config.getRetentionDays() : 365);
    }

    /**
     * 统计配置类
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class StatsConfig {
        /** 是否启用游戏时长追踪 */
        private Boolean enableTracking = false;
        /** 数据保留天数 */
        private Integer retentionDays = 365;
        /** 热力图显示配置组 */
        private HeatmapDisplayConfig heatmapDisplay;
    }

    /**
     * 热力图显示配置类
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HeatmapDisplayConfig {
        /** 是否在页面显示热力图 */
        private Boolean showHeatmap = false;
        /** 热力图显示天数 */
        private Integer heatmapDays = 365;
        /** 热力图颜色主题 */
        private String heatmapColorTheme = "steam";
        /** 是否显示图例 */
        private Boolean heatmapShowLegend = false;
    }

    /**
     * 获取统计配置
     */
    public Mono<StatsConfig> getStatsConfig() {
        return settingFetcher.fetch(GROUP_STATS, StatsConfig.class)
                .switchIfEmpty(Mono.just(new StatsConfig()));
    }

    /**
     * 是否在页面显示热力图
     */
    public Mono<Boolean> isShowHeatmap() {
        return getStatsConfig()
                .map(config -> {
                    if (config.getHeatmapDisplay() == null) {
                        return false;
                    }
                    return config.getHeatmapDisplay().getShowHeatmap() != null 
                            && config.getHeatmapDisplay().getShowHeatmap();
                });
    }

    /**
     * 获取热力图显示天数
     */
    public Mono<Integer> getHeatmapDisplayDays() {
        return getStatsConfig()
                .map(config -> {
                    if (config.getHeatmapDisplay() == null) {
                        return 365;
                    }
                    return config.getHeatmapDisplay().getHeatmapDays() != null 
                            ? config.getHeatmapDisplay().getHeatmapDays() 
                            : 365;
                });
    }

    /**
     * 获取热力图颜色主题
     */
    public Mono<String> getHeatmapColorTheme() {
        return getStatsConfig()
                .map(config -> {
                    if (config.getHeatmapDisplay() == null) {
                        return "steam";
                    }
                    return config.getHeatmapDisplay().getHeatmapColorTheme() != null 
                            ? config.getHeatmapDisplay().getHeatmapColorTheme() 
                            : "steam";
                });
    }

    /**
     * 是否显示热力图图例
     */
    public Mono<Boolean> isShowHeatmapLegend() {
        return getStatsConfig()
                .map(config -> {
                    if (config.getHeatmapDisplay() == null) {
                        return false;
                    }
                    return config.getHeatmapDisplay().getHeatmapShowLegend() != null 
                            && config.getHeatmapDisplay().getHeatmapShowLegend();
                });
    }

}
