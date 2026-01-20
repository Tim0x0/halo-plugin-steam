package com.timxs.steam.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import java.time.Instant;

/**
 * 每日游戏时长记录
 * 用于记录每天每个游戏玩了多久，用于生成热力图
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "steam.halo.run", version = "v1alpha1",
    kind = "DailyPlaytimeRecord", plural = "dailyplaytimerecords",
    singular = "dailyplaytimerecord")
public class DailyPlaytimeRecord extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private DailyPlaytimeRecordSpec spec;

    @Data
    public static class DailyPlaytimeRecordSpec {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Steam ID")
        private String steamId;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "日期 (yyyy-MM-dd)")
        private String date;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "游戏 ID")
        private Long appId;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "游戏名称")
        private String gameName;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "当日游玩时长（分钟）")
        private Integer playtimeMinutes;

        @Schema(description = "开始时间")
        private Instant startTime;

        @Schema(description = "结束时间")
        private Instant endTime;
    }
}
