package com.timxs.steam.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 游戏成就进度
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementProgress {
    
    private Long appId;
    private String gameName;
    private Integer achievedCount;
    private Integer totalAchievements;
    
    /**
     * 获取成就完成百分比
     */
    public Integer getPercentage() {
        if (totalAchievements == null || totalAchievements == 0) {
            return 0;
        }
        return (int) Math.round((double) achievedCount / totalAchievements * 100);
    }
    
    /**
     * 获取格式化的进度文本 (如 "15/30")
     */
    public String getProgressText() {
        return (achievedCount != null ? achievedCount : 0) + "/" + (totalAchievements != null ? totalAchievements : 0);
    }
}
