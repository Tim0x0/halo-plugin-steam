package com.timxs.steam.service;

import com.timxs.steam.client.SteamApiClient;
import com.timxs.steam.model.DailyPlaytimeRecord;
import com.timxs.steam.model.OwnedGame;
import com.timxs.steam.model.PlaytimeSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.router.selector.FieldSelector;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static run.halo.app.extension.index.query.QueryFactory.*;

/**
 * 游戏时长追踪服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaytimeTrackingServiceImpl implements PlaytimeTrackingService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private final ReactiveExtensionClient client;
    private final SteamApiClient steamApiClient;
    private final SteamSettingService settingService;

    @Override
    public Mono<Integer> trackAllGames() {
        return settingService.getConfig()
            .flatMap(config -> {
                String steamId = config.getSteamId();
                if (steamId == null || steamId.isBlank()) {
                    log.warn("Steam ID 未配置，无法追踪游戏时长");
                    return Mono.error(new RuntimeException("Steam ID 未配置，请在插件设置中配置 Steam ID"));
                }
                
                log.debug("追踪游戏时长: steamId={}", steamId);
                
                return settingService.isIncludeFreeGames()
                    .defaultIfEmpty(true)
                    .flatMap(includeFreeGames -> {
                        log.debug("获取游戏列表: includeFreeGames={}", includeFreeGames);
                        return steamApiClient.getOwnedGames(steamId, true, includeFreeGames);
                    })
                    .doOnSuccess(games -> log.debug("Steam API 返回 {} 款游戏", games != null ? games.size() : 0))
                    .flatMapMany(Flux::fromIterable)
                    .flatMap(game -> processGame(steamId, game)
                        .onErrorResume(e -> {
                            log.warn("处理游戏失败: game={}, appId={}, error={}", 
                                game.getName(), game.getAppId(), e.getMessage());
                            return Mono.just(0);  // 失败返回 0
                        })
                    )
                    .reduce(0, Integer::sum)  // 累加所有返回值
                    .doOnSuccess(count -> {
                        if (count == 0) {
                            log.warn("追踪完成，但未处理任何游戏。可能原因：1) 游戏时长为0；2) Steam API 返回空列表；3) 所有游戏处理失败");
                        } else {
                            log.info("追踪完成，处理 {} 款游戏", count);
                        }
                    })
                    .doOnError(e -> log.warn("追踪游戏时长失败: {}", e.getMessage()));
            })
            .onErrorResume(e -> {
                log.warn("追踪游戏时长失败: {}", e.getMessage());
                return Mono.error(e);  // 直接传递原始异常,不再包装
            });
    }

    /**
     * 处理单个游戏的时长追踪
     * @return 返回 1 表示处理成功,0 表示跳过
     */
    private Mono<Integer> processGame(String steamId, OwnedGame game) {
        Long appId = game.getAppId();
        Integer currentPlaytime = game.getPlaytimeForever();
        
        if (currentPlaytime == null || currentPlaytime == 0) {
            log.trace("跳过游戏: game={}, appId={}, playtime=0", game.getName(), appId);
            return Mono.just(0);  // 返回 0 表示跳过
        }

        return findLatestSnapshot(steamId, appId)
            .flatMap(snapshot -> {
                // 找到历史快照，计算差值
                Integer lastPlaytime = snapshot.getSpec().getPlaytimeForever();
                int delta = currentPlaytime - lastPlaytime;
                
                if (delta > 0) {
                    // 有新增时长，创建每日记录
                    return createDailyRecords(steamId, game, delta, snapshot.getSpec().getSnapshotTime())
                        .then(updateSnapshot(snapshot, currentPlaytime, Instant.now()))
                        .thenReturn(1);  // 返回 1 表示处理成功
                } else {
                    // 无变化，只更新快照时间
                    return updateSnapshot(snapshot, currentPlaytime, Instant.now())
                        .thenReturn(1);  // 返回 1 表示处理成功
                }
            })
            .switchIfEmpty(Mono.defer(() -> {
                // 首次发现，创建初始快照
                log.debug("首次发现游戏: {} ({}), 创建初始快照", game.getName(), appId);
                return createInitialSnapshot(steamId, game)
                    .thenReturn(1);  // 返回 1 表示处理成功
            }));
    }

    /**
     * 查找最新的快照
     */
    private Mono<PlaytimeSnapshot> findLatestSnapshot(String steamId, Long appId) {
        var fieldSelector = FieldSelector.of(
            and(
                equal("spec.steamId", steamId),
                equal("spec.appId", appId.toString())
            )
        );
        
        var listOptions = new run.halo.app.extension.ListOptions();
        listOptions.setFieldSelector(fieldSelector);
        
        return client.listAll(PlaytimeSnapshot.class, listOptions, 
                org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.desc("spec.snapshotTime")
                ))
            .next();
    }

    /**
     * 创建初始快照
     */
    private Mono<Void> createInitialSnapshot(String steamId, OwnedGame game) {
        log.debug("创建初始快照: game={}, appId={}, playtime={}min", 
            game.getName(), game.getAppId(), game.getPlaytimeForever());
        
        PlaytimeSnapshot snapshot = new PlaytimeSnapshot();
        snapshot.setMetadata(new run.halo.app.extension.Metadata());
        snapshot.getMetadata().setGenerateName("snapshot-");
        
        PlaytimeSnapshot.PlaytimeSnapshotSpec spec = new PlaytimeSnapshot.PlaytimeSnapshotSpec();
        spec.setSteamId(steamId);
        spec.setAppId(game.getAppId());
        spec.setGameName(game.getName());
        spec.setPlaytimeForever(game.getPlaytimeForever());
        spec.setSnapshotTime(Instant.now());
        snapshot.setSpec(spec);
        
        return client.create(snapshot).then();
    }

    /**
     * 更新快照
     */
    private Mono<Void> updateSnapshot(PlaytimeSnapshot snapshot, Integer newPlaytime, Instant newTime) {
        snapshot.getSpec().setPlaytimeForever(newPlaytime);
        snapshot.getSpec().setSnapshotTime(newTime);
        return client.update(snapshot).then();
    }

    /**
     * 创建每日记录（处理跨天情况）
     */
    private Mono<Void> createDailyRecords(String steamId, OwnedGame game, int deltaMinutes, Instant lastSnapshotTime) {
        Instant now = Instant.now();
        LocalDate lastDate = LocalDate.ofInstant(lastSnapshotTime, ZONE_ID);
        LocalDate currentDate = LocalDate.ofInstant(now, ZONE_ID);
        
        if (lastDate.equals(currentDate)) {
            // 同一天，直接创建记录
            return createOrUpdateDailyRecord(steamId, game, currentDate, deltaMinutes, lastSnapshotTime, now);
        } else {
            // 跨天，拆分记录
            return splitCrossDayRecords(steamId, game, deltaMinutes, lastSnapshotTime, now, lastDate, currentDate);
        }
    }

    /**
     * 处理跨天记录（加权平均分配算法）
     * 健壮性改进：支持跨越任意天数，按时间窗口覆盖比例分配时长，确保总时长不丢失。
     * 防御性设计：增加最大跨度限制，防止极端情况下循环过多。
     */
    private Mono<Void> splitCrossDayRecords(String steamId, OwnedGame game, int totalMinutes,
                                            Instant startTime, Instant endTime,
                                            LocalDate startDate, LocalDate endDate) {
        // 1. 计算总时间窗口的秒数（分母）
        long totalWindowSeconds = java.time.Duration.between(startTime, endTime).getSeconds();

        // 防御性检查：如果窗口时间异常，直接记在最后一天
        if (totalWindowSeconds <= 0) {
            return createOrUpdateDailyRecord(steamId, game, endDate, totalMinutes, startTime, endTime);
        }

        // 2. 熔断检查：防止跨度过大导致死循环或数据库压力过大
        // 如果跨度超过 60 天，不再细分，直接丢弃每日明细（避免热力图单日数据爆炸），但外层逻辑仍会更新总时长快照
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween > 60) {
            log.warn("跨天跨度过大 ({} 天), 跳过每日数据生成，仅更新总时长快照: steamId={}, appId={}, delta={}min",
                daysBetween, steamId, game.getAppId(), totalMinutes);
            return Mono.empty();
        }

        List<Mono<Void>> saveTasks = new java.util.ArrayList<>();
        int allocatedMinutes = 0; // 记录已分配的分钟数，用于最后一天兜底

        // 3. 遍历从开始日期到结束日期的每一天
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            // 计算当前天 00:00 和 24:00 的 Instant
            Instant dayStart = currentDate.atStartOfDay(ZONE_ID).toInstant();
            Instant nextDayStart = currentDate.plusDays(1).atStartOfDay(ZONE_ID).toInstant();

            // 4. 计算当前天与总时间窗口的 [交集] 时长
            // 交集开始时间 = max(总窗口开始, 当天开始)
            Instant overlapStart = (dayStart.isBefore(startTime)) ? startTime : dayStart;
            // 交集结束时间 = min(总窗口结束, 当天结束)
            Instant overlapEnd = (nextDayStart.isAfter(endTime)) ? endTime : nextDayStart;

            long overlapSeconds = java.time.Duration.between(overlapStart, overlapEnd).getSeconds();

            // 只有当有交集时才处理
            if (overlapSeconds > 0) {
                int minutesForCurrentDay;

                // 5. 分配时长
                if (currentDate.equals(endDate)) {
                    // 【关键步骤】最后一天：使用剩余的所有时长，确保总和守恒
                    minutesForCurrentDay = totalMinutes - allocatedMinutes;
                } else {
                    // 中间天/第一天：按时间比例分配
                    // 公式：总增量 * (当天占窗口的秒数 / 总窗口秒数)
                    double ratio = (double) overlapSeconds / totalWindowSeconds;
                    minutesForCurrentDay = (int) Math.round(totalMinutes * ratio);

                    // 累计已分配时长
                    allocatedMinutes += minutesForCurrentDay;
                }

                // 6. 记录任务
                if (minutesForCurrentDay > 0) {
                    saveTasks.add(createOrUpdateDailyRecord(
                        steamId,
                        game,
                        currentDate,
                        minutesForCurrentDay,
                        overlapStart,
                        overlapEnd
                    ));
                }
            }

            // 移动到下一天
            currentDate = currentDate.plusDays(1);
        }

        // 7. 串行执行所有保存操作，避免数据库并发压力
        return Flux.concat(saveTasks).then();
    }

    /**
     * 创建或更新每日记录
     */
    private Mono<Void> createOrUpdateDailyRecord(String steamId, OwnedGame game, LocalDate date,
                                                 int minutes, Instant startTime, Instant endTime) {
        if (minutes <= 0) {
            return Mono.empty();
        }
        
        String dateStr = date.format(DATE_FORMATTER);
        
        // 查找是否已存在该日期该游戏的记录
        var fieldSelector = FieldSelector.of(
            and(
                equal("spec.steamId", steamId),
                equal("spec.date", dateStr),
                equal("spec.appId", game.getAppId().toString())
            )
        );
        
        var listOptions = new run.halo.app.extension.ListOptions();
        listOptions.setFieldSelector(fieldSelector);
        
        return client.listAll(DailyPlaytimeRecord.class, listOptions, null)
            .next()
            .flatMap(existing -> {
                // 已存在，累加时长
                existing.getSpec().setPlaytimeMinutes(
                    existing.getSpec().getPlaytimeMinutes() + minutes
                );
                existing.getSpec().setEndTime(endTime);
                return client.update(existing);
            })
            .switchIfEmpty(Mono.defer(() -> {
                // 不存在，创建新记录
                DailyPlaytimeRecord record = new DailyPlaytimeRecord();
                record.setMetadata(new run.halo.app.extension.Metadata());
                record.getMetadata().setGenerateName("daily-");
                
                DailyPlaytimeRecord.DailyPlaytimeRecordSpec spec = 
                    new DailyPlaytimeRecord.DailyPlaytimeRecordSpec();
                spec.setSteamId(steamId);
                spec.setDate(dateStr);
                spec.setAppId(game.getAppId());
                spec.setGameName(game.getName());
                spec.setPlaytimeMinutes(minutes);
                spec.setStartTime(startTime);
                spec.setEndTime(endTime);
                record.setSpec(spec);
                
                return client.create(record);
            }))
            .then();
    }

    @Override
    public Mono<ListResult<DailyPlaytimeRecord>> queryDailyRecords(String startDate, String endDate,
                                                                    Long appId, int page, int size) {
        return settingService.getConfig()
            .flatMap(config -> {
                String steamId = config.getSteamId();
                
                // 构建查询条件
                var query = appId != null ?
                    and(
                        equal("spec.steamId", steamId),
                        greaterThanOrEqual("spec.date", startDate),
                        lessThanOrEqual("spec.date", endDate),
                        equal("spec.appId", appId.toString())
                    ) :
                    and(
                        equal("spec.steamId", steamId),
                        greaterThanOrEqual("spec.date", startDate),
                        lessThanOrEqual("spec.date", endDate)
                    );
                
                var fieldSelector = FieldSelector.of(query);
                var listOptions = new run.halo.app.extension.ListOptions();
                listOptions.setFieldSelector(fieldSelector);
                
                // 使用 Halo 的分页 API
                var pageRequest = run.halo.app.extension.PageRequestImpl.of(
                    page, 
                    size,
                    org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("spec.date")
                    )
                );
                
                return client.listBy(DailyPlaytimeRecord.class, listOptions, pageRequest)
                    .map(listResult -> new ListResult<>(
                        listResult.getPage(),
                        listResult.getSize(),
                        listResult.getTotal(),
                        listResult.getItems()
                    ));
            });
    }

    @Override
    public Mono<Integer> cleanupExpiredData() {
        return settingService.getHeatmapRetentionDays()
            .defaultIfEmpty(365)
            .flatMap(retentionDays -> {
                LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
                String cutoffDateStr = cutoffDate.format(DATE_FORMATTER);
                
                log.debug("清理过期数据: cutoffDate={}, retentionDays={}", cutoffDateStr, retentionDays);
                
                var fieldSelector = FieldSelector.of(
                    lessThan("spec.date", cutoffDateStr)
                );
                
                var listOptions = new run.halo.app.extension.ListOptions();
                listOptions.setFieldSelector(fieldSelector);
                
                return client.listAll(DailyPlaytimeRecord.class, listOptions, null)
                    .flatMap(record -> client.delete(record)
                        .onErrorResume(e -> {
                            log.warn("删除记录失败: name={}, error={}", 
                                record.getMetadata().getName(), e.getMessage());
                            return Mono.empty();
                        })
                    )
                    .count()
                    .map(Long::intValue)
                    .doOnSuccess(count -> {
                        if (count > 0) {
                            log.info("清理完成，删除 {} 条过期记录", count);
                        } else {
                            log.debug("无过期记录需要清理");
                        }
                    });
            })
            .defaultIfEmpty(0)
            .onErrorResume(e -> {
                log.warn("清理过期数据失败: {}", e.getMessage());
                return Mono.just(0);
            });
    }
}
