package com.timxs.steam.service;

import com.timxs.steam.model.DailyPlaytimeRecord;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

/**
 * 游戏时长追踪服务
 */
public interface PlaytimeTrackingService {

    /**
     * 追踪所有游戏的时长变化
     * 
     * @return 处理的游戏数量
     */
    Mono<Integer> trackAllGames();

    /**
     * 查询每日游戏时长记录
     * 
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate 结束日期 (yyyy-MM-dd)
     * @param appId 游戏 ID（可选，为 null 则查询所有游戏）
     * @param page 页码
     * @param size 每页大小
     * @return 每日记录列表
     */
    Mono<ListResult<DailyPlaytimeRecord>> queryDailyRecords(
        String startDate, 
        String endDate, 
        Long appId,
        int page, 
        int size
    );

    /**
     * 清理过期数据
     * 
     * @return 清理的记录数量
     */
    Mono<Integer> cleanupExpiredData();
}
