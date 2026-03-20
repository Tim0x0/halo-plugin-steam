package com.timxs.steam.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameDetail {
    private Long appId;
    private String name;
    private String headerImage;       // 封面图
    private String shortDescription;  // 简短描述
    private String developers;        // 开发商（逗号分隔）
    private String publishers;        // 发行商
    private String genres;            // 类型标签
    private Boolean isFree;           // 是否免费
    private String priceFormatted;    // 格式化价格（如 "¥79"）
    private String releaseDate;       // 发售日期
    private String storeUrl;          // Steam 商店链接

    // 个人数据（仅当用户拥有该游戏时填充）
    private Boolean owned;                // 是否拥有
    private Integer playtimeForever;      // 总时长（分钟）
    private String playtimeFormatted;     // 格式化时长
    private Long rtimeLastPlayed;         // 最后游玩时间戳
    private String lastPlayedFormatted;   // 格式化最后游玩时间
    private Integer achievedCount;        // 已解锁成就
    private Integer totalAchievements;    // 总成就数
    private String achievementProgress;   // 成就进度文本（如 "12/50"）
}
