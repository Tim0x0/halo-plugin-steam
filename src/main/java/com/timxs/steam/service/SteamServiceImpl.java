package com.timxs.steam.service;

import com.timxs.steam.cache.CacheService;
import com.timxs.steam.client.SteamApiClient;
import com.timxs.steam.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Steam 业务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SteamServiceImpl implements SteamService {

    private static final String CACHE_KEY_PROFILE = "steam:profile";
    private static final String CACHE_KEY_GAMES = "steam:games";
    private static final String CACHE_KEY_RECENT = "steam:recent";
    private static final String CACHE_KEY_BADGES = "steam:badges";

    private final SteamApiClient steamApiClient;
    private final CacheService cacheService;
    private final SteamSettingService settingService;
    
    // Singleflight: 防止并发请求重复调用 Steam API
    private final ConcurrentHashMap<String, Mono<?>> inflightRequests = new ConcurrentHashMap<>();

    @Override
    public Mono<SteamProfile> getProfile() {
        return settingService.getConfig()
                .flatMap(config -> {
                    int ttl = config.getCacheTtlMinutes() != null ? config.getCacheTtlMinutes() : 10;
                    String steamId = config.getSteamId();

                    // 尝试从缓存获取
                    return cacheService.get(CACHE_KEY_PROFILE, SteamProfile.class)
                            .switchIfEmpty(fetchAndCacheProfile(steamId, ttl))
                            .onErrorResume(e -> {
                                log.warn("获取资料失败，尝试返回缓存数据", e);
                                return cacheService.getStale(CACHE_KEY_PROFILE, SteamProfile.class);
                            });
                });
    }

    private Mono<SteamProfile> fetchAndCacheProfile(String steamId, int ttl) {
        return singleflight(CACHE_KEY_PROFILE, Mono.defer(() -> {
            log.debug("从 Steam API 获取用户资料: steamId={}", steamId);
            return Mono.zip(
                    steamApiClient.getPlayerSummary(steamId),
                    steamApiClient.getSteamLevel(steamId)
            ).map(tuple -> {
                SteamProfile profile = new SteamProfile();
                profile.setSummary(tuple.getT1());
                profile.setSteamLevel(tuple.getT2());
                log.debug("用户资料获取成功: {}", profile.getSummary().getPersonaName());
                return profile;
            }).flatMap(profile ->
                    cacheService.put(CACHE_KEY_PROFILE, profile, ttl)
                            .thenReturn(profile)
            );
        }));
    }
    
    /**
     * Singleflight: 对同一个 key 的并发请求只执行一次，其他请求共享结果
     */
    @SuppressWarnings("unchecked")
    private <T> Mono<T> singleflight(String key, Mono<T> fetcher) {
        return Mono.defer(() -> {
            Mono<T> cached = (Mono<T>) inflightRequests.computeIfAbsent(key,
                    k -> fetcher
                            .doFinally(s -> inflightRequests.remove(k))
                            .cache()
            );
            return cached;
        });
    }

    @Override
    public Mono<ListResult<OwnedGame>> getOwnedGames(int page, int size, String sortBy) {
        return Mono.zip(
                settingService.getConfig(),
                settingService.getGamesLimit(),
                settingService.getHeaderImageTemplate(),
                settingService.getIconImageTemplate(),
                settingService.getHiddenGameIds()
        ).flatMap(tuple -> {
            var config = tuple.getT1();
            int gamesLimit = tuple.getT2();
            String headerTemplate = tuple.getT3();
            String iconTemplate = tuple.getT4();
            var hiddenGameIds = tuple.getT5();
            int ttl = config.getCacheTtlMinutes() != null ? config.getCacheTtlMinutes() : 10;
            String steamId = config.getSteamId();

            return cacheService.get(CACHE_KEY_GAMES, GamesList.class)
                    .switchIfEmpty(fetchAndCacheGames(steamId, ttl))
                    .map(gamesList -> {
                        // 为每个游戏设置 URL 模板
                        applyTemplates(gamesList.getGames(), headerTemplate, iconTemplate);
                        // 过滤隐藏的游戏
                        List<OwnedGame> filteredGames = filterHiddenGames(gamesList.getGames(), hiddenGameIds);
                        return paginateAndSort(filteredGames, page, size, sortBy, gamesLimit);
                    })
                    .onErrorResume(e -> {
                        log.warn("获取游戏库失败，尝试返回缓存数据", e);
                        return cacheService.getStale(CACHE_KEY_GAMES, GamesList.class)
                                .map(gamesList -> {
                                    applyTemplates(gamesList.getGames(), headerTemplate, iconTemplate);
                                    List<OwnedGame> filteredGames = filterHiddenGames(gamesList.getGames(), hiddenGameIds);
                                    return paginateAndSort(filteredGames, page, size, sortBy, gamesLimit);
                                });
                    });
        });
    }
    
    /**
     * 为游戏列表设置 URL 模板
     */
    private void applyTemplates(List<? extends OwnedGame> games, String headerTemplate, String iconTemplate) {
        if (games != null) {
            games.forEach(game -> {
                game.setHeaderTemplate(headerTemplate);
                game.setIconTemplate(iconTemplate);
            });
        }
    }

    private Mono<GamesList> fetchAndCacheGames(String steamId, int ttl) {
        return singleflight(CACHE_KEY_GAMES, Mono.defer(() -> {
            log.debug("从 Steam API 获取游戏库: steamId={}", steamId);
            return settingService.isIncludeFreeGames()
                    .flatMap(includeFreeGames -> steamApiClient.getOwnedGames(steamId, true, includeFreeGames))
                    .map(games -> {
                        log.debug("游戏库获取成功: {} 款游戏", games.size());
                        GamesList gamesList = new GamesList();
                        gamesList.setGames(games);
                        return gamesList;
                    })
                    .flatMap(gamesList ->
                            cacheService.put(CACHE_KEY_GAMES, gamesList, ttl)
                                    .thenReturn(gamesList)
                    );
        }));
    }

    private ListResult<OwnedGame> paginateAndSort(List<OwnedGame> allGames, int page, int size, String sortBy, int gamesLimit) {
        // 排序
        List<OwnedGame> sortedGames = sortGames(allGames, sortBy);
        
        // 应用 gamesLimit 限制（0 表示不限制）
        if (gamesLimit > 0 && sortedGames.size() > gamesLimit) {
            sortedGames = sortedGames.subList(0, gamesLimit);
        }

        // 分页
        int totalCount = sortedGames.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalCount);

        List<OwnedGame> pagedList;
        if (fromIndex >= totalCount) {
            pagedList = List.of();
        } else {
            pagedList = sortedGames.subList(fromIndex, toIndex);
        }

        return new ListResult<>(page, size, totalCount, pagedList);
    }

    private List<OwnedGame> sortGames(List<OwnedGame> games, String sortBy) {
        if (games == null || games.isEmpty()) {
            return List.of();
        }

        Comparator<OwnedGame> comparator;
        if ("name".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(
                    OwnedGame::getName,
                    Comparator.nullsLast(String::compareToIgnoreCase)
            );
        } else {
            // 默认按游玩时长降序
            comparator = Comparator.comparing(
                    OwnedGame::getPlaytimeForever,
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
        }

        return games.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * 过滤隐藏的游戏
     */
    private List<OwnedGame> filterHiddenGames(List<OwnedGame> games, Set<Long> hiddenGameIds) {
        if (games == null || games.isEmpty() || hiddenGameIds == null || hiddenGameIds.isEmpty()) {
            return games != null ? games : List.of();
        }
        return games.stream()
                .filter(game -> game.getAppId() == null || !hiddenGameIds.contains(game.getAppId()))
                .collect(Collectors.toList());
    }

    /**
     * 过滤隐藏的最近游玩游戏
     */
    private List<RecentGame> filterHiddenRecentGames(List<RecentGame> games, Set<Long> hiddenGameIds) {
        if (games == null || games.isEmpty() || hiddenGameIds == null || hiddenGameIds.isEmpty()) {
            return games != null ? games : List.of();
        }
        return games.stream()
                .filter(game -> game.getAppId() == null || !hiddenGameIds.contains(game.getAppId()))
                .collect(Collectors.toList());
    }

    @Override
    public Mono<List<RecentGame>> getRecentGames(int limit) {
        return Mono.zip(
                settingService.getConfig(),
                settingService.getRecentGamesLimit(),
                settingService.getHeaderImageTemplate(),
                settingService.getIconImageTemplate(),
                settingService.isShowRecentAchievements(),
                settingService.getHiddenGameIds()
        ).flatMap(tuple -> {
            var config = tuple.getT1();
            int configLimit = tuple.getT2();
            String headerTemplate = tuple.getT3();
            String iconTemplate = tuple.getT4();
            boolean showAchievements = tuple.getT5();
            var hiddenGameIds = tuple.getT6();
            int ttl = config.getCacheTtlMinutes() != null ? config.getCacheTtlMinutes() : 10;
            String steamId = config.getSteamId();
            int actualLimit = limit > 0 ? limit : configLimit;

            return cacheService.get(CACHE_KEY_RECENT, RecentGamesList.class)
                    .map(RecentGamesList::getGames)
                    .switchIfEmpty(fetchAndCacheRecentGames(steamId, ttl))
                    .map(games -> {
                        // 为每个游戏设置 URL 模板
                        applyTemplates(games, headerTemplate, iconTemplate);
                        // 过滤隐藏的游戏
                        List<RecentGame> filteredGames = filterHiddenRecentGames(games, hiddenGameIds);
                        return filteredGames.stream().limit(actualLimit).collect(Collectors.toList());
                    })
                    .flatMap(games -> {
                        if (showAchievements && !games.isEmpty()) {
                            // 并行获取每个游戏的成就进度
                            return enrichWithAchievements(games, steamId);
                        }
                        return Mono.just(games);
                    })
                    .onErrorResume(e -> {
                        log.warn("获取最近游玩失败，尝试返回缓存数据", e);
                        return cacheService.getStale(CACHE_KEY_RECENT, RecentGamesList.class)
                                .map(RecentGamesList::getGames)
                                .map(games -> {
                                    applyTemplates(games, headerTemplate, iconTemplate);
                                    List<RecentGame> filteredGames = filterHiddenRecentGames(games, hiddenGameIds);
                                    return filteredGames.stream().limit(actualLimit).collect(Collectors.toList());
                                });
                    });
        });
    }

    /**
     * 并行获取成就进度并填充到游戏列表
     */
    private Mono<List<RecentGame>> enrichWithAchievements(List<RecentGame> games, String steamId) {
        if (games.isEmpty()) {
            return Mono.just(games);
        }
        
        log.debug("开始获取 {} 款游戏的成就进度", games.size());
        List<Mono<RecentGame>> enrichedGames = games.stream()
                .map(game -> steamApiClient.getPlayerAchievements(steamId, game.getAppId())
                        .map(progress -> {
                            game.setAchievedCount(progress.getAchievedCount());
                            game.setTotalAchievements(progress.getTotalAchievements());
                            return game;
                        })
                        .onErrorResume(e -> {
                            log.debug("获取游戏 {} 成就失败: {}", game.getAppId(), e.getMessage());
                            // 403 表示成就不可用（隐私设置不公开）
                            if (e.getMessage() != null && e.getMessage().contains("403")) {
                                game.setAchievementsLocked(true);
                            }
                            // 400 表示游戏没有成就系统，不做标记
                            return Mono.just(game);
                        }))
                .collect(Collectors.toList());
        
        return Mono.zip(enrichedGames, results -> {
            List<RecentGame> list = new java.util.ArrayList<>();
            for (Object result : results) {
                list.add((RecentGame) result);
            }
            return list;
        });
    }

    private Mono<List<RecentGame>> fetchAndCacheRecentGames(String steamId, int ttl) {
        return singleflight(CACHE_KEY_RECENT, Mono.defer(() -> {
            log.debug("从 Steam API 获取最近游玩: steamId={}", steamId);
            // 请求全部最近游玩的游戏（不限制数量），显示时再截取
            return steamApiClient.getRecentlyPlayedGames(steamId, 0)
                    .flatMap(games -> {
                        log.debug("最近游玩获取成功: {} 款游戏", games.size());
                        RecentGamesList gamesList = new RecentGamesList();
                        gamesList.setGames(games);
                        return cacheService.put(CACHE_KEY_RECENT, gamesList, ttl)
                                .thenReturn(games);
                    });
        }));
    }

    @Override
    public Mono<Void> refreshCache() {
        return cacheService.evictAll()
                .doOnSuccess(v -> log.info("Steam 缓存已刷新"));
    }

    @Override
    public Mono<SteamStats> getFullStats() {
        return Mono.zip(
                settingService.getConfig(),
                settingService.getHeaderImageTemplate(),
                settingService.getIconImageTemplate()
        ).flatMap(tuple -> {
            var config = tuple.getT1();
            String headerTemplate = tuple.getT2();
            String iconTemplate = tuple.getT3();
            int ttl = config.getCacheTtlMinutes() != null ? config.getCacheTtlMinutes() : 10;
            String steamId = config.getSteamId();

            // 获取全量游戏数据（必须）
            Mono<GamesList> gamesMono = cacheService.get(CACHE_KEY_GAMES, GamesList.class)
                    .switchIfEmpty(fetchAndCacheGames(steamId, ttl))
                    .doOnNext(gamesList -> applyTemplates(gamesList.getGames(), headerTemplate, iconTemplate))
                    .onErrorResume(e -> {
                        log.warn("获取游戏库失败: {}", e.getMessage());
                        return cacheService.getStale(CACHE_KEY_GAMES, GamesList.class)
                                .defaultIfEmpty(new GamesList());
                    });

            // 获取最近游玩数据（可选，失败返回空列表）
            Mono<List<RecentGame>> recentMono = cacheService.get(CACHE_KEY_RECENT, RecentGamesList.class)
                    .map(RecentGamesList::getGames)
                    .switchIfEmpty(fetchAndCacheRecentGames(steamId, ttl))
                    .doOnNext(games -> applyTemplates(games, headerTemplate, iconTemplate))
                    .onErrorResume(e -> {
                        log.warn("获取最近游玩失败，统计中跳过: {}", e.getMessage());
                        return cacheService.getStale(CACHE_KEY_RECENT, RecentGamesList.class)
                                .map(RecentGamesList::getGames)
                                .defaultIfEmpty(List.of());
                    });

            return Mono.zip(gamesMono, recentMono).map(data -> {
                var allGames = data.getT1().getGames();
                var recentGames = data.getT2();

                int totalGames = allGames != null ? allGames.size() : 0;
                int totalPlaytime = allGames != null ? allGames.stream()
                        .mapToInt(g -> g.getPlaytimeForever() != null ? g.getPlaytimeForever() : 0)
                        .sum() : 0;
                int recentPlaytime = recentGames != null ? recentGames.stream()
                        .mapToInt(g -> g.getPlaytime2Weeks() != null ? g.getPlaytime2Weeks() : 0)
                        .sum() : 0;

                return SteamStats.builder()
                        .totalGames(totalGames)
                        .totalPlaytimeMinutes(totalPlaytime)
                        .recentPlaytimeMinutes(recentPlaytime)
                        .build();
            });
        }).onErrorResume(e -> {
            log.warn("获取全量统计数据失败: {}", e.getMessage());
            return Mono.just(SteamStats.builder()
                    .totalGames(0)
                    .totalPlaytimeMinutes(0)
                    .recentPlaytimeMinutes(0)
                    .build());
        });
    }

    @Override
    public Mono<ValidationResult> validateApiKey(String apiKey, String steamId) {
        return steamApiClient.validateApiKey(apiKey, steamId);
    }

    @Override
    public Mono<AchievementProgress> getAchievementProgress(Long appId) {
        return settingService.getConfig()
                .flatMap(config -> {
                    String steamId = config.getSteamId();
                    return steamApiClient.getPlayerAchievements(steamId, appId);
                })
                .onErrorResume(e -> {
                    log.warn("获取游戏 {} 成就进度失败: {}", appId, e.getMessage());
                    return Mono.just(AchievementProgress.builder()
                            .appId(appId)
                            .achievedCount(0)
                            .totalAchievements(0)
                            .build());
                });
    }

    @Override
    public Mono<BadgeInfo> getBadges() {
        return Mono.zip(
                settingService.getConfig(),
                settingService.getBadgeConfig()
        ).flatMap(tuple -> {
            var config = tuple.getT1();
            var badgeConfig = tuple.getT2();
            int ttl = config.getCacheTtlMinutes() != null ? config.getCacheTtlMinutes() : 10;
            String steamId = config.getSteamId();

            return cacheService.get(CACHE_KEY_BADGES, BadgeInfo.class)
                    .switchIfEmpty(fetchAndCacheBadges(steamId, ttl))
                    .map(badges -> {
                        enrichBadgesWithImageUrl(badges, badgeConfig);
                        return badges;
                    })
                    .onErrorResume(e -> {
                        log.warn("获取徽章失败，尝试返回缓存数据", e);
                        return cacheService.getStale(CACHE_KEY_BADGES, BadgeInfo.class)
                                .map(badges -> {
                                    enrichBadgesWithImageUrl(badges, badgeConfig);
                                    return badges;
                                });
                    });
        });
    }

    /**
     * 为徽章列表填充图片 URL
     */
    private void enrichBadgesWithImageUrl(BadgeInfo badgeInfo, SteamSettingService.BadgeConfig badgeConfig) {
        if (badgeInfo == null || badgeInfo.getBadges() == null || badgeConfig == null) {
            return;
        }
        var mappings = badgeConfig.getBadgeMappings();
        if (mappings == null || mappings.isEmpty()) {
            return;
        }
        for (Badge badge : badgeInfo.getBadges()) {
            for (SteamSettingService.BadgeMapping mapping : mappings) {
                if (badge.getBadgeId() != null && badge.getBadgeId().equals(mapping.getBadgeId())) {
                    if (mapping.getImageUrl() != null && !mapping.getImageUrl().isBlank()) {
                        badge.setImageUrl(mapping.getImageUrl());
                    }
                    if (mapping.getName() != null && !mapping.getName().isBlank()) {
                        badge.setBadgeName(mapping.getName());
                    }
                    break;
                }
            }
        }
    }

    private Mono<BadgeInfo> fetchAndCacheBadges(String steamId, int ttl) {
        return singleflight(CACHE_KEY_BADGES, Mono.defer(() -> {
            log.debug("从 Steam API 获取徽章: steamId={}", steamId);
            return steamApiClient.getBadges(steamId)
                    .doOnNext(badges -> log.debug("徽章获取成功: {} 个徽章", badges.getTotalBadges()))
                    .flatMap(badges ->
                            cacheService.put(CACHE_KEY_BADGES, badges, ttl)
                                    .thenReturn(badges)
                    );
        }));
    }

    // 用于缓存的包装类
    @lombok.Data
    private static class GamesList {
        private List<OwnedGame> games;
    }

    @lombok.Data
    private static class RecentGamesList {
        private List<RecentGame> games;
    }
}
