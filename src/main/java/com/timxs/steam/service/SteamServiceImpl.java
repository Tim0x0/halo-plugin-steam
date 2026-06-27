package com.timxs.steam.service;

import com.timxs.steam.cache.CacheService;
import com.timxs.steam.client.SteamApiClient;
import com.timxs.steam.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final String CACHE_KEY_GAME_DETAIL_PREFIX = "steam:game-detail:";

    /**
     * 列表数据（资料/游戏库/最近游玩/徽章）的缓存时长（分钟）。
     * 取一个较长的值：这些数据实际靠定时预热刷新 + getStale 兜底，不受「缓存过期时间」配置影响，
     * 后者只用于按需查询的游戏详情。
     */
    private static final int LIST_CACHE_TTL_MINUTES = 1440;
    private static final int ACHIEVEMENT_FETCH_CONCURRENCY = 3;

    private final SteamApiClient steamApiClient;
    private final CacheService cacheService;
    private final SteamSettingService settingService;
    
    // Singleflight: 防止并发请求重复调用 Steam API
    private final ConcurrentHashMap<String, Mono<?>> inflightRequests = new ConcurrentHashMap<>();

    @Override
    public Mono<SteamProfile> getProfile() {
        return Mono.zip(settingService.getConfig(), settingService.getStoreImageCdn().defaultIfEmpty(""))
                .flatMap(tuple -> {
                    var config = tuple.getT1();
                    String imageCdn = tuple.getT2();
                    String steamId = config.getSteamId();

                    // 尝试从缓存获取
                    return cacheService.getStale(CACHE_KEY_PROFILE, SteamProfile.class)
                            .switchIfEmpty(fetchAndCacheProfile(steamId))
                            .onErrorResume(e -> {
                                log.warn("获取资料失败，尝试返回缓存数据", e);
                                return cacheService.getStale(CACHE_KEY_PROFILE, SteamProfile.class);
                            })
                            .map(profile -> applyAvatarCdn(copyProfile(profile), imageCdn));
                });
    }

    /**
     * 对头像 URL 应用加速域名替换（未配置则原样返回）
     */
    private SteamProfile applyAvatarCdn(SteamProfile profile, String imageCdn) {
        if (profile == null || imageCdn == null || imageCdn.isBlank() || profile.getSummary() == null) {
            return profile;
        }
        PlayerSummary summary = profile.getSummary();
        summary.setAvatar(SteamSettingService.replaceStoreImageDomain(summary.getAvatar(), imageCdn));
        summary.setAvatarMedium(SteamSettingService.replaceStoreImageDomain(summary.getAvatarMedium(), imageCdn));
        summary.setAvatarFull(SteamSettingService.replaceStoreImageDomain(summary.getAvatarFull(), imageCdn));
        return profile;
    }

    private Mono<SteamProfile> fetchAndCacheProfile(String steamId) {
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
                    cacheService.put(CACHE_KEY_PROFILE, profile, LIST_CACHE_TTL_MINUTES)
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
                settingService.getIconImageTemplate(),
                settingService.getStoreImageCdn().defaultIfEmpty(""),
                settingService.getHiddenGameIds()
        ).flatMap(tuple -> {
            var config = tuple.getT1();
            int gamesLimit = tuple.getT2();
            String iconTemplate = tuple.getT3();
            String imageCdn = tuple.getT4();
            var hiddenGameIds = tuple.getT5();
            String steamId = config.getSteamId();

            return cacheService.getStale(CACHE_KEY_GAMES, GamesList.class)
                    .switchIfEmpty(fetchAndCacheGames(steamId))
                    .map(gamesList -> {
                        List<OwnedGame> games = copyOwnedGames(gamesList.getGames());
                        // 为每个游戏设置展示参数（图标模板 + 加速域名）
                        applyDisplaySettings(games, iconTemplate, imageCdn);
                        // 过滤隐藏的游戏
                        List<OwnedGame> filteredGames = filterHiddenGames(games, hiddenGameIds);
                        return paginateAndSort(filteredGames, page, size, sortBy, gamesLimit);
                    })
                    .onErrorResume(e -> {
                        log.warn("获取游戏库失败，尝试返回缓存数据", e);
                        return cacheService.getStale(CACHE_KEY_GAMES, GamesList.class)
                                .map(gamesList -> {
                                    List<OwnedGame> games = copyOwnedGames(gamesList.getGames());
                                    applyDisplaySettings(games, iconTemplate, imageCdn);
                                    List<OwnedGame> filteredGames = filterHiddenGames(games, hiddenGameIds);
                                    return paginateAndSort(filteredGames, page, size, sortBy, gamesLimit);
                                });
                    });
        });
    }

    /**
     * 为游戏列表设置展示参数（图标模板 + 图片加速域名）
     */
    private void applyDisplaySettings(List<? extends OwnedGame> games, String iconTemplate, String imageCdn) {
        if (games != null) {
            games.forEach(game -> {
                game.setIconTemplate(iconTemplate);
                game.setImageCdn(imageCdn);
            });
        }
    }

    /**
     * 解析列表场景的语言与国家码。
     * storeLanguage=auto 时从 Halo 系统语言读取（批量补全无访客上下文）；其余用配置值。
     * @return [language, countryCode]
     */
    private List<OwnedGame> copyOwnedGames(List<OwnedGame> games) {
        if (games == null || games.isEmpty()) {
            return List.of();
        }
        return games.stream()
                .map(this::copyOwnedGame)
                .collect(Collectors.toList());
    }

    private List<RecentGame> copyRecentGames(List<RecentGame> games) {
        if (games == null || games.isEmpty()) {
            return List.of();
        }
        return games.stream()
                .map(this::copyRecentGame)
                .collect(Collectors.toList());
    }

    private OwnedGame copyOwnedGame(OwnedGame source) {
        if (source == null) {
            return null;
        }
        OwnedGame copy = new OwnedGame();
        copyOwnedGameFields(source, copy);
        return copy;
    }

    private RecentGame copyRecentGame(RecentGame source) {
        if (source == null) {
            return null;
        }
        RecentGame copy = new RecentGame();
        copyOwnedGameFields(source, copy);
        copy.setPlaytime2Weeks(source.getPlaytime2Weeks());
        copy.setAchievedCount(source.getAchievedCount());
        copy.setTotalAchievements(source.getTotalAchievements());
        copy.setAchievementsLocked(source.getAchievementsLocked());
        copy.setInLibrary(source.getInLibrary());
        return copy;
    }

    private void copyOwnedGameFields(OwnedGame source, OwnedGame target) {
        target.setAppId(source.getAppId());
        target.setName(source.getName());
        target.setPlaytimeForever(source.getPlaytimeForever());
        target.setImgIconUrl(source.getImgIconUrl());
        target.setImgLogoUrl(source.getImgLogoUrl());
        target.setRtimeLastPlayed(source.getRtimeLastPlayed());
        target.setIconTemplate(source.getIconTemplate());
        target.setImageCdn(source.getImageCdn());
        target.setRealHeaderImage(source.getRealHeaderImage());
        target.setDelisted(source.isDelisted());
    }

    private SteamProfile copyProfile(SteamProfile source) {
        if (source == null) {
            return null;
        }
        SteamProfile copy = new SteamProfile();
        copy.setSteamLevel(source.getSteamLevel());
        copy.setSummary(copyPlayerSummary(source.getSummary()));
        return copy;
    }

    private PlayerSummary copyPlayerSummary(PlayerSummary source) {
        if (source == null) {
            return null;
        }
        PlayerSummary copy = new PlayerSummary();
        copy.setSteamId(source.getSteamId());
        copy.setPersonaName(source.getPersonaName());
        copy.setProfileUrl(source.getProfileUrl());
        copy.setAvatar(source.getAvatar());
        copy.setAvatarMedium(source.getAvatarMedium());
        copy.setAvatarFull(source.getAvatarFull());
        copy.setPersonaState(source.getPersonaState());
        copy.setGameExtraInfo(source.getGameExtraInfo());
        copy.setGameId(source.getGameId());
        copy.setLastLogoff(source.getLastLogoff());
        return copy;
    }

    private Mono<String[]> resolveListLangAndCc() {
        return settingService.getEditorConfig()
                .flatMap(editorConfig -> {
                    String storeLanguage = editorConfig.getStoreLanguage() != null
                            ? editorConfig.getStoreLanguage() : "auto";
                    if ("auto".equals(storeLanguage)) {
                        // 从 Halo 系统语言设置读取
                        return settingService.getInstanceStoreLanguageReactive()
                                .map(steamLang -> {
                                    String cc = SteamSettingService.getCountryCodeOrDefault(steamLang);
                                    return new String[]{steamLang, cc};
                                });
                    }
                    String cc = SteamSettingService.getCountryCodeOrDefault(storeLanguage);
                    return Mono.just(new String[]{storeLanguage, cc});
                });
    }

    /**
     * 用 GetItems 批量补全真实封面与本地化名称。
     * 名称优先用 GetItems（本地化）；GetItems 未返回则保留原名。封面写入 realHeaderImage。
     *
     * @param fallbackHeaders 上次缓存的 appId -> 真实封面映射。GetItems 整体失败或某游戏未返回封面时
     *                        用它兜底，避免一次预热失败把已有封面冲掉；明确下架的游戏不回退（保持占位图）。
     */
    private <T extends OwnedGame> Mono<List<T>> enrichWithStoreItems(List<T> games, String language,
                                                                     String countryCode,
                                                                     Map<Long, String> fallbackHeaders) {
        log.debug("GetItems 补全 {} 款, language={}, cc={}", games != null ? games.size() : 0, language, countryCode);
        if (games == null || games.isEmpty()) {
            return Mono.just(games != null ? games : List.of());
        }
        List<Long> appIds = games.stream()
                .map(OwnedGame::getAppId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (appIds.isEmpty()) {
            return Mono.just(games);
        }
        return steamApiClient.getStoreItems(appIds, language, countryCode)
                .map(itemMap -> {
                    // 空 map 视为请求失败（无法判断是否下架），保守不标下架，封面回退上次缓存
                    boolean requestFailed = itemMap.isEmpty();
                    games.forEach(game -> {
                        StoreItem item = requestFailed ? null : itemMap.get(game.getAppId());
                        boolean delisted = false;
                        if (!requestFailed) {
                            if (item == null) {
                                // GetItems 缺席只表示未知状态，不作为下架/不可用信号
                            } else if (Boolean.FALSE.equals(item.getVisible())) {
                                // 商店不可见（visible=false → 地区锁/已下架/审核限制）
                                delisted = true;
                            } else {
                                // 正常游戏，更新封面和名称
                                if (item.getHeaderImage() != null) {
                                    game.setRealHeaderImage(item.getHeaderImage());
                                }
                                if (item.getName() != null && !item.getName().isBlank()) {
                                    game.setName(item.getName());
                                }
                            }
                        }
                        if (delisted) {
                            // 不可用游戏统一走占位图，不回退封面，与"不可用"徽章语义一致
                            game.setDelisted(true);
                        } else {
                            game.setDelisted(false);
                            // 正常游戏或请求失败：当前没补到封面时回退上次缓存，避免封面被冲掉
                            applyFallbackHeader(game, fallbackHeaders);
                        }
                        // 名称兜底：GetItems 与原始数据都没有名称时，用 AppID 占位，避免显示空白
                        if ((game.getName() == null || game.getName().isBlank()) && game.getAppId() != null) {
                            game.setName("AppID " + game.getAppId());
                        }
                    });
                    return games;
                })
                .onErrorResume(e -> {
                    log.warn("GetItems 补全失败，使用原始数据: {}", e.getMessage());
                    // 请求异常：不标下架，封面回退上次缓存
                    games.forEach(game -> {
                        game.setDelisted(false);
                        applyFallbackHeader(game, fallbackHeaders);
                    });
                    return Mono.just(games);
                });
    }

    /**
     * 封面缺失时回退到上次缓存的真实封面（仅当当前未补到封面、且回退映射里有值时）
     */
    private static void applyFallbackHeader(OwnedGame game, Map<Long, String> fallbackHeaders) {
        if (fallbackHeaders == null || fallbackHeaders.isEmpty() || game.getAppId() == null) {
            return;
        }
        if (game.getRealHeaderImage() != null && !game.getRealHeaderImage().isBlank()) {
            return;
        }
        String prev = fallbackHeaders.get(game.getAppId());
        if (prev != null && !prev.isBlank()) {
            game.setRealHeaderImage(prev);
        }
    }

    /**
     * 从上次缓存的游戏库提取 appId -> 真实封面映射，供 GetItems 失败时兜底
     */
    private Mono<Map<Long, String>> previousGameHeaders() {
        return cacheService.getStale(CACHE_KEY_GAMES, GamesList.class)
                .map(list -> headerMap(list.getGames()))
                .defaultIfEmpty(Map.of());
    }

    /**
     * 从上次缓存的最近游玩提取 appId -> 真实封面映射，供 GetItems 失败时兜底
     */
    private Mono<Map<Long, String>> previousRecentHeaders() {
        return cacheService.getStale(CACHE_KEY_RECENT, RecentGamesList.class)
                .map(list -> headerMap(list.getGames()))
                .defaultIfEmpty(Map.of());
    }

    private static Map<Long, String> headerMap(List<? extends OwnedGame> games) {
        if (games == null || games.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> map = new HashMap<>();
        for (OwnedGame g : games) {
            if (g.getAppId() != null && g.getRealHeaderImage() != null && !g.getRealHeaderImage().isBlank()) {
                map.put(g.getAppId(), g.getRealHeaderImage());
            }
        }
        return map;
    }

    private Mono<GamesList> fetchAndCacheGames(String steamId) {
        return singleflight(CACHE_KEY_GAMES, Mono.defer(() ->
                Mono.zip(settingService.isIncludeFreeGames(), resolveListLangAndCc(), previousGameHeaders())
                        .flatMap(t -> {
                            boolean includeFreeGames = t.getT1();
                            String[] langCc = t.getT2();
                            Map<Long, String> fallback = t.getT3();
                            log.debug("从 Steam API 获取游戏库: steamId={}", steamId);
                            return steamApiClient.getOwnedGames(steamId, true, includeFreeGames)
                                    .flatMap(games -> enrichWithStoreItems(games, langCc[0], langCc[1], fallback))
                                    .map(games -> {
                                        log.debug("游戏库获取成功: {} 款游戏", games.size());
                                        GamesList gamesList = new GamesList();
                                        gamesList.setGames(games);
                                        return gamesList;
                                    })
                                    .flatMap(gamesList ->
                                            cacheService.put(CACHE_KEY_GAMES, gamesList, LIST_CACHE_TTL_MINUTES)
                                                    .thenReturn(gamesList));
                        })
        ));
    }

    private ListResult<OwnedGame> paginateAndSort(List<OwnedGame> allGames, int page, int size, String sortBy, int gamesLimit) {
        // 排序
        List<OwnedGame> sortedGames = sortGames(allGames, sortBy);
        
        // 应用 gamesLimit 限制（0 表示不限制）
        if (gamesLimit > 0 && sortedGames.size() > gamesLimit) {
            sortedGames = sortedGames.subList(0, gamesLimit);
        }

        // 分页
        int safePage = Math.max(page, 1);
        int totalCount = sortedGames.size();
        List<OwnedGame> pagedList = safeSubList(sortedGames, safePage, size);

        return new ListResult<>(safePage, size, totalCount, pagedList);
    }

    /**
     * Follows Halo's ListResult.subList pagination semantics while avoiding int overflow.
     */
    private static <T> List<T> safeSubList(List<T> list, int page, int size) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        int safePage = Math.max(page, 1);
        if (size < 1) {
            return list;
        }

        long total = list.size();
        long fromIndex = safePage == 1 ? 0L : ((long) safePage - 1) * size;
        if (fromIndex >= total) {
            return List.of();
        }

        long toIndex = Math.min(total, fromIndex + size);
        return list.subList((int) fromIndex, (int) toIndex);
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
                settingService.getIconImageTemplate(),
                settingService.getStoreImageCdn().defaultIfEmpty(""),
                settingService.getHiddenGameIds()
        ).flatMap(tuple -> {
            var config = tuple.getT1();
            int configLimit = tuple.getT2();
            String iconTemplate = tuple.getT3();
            String imageCdn = tuple.getT4();
            var hiddenGameIds = tuple.getT5();
            String steamId = config.getSteamId();
            int actualLimit = limit > 0 ? limit : configLimit;

            return cacheService.getStale(CACHE_KEY_RECENT, RecentGamesList.class)
                    .map(RecentGamesList::getGames)
                    .switchIfEmpty(fetchAndCacheRecentGames(steamId))
                    .map(this::copyRecentGames)
                    .flatMap(games -> enrichWithLibraryStatus(games))
                    .map(games -> {
                        // 为每个游戏设置展示参数（图标模板 + 加速域名）
                        applyDisplaySettings(games, iconTemplate, imageCdn);
                        // 过滤隐藏的游戏
                        List<RecentGame> filteredGames = filterHiddenRecentGames(games, hiddenGameIds);
                        return filteredGames.stream().limit(actualLimit).collect(Collectors.toList());
                    })
                    .onErrorResume(e -> {
                        log.warn("获取最近游玩失败，尝试返回缓存数据", e);
                        return cacheService.getStale(CACHE_KEY_RECENT, RecentGamesList.class)
                                .map(RecentGamesList::getGames)
                                .map(this::copyRecentGames)
                                .map(games -> {
                                    applyDisplaySettings(games, iconTemplate, imageCdn);
                                    List<RecentGame> filteredGames = filterHiddenRecentGames(games, hiddenGameIds);
                                    return filteredGames.stream().limit(actualLimit).collect(Collectors.toList());
                                });
                    });
        });
    }

    private Mono<List<RecentGame>> enrichWithLibraryStatus(List<RecentGame> games) {
        if (games == null || games.isEmpty()) {
            return Mono.just(games != null ? games : List.of());
        }

        return cacheService.getStale(CACHE_KEY_GAMES, GamesList.class)
                .map(gamesList -> gamesList.getGames() != null ? gamesList.getGames() : List.<OwnedGame>of())
                .map(ownedGames -> {
                    Set<Long> ownedAppIds = ownedGames.stream()
                            .map(OwnedGame::getAppId)
                            .collect(Collectors.toSet());
                    games.forEach(game -> game.setInLibrary(
                            game.getAppId() != null && ownedAppIds.contains(game.getAppId())
                    ));
                    return games;
                })
                .defaultIfEmpty(games)
                .onErrorResume(e -> {
                    log.warn("获取游戏库状态失败，跳过库外标识", e);
                    return Mono.just(games);
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
        return Flux.fromIterable(games)
                .flatMapSequential(game -> steamApiClient.getPlayerAchievements(steamId, game.getAppId())
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
                        }), ACHIEVEMENT_FETCH_CONCURRENCY)
                .collectList();
    }

    private Mono<List<RecentGame>> fetchAndCacheRecentGames(String steamId) {
        return singleflight(CACHE_KEY_RECENT, Mono.defer(() ->
                Mono.zip(resolveListLangAndCc(), previousRecentHeaders(), settingService.isShowRecentAchievements()).flatMap(t -> {
                    String[] langCc = t.getT1();
                    Map<Long, String> fallback = t.getT2();
                    boolean showAchievements = t.getT3();
                    log.debug("从 Steam API 获取最近游玩: steamId={}", steamId);
                    // 请求全部最近游玩的游戏（不限制数量），显示时再截取
                    return steamApiClient.getRecentlyPlayedGames(steamId, 0)
                            .flatMap(games -> enrichWithStoreItems(games, langCc[0], langCc[1], fallback))
                            .flatMap(games -> {
                                if (showAchievements && !games.isEmpty()) {
                                    return enrichWithAchievements(games, steamId);
                                }
                                return Mono.just(games);
                            })
                            .flatMap(games -> {
                                log.debug("最近游玩获取成功: {} 款游戏", games.size());
                                RecentGamesList gamesList = new RecentGamesList();
                                gamesList.setGames(games);
                                return cacheService.put(CACHE_KEY_RECENT, gamesList, LIST_CACHE_TTL_MINUTES)
                                        .thenReturn(games);
                            });
                })
        ));
    }

    @Override
    public Mono<Void> refreshCache() {
        return cacheService.evictAll()
                .doOnSuccess(v -> {
                    log.info("Steam 缓存已刷新，后台触发预热");
                    // 异步预热，不阻塞刷新响应
                    warmUpActivity().then(warmUpLibrary())
                            .onErrorResume(e -> Mono.empty())
                            .subscribe();
                });
    }

    @Override
    public Mono<Void> warmUpActivity() {
        return settingService.getConfig().flatMap(config -> {
            if (notConfigured(config)) {
                return Mono.empty();
            }
            String steamId = config.getSteamId();
            log.debug("预热活跃组（资料+最近游玩）");
            return Mono.when(
                    fetchAndCacheProfile(steamId).onErrorResume(e -> {
                        log.warn("预热资料失败: {}", e.getMessage());
                        return Mono.empty();
                    }),
                    fetchAndCacheRecentGames(steamId).onErrorResume(e -> {
                        log.warn("预热最近游玩失败: {}", e.getMessage());
                        return Mono.empty();
                    })
            );
        });
    }

    @Override
    public Mono<Void> warmUpLibrary() {
        return settingService.getConfig().flatMap(config -> {
            if (notConfigured(config)) {
                return Mono.empty();
            }
            String steamId = config.getSteamId();
            log.debug("预热库藏组（游戏库+徽章）");
            // 串行执行：先游戏库（含 GetItems 多批），完成后再徽章，避免并发叠加触发 Steam 限流
            return fetchAndCacheGames(steamId).onErrorResume(e -> {
                        log.warn("预热游戏库失败: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .then(fetchAndCacheBadges(steamId).onErrorResume(e -> {
                        log.warn("预热徽章失败: {}", e.getMessage());
                        return Mono.empty();
                    }))
                    .then();
        });
    }

    /**
     * 检查是否未配置 API Key 或 Steam ID（未配置则跳过预热）
     */
    private boolean notConfigured(SteamSettingService.SteamConfig config) {
        return config == null
                || config.getApiKey() == null || config.getApiKey().isBlank()
                || config.getSteamId() == null || config.getSteamId().isBlank();
    }

    @Override
    public Mono<SteamStats> getFullStats() {
        return settingService.getConfig().flatMap(config -> {
            String steamId = config.getSteamId();

            // 获取全量游戏数据（必须）
            Mono<GamesList> gamesMono = cacheService.getStale(CACHE_KEY_GAMES, GamesList.class)
                    .switchIfEmpty(fetchAndCacheGames(steamId))
                    .onErrorResume(e -> {
                        log.warn("获取游戏库失败: {}", e.getMessage());
                        return cacheService.getStale(CACHE_KEY_GAMES, GamesList.class)
                                .defaultIfEmpty(new GamesList());
                    });

            // 获取最近游玩数据（可选，失败返回空列表）
            Mono<List<RecentGame>> recentMono = cacheService.getStale(CACHE_KEY_RECENT, RecentGamesList.class)
                    .map(RecentGamesList::getGames)
                    .switchIfEmpty(fetchAndCacheRecentGames(steamId))
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
            String steamId = config.getSteamId();

            return cacheService.getStale(CACHE_KEY_BADGES, BadgeInfo.class)
                    .switchIfEmpty(fetchAndCacheBadges(steamId))
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

    private Mono<BadgeInfo> fetchAndCacheBadges(String steamId) {
        return singleflight(CACHE_KEY_BADGES, Mono.defer(() -> {
            log.debug("从 Steam API 获取徽章: steamId={}", steamId);
            return steamApiClient.getBadges(steamId)
                    .doOnNext(badges -> log.debug("徽章获取成功: {} 个徽章", badges.getTotalBadges()))
                    .flatMap(badges ->
                            cacheService.put(CACHE_KEY_BADGES, badges, LIST_CACHE_TTL_MINUTES)
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

    @Override
    public Mono<GameDetail> getGameDetail(Long appId, String language) {
        return Mono.zip(settingService.getConfig(), settingService.getEditorConfig())
                .flatMap(tuple -> {
                    var config = tuple.getT1();
                    var editorConfig = tuple.getT2();
                    int ttl = config.getCacheTtlMinutes() != null ? config.getCacheTtlMinutes() : 10;
                    String steamId = config.getSteamId();

                    // 解析最终语言：非 auto 用配置值，auto 用前端传来的语言
                    String storeLanguage = editorConfig.getStoreLanguage() != null
                            ? editorConfig.getStoreLanguage() : "auto";
                    String resolvedLanguage = "auto".equals(storeLanguage)
                            ? (language != null && !language.isBlank() ? language : "english")
                            : storeLanguage;

                    String cacheKey = CACHE_KEY_GAME_DETAIL_PREFIX + appId + ":" + resolvedLanguage;
                    return cacheService.get(cacheKey, GameDetail.class)
                            .switchIfEmpty(fetchAndCacheGameDetail(appId, steamId, ttl, cacheKey, resolvedLanguage));
                });
    }

    private Mono<GameDetail> fetchAndCacheGameDetail(Long appId, String steamId, int ttl, String cacheKey, String language) {
        return singleflight(cacheKey, Mono.defer(() -> {
            log.debug("从 Steam API 获取游戏详情: appId={}", appId);

            // 1. 获取 Store API 基础数据；appdetails success=false（empty）时用 GetItems 判定是否「不可用」
            Mono<GameDetail> detailMono = steamApiClient.getGameDetail(appId, language)
                    .switchIfEmpty(Mono.defer(() -> resolveDelistedOrEmpty(appId, language)));

            // 2. 获取拥有的游戏列表（复用缓存）
            Mono<List<OwnedGame>> gamesMono = cacheService.getStale(CACHE_KEY_GAMES, GamesList.class)
                    .map(GamesList::getGames)
                    .switchIfEmpty(
                        steamApiClient.getOwnedGames(steamId, true, true)
                    )
                    .onErrorResume(e -> {
                        log.debug("获取游戏库失败，跳过个人数据: {}", e.getMessage());
                        return Mono.just(List.of());
                    });

            // 3. 组合 detail 和 games，然后应用 CDN 替换
            return Mono.zip(detailMono, gamesMono)
                    .flatMap(tuple -> {
                        GameDetail detail = tuple.getT1();
                        List<OwnedGame> ownedGames = tuple.getT2();

                        // 不可用游戏：跳过 CDN / 拥有状态 / 成就，直接交给下游统一缓存
                        if (Boolean.TRUE.equals(detail.getDelisted())) {
                            return Mono.just(detail);
                        }

                        // 应用图片 CDN 域名替换（可选操作）
                        return applyCdnIfConfigured(detail)
                                .flatMap(detailWithCdn -> enrichWithOwnedData(detailWithCdn, ownedGames, steamId, appId));
                    })
                    .flatMap(detail ->
                            cacheService.put(cacheKey, detail, ttl)
                                    .thenReturn(detail)
                    );
        }));
    }

    /**
     * 如果配置了 CDN，则替换图片 URL
     */
    private Mono<GameDetail> applyCdnIfConfigured(GameDetail detail) {
        return settingService.getStoreImageCdn()
                .map(cdnDomain -> {
                    if (detail.getHeaderImage() != null) {
                        detail.setHeaderImage(SteamSettingService.replaceStoreImageDomain(detail.getHeaderImage(), cdnDomain));
                    }
                    return detail;
                })
                .switchIfEmpty(Mono.just(detail));  // 没有配置 CDN，直接返回原 detail
    }

    /**
     * 补充拥有状态和成就数据
     */
    private Mono<GameDetail> enrichWithOwnedData(GameDetail detail, List<OwnedGame> ownedGames, String steamId, Long appId) {
        // 检查是否拥有该游戏
        OwnedGame ownedGame = ownedGames.stream()
                .filter(g -> appId.equals(g.getAppId()))
                .findFirst()
                .orElse(null);

        if (ownedGame != null) {
            detail.setOwned(true);
            detail.setPlaytimeForever(ownedGame.getPlaytimeForever());
            detail.setPlaytimeFormatted(OwnedGame.formatPlaytime(
                    ownedGame.getPlaytimeForever() != null ? ownedGame.getPlaytimeForever() : 0));
            detail.setRtimeLastPlayed(ownedGame.getRtimeLastPlayed());
            detail.setLastPlayedFormatted(ownedGame.getLastPlayedFormatted());

            // 获取成就数据
            return steamApiClient.getPlayerAchievements(steamId, appId)
                    .map(progress -> {
                        detail.setAchievedCount(progress.getAchievedCount());
                        detail.setTotalAchievements(progress.getTotalAchievements());
                        detail.setAchievementProgress(progress.getProgressText());
                        return detail;
                    })
                    .onErrorResume(e -> {
                        log.debug("获取游戏 {} 成就失败: {}", appId, e.getMessage());
                        return Mono.just(detail);
                    });
        }

        return Mono.just(detail);
    }

    /**
     * Store appdetails 返回 success=false（取不到详情）时的兜底判定：
     * 用 GetItems 的 visible 判断商店是否「明确不可见」（已下架 / 区域锁 / 审核限制）。
     * <p>仅当 GetItems 明确返回 visible=false 才标记 delisted；item 缺席或请求失败一律不标
     * （可能只是限流或冷门），维持 empty，让上层走 404 → 前端「加载失败 + 重试」。
     * 判定标准与游戏库 / 最近游玩的 {@link #enrichWithStoreItems} 同源。
     */
    private Mono<GameDetail> resolveDelistedOrEmpty(Long appId, String language) {
        String cc = SteamSettingService.getCountryCodeOrDefault(language);
        return steamApiClient.getStoreItems(List.of(appId), language, cc)
                .flatMap(itemMap -> {
                    StoreItem item = itemMap.get(appId);
                    if (item != null && Boolean.FALSE.equals(item.getVisible())) {
                        log.debug("游戏不可用（GetItems visible=false）: appId={}", appId);
                        return Mono.just(GameDetail.builder()
                                .appId(appId)
                                .delisted(true)
                                .build());
                    }
                    return Mono.<GameDetail>empty();
                });
    }
}
