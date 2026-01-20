package com.timxs.steam.scheduler;

import com.timxs.steam.service.PlaytimeTrackingService;
import com.timxs.steam.service.SteamSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Steam 游戏时长追踪定时任务
 * 每小时执行一次，追踪所有游戏的时长变化
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaytimeTrackingScheduler {

    private final PlaytimeTrackingService trackingService;
    private final SteamSettingService settingService;

    /**
     * 每小时的第 59 分钟执行（使用系统默认时区）
     * Cron 表达式: 秒 分 时 日 月 周
     * 注意: 使用系统默认时区，确保服务器时区配置正确
     */
    @Scheduled(cron = "0 59 * * * ?", zone = "Asia/Shanghai")
    public void trackPlaytime() {
        log.debug("定时任务触发：检查热力图功能是否启用");
        
        // 检查功能是否启用
        settingService.isHeatmapEnabled()
            .filter(enabled -> enabled)
            .flatMap(enabled -> {
                log.info("热力图功能已启用，开始执行游戏时长追踪任务");
                return trackingService.trackAllGames();
            })
            .doOnSuccess(count -> {
                if (count != null) {
                    log.info("游戏时长追踪完成，处理了 {} 款游戏", count);
                }
            })
            .doOnError(error -> log.error("游戏时长追踪失败", error))
            .onErrorResume(e -> Mono.empty())
            .subscribe();
    }

    /**
     * 每天凌晨 3 点清理过期数据（使用系统默认时区）
     * 注意: 使用系统默认时区，确保服务器时区配置正确
     */
    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Shanghai")
    public void cleanupExpiredData() {
        log.debug("定时任务触发：检查热力图功能是否启用");
        
        // 检查功能是否启用
        settingService.isHeatmapEnabled()
            .filter(enabled -> enabled)
            .flatMap(enabled -> {
                log.info("热力图功能已启用，开始执行过期数据清理任务");
                return trackingService.cleanupExpiredData();
            })
            .doOnSuccess(count -> {
                if (count != null) {
                    log.info("过期数据清理完成，清理了 {} 条记录", count);
                }
            })
            .doOnError(error -> log.error("过期数据清理失败", error))
            .onErrorResume(e -> Mono.empty())
            .subscribe();
    }
}
