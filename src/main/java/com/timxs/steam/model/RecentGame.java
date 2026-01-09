package com.timxs.steam.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Steam 最近游玩的游戏
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RecentGame extends OwnedGame {
    
    @JsonProperty("playtime_2weeks")
    private Integer playtime2Weeks;
    
    /**
     * 已解锁成就数量
     */
    private Integer achievedCount;
    
    /**
     * 总成就数量
     */
    private Integer totalAchievements;
    
    /**
     * 成就数据是否不可用（403 等错误）
     */
    private Boolean achievementsLocked;

    /**
     * 获取最近两周游玩时长的格式化字符串
     */
    public String getPlaytime2WeeksFormatted() {
        return formatPlaytime(playtime2Weeks != null ? playtime2Weeks : 0);
    }
    
    /**
     * 获取成就进度文本 (如 "15/30")，锁定时返回 null
     */
    public String getAchievementProgressText() {
        if (achievementsLocked != null && achievementsLocked) {
            return null;
        }
        if (totalAchievements == null || totalAchievements == 0) {
            return null;
        }
        return (achievedCount != null ? achievedCount : 0) + "/" + totalAchievements;
    }
    
    /**
     * 成就是否被锁定（不可用）
     */
    public boolean isAchievementsLocked() {
        return achievementsLocked != null && achievementsLocked;
    }
    
    /**
     * 是否有成就数据
     */
    @JsonIgnore
    public boolean hasAchievements() {
        return totalAchievements != null && totalAchievements > 0;
    }
}
