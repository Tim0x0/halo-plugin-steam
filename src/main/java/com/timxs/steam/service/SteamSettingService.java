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
                .map(config -> config.getApiTimeoutSeconds() != null ? config.getApiTimeoutSeconds() : 10);
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
                .map(config -> config.getEnableGameLink() == null || config.getEnableGameLink());
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
        private Integer apiTimeoutSeconds = 10;
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
        private Boolean enableGameLink = true;
        private Boolean includeFreeGames = true;
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

}
