package com.timxs.steam.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ConfigMap;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.SystemSetting;
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
    private static final String GROUP_EDITOR = "editor";

    // 图片 URL 模板常量（公开供其他类使用）
    public static final String DEFAULT_ICON_TEMPLATE = "https://media.steampowered.com/steamcommunity/public/images/apps/{appid}/{hash}.jpg";

    private final ReactiveSettingFetcher settingFetcher;
    private final ReactiveExtensionClient client;

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
     * 获取「最近游玩」刷新间隔（分钟，定时预热用）
     */
    public Mono<Integer> getRecentRefreshMinutes() {
        return getConfig()
                .map(config -> config.getRecentRefreshMinutes() != null
                        ? config.getRecentRefreshMinutes()
                        : 10);
    }

    /**
     * 获取「游戏库」刷新间隔（分钟，定时预热用）
     */
    public Mono<Integer> getGamesRefreshMinutes() {
        return getConfig()
                .map(config -> config.getGamesRefreshMinutes() != null
                        ? config.getGamesRefreshMinutes()
                        : 60);
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
        private Integer recentRefreshMinutes = 10;
        private Integer gamesRefreshMinutes = 60;
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
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ImageProxyConfig {
        private String iconImageTemplate;
        private String storeImageCdn;
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
     * 获取 Store 图片 CDN 域名
     * 留空则返回空 Mono，表示使用原始 URL
     */
    public Mono<String> getStoreImageCdn() {
        return getProxyConfig()
                .flatMap(config -> {
                    if (config.getImageProxy() != null
                            && config.getImageProxy().getStoreImageCdn() != null
                            && !config.getImageProxy().getStoreImageCdn().isBlank()) {
                        return Mono.just(config.getImageProxy().getStoreImageCdn());
                    }
                    return Mono.empty();
                });
    }

    /**
     * 替换 Store 图片 URL 的域名为 CDN 域名
     * @param originalUrl 原 Store 图片 URL
     * @param cdnDomain CDN 域名（如 https://my-cdn.com）
     * @return 替换后的 URL
     */
    public static String replaceStoreImageDomain(String originalUrl, String cdnDomain) {
        if (originalUrl == null || cdnDomain == null || cdnDomain.isBlank()) {
            return originalUrl;
        }
        try {
            java.net.URL url = new java.net.URL(originalUrl);
            // 用 getPath()（只返回路径、不含 query）；若误用 getFile() 会自带 query，导致下面重复拼接 query
            String path = url.getPath();
            String query = url.getQuery();
            String newPath = query == null ? path : path + "?" + query;
            // 确保 CDN 域名不以 / 结尾
            String domain = cdnDomain.endsWith("/") ? cdnDomain.substring(0, cdnDomain.length() - 1) : cdnDomain;
            return domain + newPath;
        } catch (Exception e) {
            return originalUrl;
        }
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
        /** ECharts JS 地址 */
        private String echartsUrl = "https://cdn.bootcdn.net/ajax/libs/echarts/5.4.3/echarts.min.js";
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

    /**
     * 获取 ECharts JS 地址
     */
    public Mono<String> getEchartsUrl() {
        return getStatsConfig()
                .map(config -> {
                    if (config.getHeatmapDisplay() == null) {
                        return "https://cdn.bootcdn.net/ajax/libs/echarts/5.4.3/echarts.min.js";
                    }
                    return config.getHeatmapDisplay().getEchartsUrl() != null
                            ? config.getHeatmapDisplay().getEchartsUrl()
                            : "https://cdn.bootcdn.net/ajax/libs/echarts/5.4.3/echarts.min.js";
                });
    }

    /**
     * 编辑器配置类
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EditorConfig {
        private String darkModeSelector = "html.dark";
        /** Steam 商店语言（如 schinese, english, auto） */
        private String storeLanguage = "auto";
    }

    /**
     * 根据 Steam 语言代码获取对应的国家/地区代码（用于 cc 参数）
     */
    public static String getCountryCode(String language) {
        if (language == null) return null;
        return switch (language) {
            case "schinese" -> "cn";
            case "tchinese" -> "tw";
            case "english" -> "us";
            case "japanese" -> "jp";
            case "koreana" -> "kr";
            case "german" -> "de";
            case "french" -> "fr";
            default -> null;
        };
    }

    /**
     * 获取国家/地区码，无法映射时回退 US。
     *
     * <p>GetItems 接口要求必须携带 country_code（不传会返回空数据）。country_code 只影响
     * 数据可见性与价格区域，不影响名称语言，因此未知语言统一回退覆盖最全的美区。
     */
    public static String getCountryCodeOrDefault(String language) {
        String cc = getCountryCode(language);
        return cc != null ? cc : "US";
    }

    /**
     * 获取 Halo 系统语言设置（从 system ConfigMap 的 basic.language 读取）
     * 返回 Mono<String>，用于响应式环境（预热、列表补全）
     */
    public Mono<String> getHaloSystemLanguage() {
        return client.fetch(ConfigMap.class, SystemSetting.SYSTEM_CONFIG)
            .map(ConfigMap::getData)
            .filter(data -> data != null && !data.isEmpty())
            .map(data -> SystemSetting.get(data, SystemSetting.Basic.GROUP, SystemSetting.Basic.class))
            .map(SystemSetting.Basic::getLanguage)
            .filter(lang -> lang != null && !lang.isBlank())
            .defaultIfEmpty("zh-CN")
            .onErrorReturn("zh-CN"); // ConfigMap 读取失败时回退
    }

    /**
     * 从 Halo 系统语言映射到 Steam API 语言代码
     * Halo: zh-CN, zh-TW, en, es 等 BCP 47 标签
     * Steam: schinese, tchinese, english, spanish 等
     */
    private static String mapHaloLangToSteamLang(String haloLang) {
        if (haloLang == null || haloLang.isBlank()) {
            return "schinese";
        }
        String normalized = haloLang.toLowerCase().replace('_', '-');
        return switch (normalized) {
            case "zh-cn" -> "schinese";
            case "zh-tw" -> "tchinese";
            default -> switch (normalized.split("-", 2)[0]) {
                case "zh" -> "schinese";
                case "en" -> "english";
                case "es" -> "spanish";
                case "ja" -> "japanese";
                case "ko" -> "koreana";
                case "de" -> "german";
                case "fr" -> "french";
                case "ru" -> "russian";
                case "pt" -> "portuguese";
                case "it" -> "italian";
                default -> "schinese"; // 其他语言回退简体中文
            };
        };
    }

    /**
     * 获取实例商店语言（用于列表/最近游玩批量补全，响应式版本）
     * 从 Halo 系统语言设置读取，而不是 JVM Locale
     */
    public Mono<String> getInstanceStoreLanguageReactive() {
        return getHaloSystemLanguage()
            .map(SteamSettingService::mapHaloLangToSteamLang);
    }

    /**
     * 根据 Steam 语言代码获取本地化的"免费"文本
     */
    public static String getFreeText(String language) {
        if (language == null) return "Free";
        return switch (language) {
            case "schinese" -> "免费";
            case "tchinese" -> "免費";
            case "english" -> "Free";
            case "japanese" -> "無料";
            case "koreana" -> "무료";
            case "german" -> "Kostenlos";
            case "french" -> "Gratuit";
            default -> "Free";
        };
    }

    /**
     * 获取编辑器配置
     */
    public Mono<EditorConfig> getEditorConfig() {
        return settingFetcher.fetch(GROUP_EDITOR, EditorConfig.class)
                .switchIfEmpty(Mono.just(new EditorConfig()));
    }

    /**
     * 获取暗色模式选择器
     */
    public Mono<String> getDarkModeSelector() {
        return getEditorConfig()
                .map(config -> config.getDarkModeSelector() != null
                        ? config.getDarkModeSelector()
                        : "html.dark");
    }

}
