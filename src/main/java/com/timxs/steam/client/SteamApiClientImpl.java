package com.timxs.steam.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timxs.steam.model.AchievementProgress;
import com.timxs.steam.model.Badge;
import com.timxs.steam.model.BadgeInfo;
import com.timxs.steam.model.GameDetail;
import com.timxs.steam.model.OwnedGame;
import com.timxs.steam.model.PlayerSummary;
import com.timxs.steam.model.RecentGame;
import com.timxs.steam.model.ValidationResult;
import com.timxs.steam.service.SteamSettingService;
import com.timxs.steam.service.SteamSettingService.ApiProxyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Steam Web API 客户端实现
 */
@Slf4j
@Component
public class SteamApiClientImpl implements SteamApiClient {

    private static final String STEAM_API_BASE = "https://api.steampowered.com";
    private static final String STEAM_STORE_API = "https://store.steampowered.com";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SteamSettingService settingService;

    public SteamApiClientImpl(SteamSettingService settingService) {
        this.settingService = settingService;
    }

    /**
     * 根据配置创建 WebClient
     */
    private Mono<WebClient> getWebClient() {
        return settingService.getApiProxyConfig()
                .map(proxyConfig -> {
                    String baseUrl = getBaseUrl(proxyConfig);
                    log.debug("API 代理配置 - enabled: {}, proxyType: {}, httpHost: {}, httpPort: {}, baseUrl: {}", 
                            proxyConfig.getEnabled(), proxyConfig.getProxyType(), 
                            proxyConfig.getHttpHost(), proxyConfig.getHttpPort(), baseUrl);
                    
                    if (proxyConfig.getEnabled() != null && proxyConfig.getEnabled() 
                            && "http".equals(proxyConfig.getProxyType())
                            && proxyConfig.getHttpHost() != null && !proxyConfig.getHttpHost().isBlank()
                            && proxyConfig.getHttpPort() != null) {
                        // 使用 HTTP 代理
                        log.debug("使用 HTTP 代理: {}:{}", proxyConfig.getHttpHost(), proxyConfig.getHttpPort());
                        HttpClient httpClient = HttpClient.create()
                                .proxy(proxy -> proxy
                                        .type(ProxyProvider.Proxy.HTTP)
                                        .host(proxyConfig.getHttpHost())
                                        .port(proxyConfig.getHttpPort()));
                        return WebClient.builder()
                                .baseUrl(baseUrl)
                                .clientConnector(new ReactorClientHttpConnector(httpClient))
                                .build();
                    }
                    
                    // 不使用代理或使用自定义 API 地址
                    log.debug("不使用 HTTP 代理，直连 baseUrl: {}", baseUrl);
                    return WebClient.builder()
                            .baseUrl(baseUrl)
                            .build();
                });
    }

    /**
     * 获取 API 基础地址
     */
    private String getBaseUrl(ApiProxyConfig proxyConfig) {
        if (proxyConfig.getEnabled() != null && proxyConfig.getEnabled() 
                && "custom".equals(proxyConfig.getProxyType())
                && proxyConfig.getCustomApiUrl() != null && !proxyConfig.getCustomApiUrl().isBlank()) {
            String customUrl = proxyConfig.getCustomApiUrl().trim();
            // 移除末尾斜杠
            if (customUrl.endsWith("/")) {
                customUrl = customUrl.substring(0, customUrl.length() - 1);
            }
            return customUrl;
        }
        return STEAM_API_BASE;
    }

    private Mono<Duration> getTimeout() {
        return settingService.getApiTimeoutSeconds()
                .map(Duration::ofSeconds)
                .defaultIfEmpty(DEFAULT_TIMEOUT);
    }

    @Override
    public Mono<PlayerSummary> getPlayerSummary(String steamId) {
        return Mono.zip(settingService.getApiKey(), getTimeout(), getWebClient())
                .flatMap(tuple -> {
                    String apiKey = tuple.getT1();
                    Duration timeout = tuple.getT2();
                    WebClient webClient = tuple.getT3();
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/ISteamUser/GetPlayerSummaries/v2/")
                                    .queryParam("key", apiKey)
                                    .queryParam("steamids", steamId)
                                    .build())
                            .retrieve()
                            .bodyToMono(PlayerSummaryResponse.class)
                            .timeout(timeout)
                            .flatMap(response -> {
                                if (response.response != null && 
                                    response.response.players != null && 
                                    !response.response.players.isEmpty()) {
                                    return Mono.just(response.response.players.get(0));
                                }
                                return Mono.empty();
                            })
                            .doOnError(e -> log.error("获取用户资料失败: steamId={}", steamId, e));
                });
    }

    @Override
    public Mono<List<OwnedGame>> getOwnedGames(String steamId, boolean includeAppInfo, boolean includeFreeGames) {
        return Mono.zip(settingService.getApiKey(), getTimeout(), getWebClient())
                .flatMap(tuple -> {
                    String apiKey = tuple.getT1();
                    Duration timeout = tuple.getT2();
                    WebClient webClient = tuple.getT3();
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/IPlayerService/GetOwnedGames/v1/")
                                    .queryParam("key", apiKey)
                                    .queryParam("steamid", steamId)
                                    .queryParam("include_appinfo", includeAppInfo ? 1 : 0)
                                    .queryParam("include_played_free_games", includeFreeGames ? 1 : 0)
                                    .build())
                            .retrieve()
                            .bodyToMono(OwnedGamesResponse.class)
                            .timeout(timeout)
                            .map(response -> {
                                if (response.response != null && response.response.games != null) {
                                    return response.response.games;
                                }
                                return Collections.<OwnedGame>emptyList();
                            })
                            .doOnError(e -> log.error("获取游戏库失败: steamId={}", steamId, e));
                });
    }

    @Override
    public Mono<List<RecentGame>> getRecentlyPlayedGames(String steamId, int count) {
        return Mono.zip(settingService.getApiKey(), getTimeout(), getWebClient())
                .flatMap(tuple -> {
                    String apiKey = tuple.getT1();
                    Duration timeout = tuple.getT2();
                    WebClient webClient = tuple.getT3();
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/IPlayerService/GetRecentlyPlayedGames/v1/")
                                    .queryParam("key", apiKey)
                                    .queryParam("steamid", steamId)
                                    .queryParam("count", count)
                                    .build())
                            .retrieve()
                            .bodyToMono(RecentGamesResponse.class)
                            .timeout(timeout)
                            .map(response -> {
                                if (response.response != null && response.response.games != null) {
                                    return response.response.games;
                                }
                                return Collections.<RecentGame>emptyList();
                            })
                            .doOnError(e -> log.error("获取最近游玩失败: steamId={}", steamId, e));
                });
    }

    @Override
    public Mono<Integer> getSteamLevel(String steamId) {
        return Mono.zip(settingService.getApiKey(), getTimeout(), getWebClient())
                .flatMap(tuple -> {
                    String apiKey = tuple.getT1();
                    Duration timeout = tuple.getT2();
                    WebClient webClient = tuple.getT3();
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/IPlayerService/GetSteamLevel/v1/")
                                    .queryParam("key", apiKey)
                                    .queryParam("steamid", steamId)
                                    .build())
                            .retrieve()
                            .bodyToMono(SteamLevelResponse.class)
                            .timeout(timeout)
                            .map(response -> {
                                if (response.response != null) {
                                    return response.response.getOrDefault("player_level", 0);
                                }
                                return 0;
                            })
                            .doOnError(e -> log.error("获取Steam等级失败: steamId={}", steamId, e));
                });
    }

    @Override
    public Mono<ValidationResult> validateApiKey(String apiKey, String steamId) {
        return Mono.zip(getTimeout(), getWebClient()).flatMap(tuple -> {
            Duration timeout = tuple.getT1();
            WebClient webClient = tuple.getT2();
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ISteamUser/GetPlayerSummaries/v2/")
                            .queryParam("key", apiKey)
                            .queryParam("steamids", steamId)
                            .build())
                    .exchangeToMono(response -> {
                        int statusCode = response.statusCode().value();
                        if (response.statusCode().isError()) {
                            return response.bodyToMono(String.class)
                                    .defaultIfEmpty(response.statusCode().toString())
                                    .map(body -> ValidationResult.error(statusCode, body));
                        }
                        return response.bodyToMono(PlayerSummaryResponse.class)
                                .map(body -> {
                                    if (body.response != null && 
                                            body.response.players != null && 
                                            !body.response.players.isEmpty()) {
                                        return ValidationResult.success();
                                    }
                                    return ValidationResult.error(404, "Steam ID 不存在");
                                });
                    })
                    .timeout(timeout)
                    .onErrorResume(e -> {
                        log.error("验证 API Key 时发生错误", e);
                        return Mono.just(ValidationResult.error(500, e.getMessage()));
                    });
        });
    }

    @Override
    public Mono<AchievementProgress> getPlayerAchievements(String steamId, Long appId) {
        return Mono.zip(settingService.getApiKey(), getTimeout(), getWebClient())
                .flatMap(tuple -> {
                    String apiKey = tuple.getT1();
                    Duration timeout = tuple.getT2();
                    WebClient webClient = tuple.getT3();
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/ISteamUserStats/GetPlayerAchievements/v1/")
                                    .queryParam("key", apiKey)
                                    .queryParam("steamid", steamId)
                                    .queryParam("appid", appId)
                                    .queryParam("l", "schinese")
                                    .build())
                            .retrieve()
                            .bodyToMono(PlayerAchievementsResponse.class)
                            .timeout(timeout)
                            .map(response -> {
                                if (response.playerstats != null && response.playerstats.achievements != null) {
                                    List<AchievementItem> achievements = response.playerstats.achievements;
                                    int total = achievements.size();
                                    int achieved = (int) achievements.stream()
                                            .filter(a -> a.achieved != null && a.achieved == 1)
                                            .count();
                                    return AchievementProgress.builder()
                                            .appId(appId)
                                            .gameName(response.playerstats.gameName)
                                            .achievedCount(achieved)
                                            .totalAchievements(total)
                                            .build();
                                }
                                return AchievementProgress.builder()
                                        .appId(appId)
                                        .achievedCount(0)
                                        .totalAchievements(0)
                                        .build();
                            })
                            .onErrorResume(e -> {
                                log.debug("获取游戏成就失败 (可能游戏无成就): appId={}, error={}", appId, e.getMessage());
                                // 让错误继续传播，由 Service 层处理 403/400 逻辑
                                return Mono.error(e);
                            });
                });
    }

    // API 响应 DTO 类
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlayerSummaryResponse {
        public PlayerSummaryResponseInner response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlayerSummaryResponseInner {
        public List<PlayerSummary> players;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OwnedGamesResponse {
        public OwnedGamesResponseInner response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OwnedGamesResponseInner {
        public Integer game_count;
        public List<OwnedGame> games;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RecentGamesResponse {
        public RecentGamesResponseInner response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RecentGamesResponseInner {
        public Integer total_count;
        public List<RecentGame> games;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SteamLevelResponse {
        public Map<String, Integer> response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlayerAchievementsResponse {
        public PlayerStatsInner playerstats;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlayerStatsInner {
        public String steamID;
        public String gameName;
        public List<AchievementItem> achievements;
        public Boolean success;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AchievementItem {
        public String apiname;
        public Integer achieved;
        public Long unlocktime;
        public String name;
        public String description;
    }

    @Override
    public Mono<BadgeInfo> getBadges(String steamId) {
        return Mono.zip(settingService.getApiKey(), getTimeout(), getWebClient())
                .flatMap(tuple -> {
                    String apiKey = tuple.getT1();
                    Duration timeout = tuple.getT2();
                    WebClient webClient = tuple.getT3();
                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/IPlayerService/GetBadges/v1/")
                                    .queryParam("key", apiKey)
                                    .queryParam("steamid", steamId)
                                    .build())
                            .retrieve()
                            .bodyToMono(BadgesResponse.class)
                            .timeout(timeout)
                            .map(response -> {
                                if (response.response != null) {
                                    return BadgeInfo.builder()
                                            .badges(response.response.badges != null ? response.response.badges : List.of())
                                            .playerXp(response.response.playerXp)
                                            .playerLevel(response.response.playerLevel)
                                            .xpNeededToLevelUp(response.response.playerXpNeededToLevelUp)
                                            .xpNeededCurrentLevel(response.response.playerXpNeededCurrentLevel)
                                            .build();
                                }
                                return BadgeInfo.builder()
                                        .badges(List.of())
                                        .playerXp(0)
                                        .playerLevel(0)
                                        .xpNeededToLevelUp(0)
                                        .xpNeededCurrentLevel(0)
                                        .build();
                            })
                            .doOnError(e -> log.error("获取徽章失败: steamId={}", steamId, e));
                });
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BadgesResponse {
        public BadgesResponseInner response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class BadgesResponseInner {
        public List<Badge> badges;
        @JsonProperty("player_xp")
        public Integer playerXp;
        @JsonProperty("player_level")
        public Integer playerLevel;
        @JsonProperty("player_xp_needed_to_level_up")
        public Integer playerXpNeededToLevelUp;
        @JsonProperty("player_xp_needed_current_level")
        public Integer playerXpNeededCurrentLevel;
    }

    @Override
    public Mono<GameDetail> getGameDetail(Long appId, String language) {
        return Mono.zip(getTimeout(), getStoreWebClient())
                .flatMap(tuple -> {
                    Duration timeout = tuple.getT1();
                    WebClient webClient = tuple.getT2();
                    String cc = SteamSettingService.getCountryCode(language);
                    return webClient.get()
                            .uri(uriBuilder -> {
                                uriBuilder.path("/api/appdetails")
                                        .queryParam("appids", appId)
                                        .queryParam("l", language);
                                if (cc != null) {
                                    uriBuilder.queryParam("cc", cc);
                                }
                                return uriBuilder.build();
                            })
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(timeout)
                            .flatMap(body -> parseStoreResponse(body, appId, language))
                            .doOnError(e -> log.error("获取游戏详情失败: appId={}", appId, e));
                });
    }

    private Mono<WebClient> getStoreWebClient() {
        return settingService.getApiProxyConfig()
                .map(proxyConfig -> {
                    String baseUrl = getStoreBaseUrl(proxyConfig);
                    if (proxyConfig.getEnabled() != null && proxyConfig.getEnabled()
                            && "http".equals(proxyConfig.getProxyType())
                            && proxyConfig.getHttpHost() != null && !proxyConfig.getHttpHost().isBlank()
                            && proxyConfig.getHttpPort() != null) {
                        HttpClient httpClient = HttpClient.create()
                                .proxy(proxy -> proxy
                                        .type(ProxyProvider.Proxy.HTTP)
                                        .host(proxyConfig.getHttpHost())
                                        .port(proxyConfig.getHttpPort()));
                        return WebClient.builder()
                                .baseUrl(baseUrl)
                                .clientConnector(new ReactorClientHttpConnector(httpClient))
                                .build();
                    }
                    return WebClient.builder()
                            .baseUrl(baseUrl)
                            .build();
                });
    }

    /**
     * 获取 Store API 基础地址
     * 自定义 API 地址时，将路径前缀改为 /store 以区分 Steam Web API 和 Store API
     */
    private String getStoreBaseUrl(ApiProxyConfig proxyConfig) {
        if (proxyConfig.getEnabled() != null && proxyConfig.getEnabled()
                && "custom".equals(proxyConfig.getProxyType())
                && proxyConfig.getCustomApiUrl() != null && !proxyConfig.getCustomApiUrl().isBlank()) {
            String customUrl = proxyConfig.getCustomApiUrl().trim();
            if (customUrl.endsWith("/")) {
                customUrl = customUrl.substring(0, customUrl.length() - 1);
            }
            return customUrl;
        }
        return STEAM_STORE_API;
    }

    private Mono<GameDetail> parseStoreResponse(String body, Long appId, String language) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            JsonNode appNode = root.path(String.valueOf(appId));

            if (!appNode.path("success").asBoolean(false)) {
                return Mono.empty();
            }

            JsonNode data = appNode.path("data");

            // 开发商
            StringBuilder devs = new StringBuilder();
            JsonNode devsNode = data.path("developers");
            if (devsNode.isArray()) {
                for (int i = 0; i < devsNode.size(); i++) {
                    if (i > 0) devs.append(", ");
                    devs.append(devsNode.get(i).asText());
                }
            }

            // 发行商
            StringBuilder pubs = new StringBuilder();
            JsonNode pubsNode = data.path("publishers");
            if (pubsNode.isArray()) {
                for (int i = 0; i < pubsNode.size(); i++) {
                    if (i > 0) pubs.append(", ");
                    pubs.append(pubsNode.get(i).asText());
                }
            }

            // 类型标签
            StringBuilder genres = new StringBuilder();
            JsonNode genresNode = data.path("genres");
            if (genresNode.isArray()) {
                for (int i = 0; i < genresNode.size(); i++) {
                    if (i > 0) genres.append(", ");
                    genres.append(genresNode.get(i).path("description").asText());
                }
            }

            // 价格
            String priceFormatted = null;
            boolean isFree = data.path("is_free").asBoolean(false);
            if (isFree) {
                priceFormatted = SteamSettingService.getFreeText(language);
            } else {
                JsonNode priceNode = data.path("price_overview");
                if (!priceNode.isMissingNode()) {
                    priceFormatted = priceNode.path("final_formatted").asText(null);
                }
            }

            // 发售日期
            String releaseDate = null;
            JsonNode releaseDateNode = data.path("release_date");
            if (!releaseDateNode.isMissingNode()) {
                releaseDate = releaseDateNode.path("date").asText(null);
            }

            GameDetail detail = GameDetail.builder()
                    .appId(appId)
                    .name(data.path("name").asText(null))
                    .headerImage(data.path("header_image").asText(null))
                    .shortDescription(data.path("short_description").asText(null))
                    .developers(devs.length() > 0 ? devs.toString() : null)
                    .publishers(pubs.length() > 0 ? pubs.toString() : null)
                    .genres(genres.length() > 0 ? genres.toString() : null)
                    .isFree(isFree)
                    .priceFormatted(priceFormatted)
                    .releaseDate(releaseDate)
                    .storeUrl("https://store.steampowered.com/app/" + appId)
                    .owned(false)
                    .build();

            return Mono.just(detail);
        } catch (Exception e) {
            log.error("解析 Steam Store API 响应失败: appId={}", appId, e);
            return Mono.error(e);
        }
    }
}
