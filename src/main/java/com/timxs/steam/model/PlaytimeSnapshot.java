package com.timxs.steam.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import java.time.Instant;

/**
 * 游戏时长快照
 * 用于记录每次查询时的累计时长，用于计算差值
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "steam.timxs.com", version = "v1alpha1",
    kind = "PlaytimeSnapshot", plural = "playtimesnapshots",
    singular = "playtimesnapshot")
public class PlaytimeSnapshot extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private PlaytimeSnapshotSpec spec;

    @Data
    public static class PlaytimeSnapshotSpec {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Steam ID")
        private String steamId;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "游戏 ID")
        private Long appId;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "游戏名称")
        private String gameName;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "累计时长（分钟）")
        private Integer playtimeForever;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "快照时间")
        private Instant snapshotTime;
    }
}
