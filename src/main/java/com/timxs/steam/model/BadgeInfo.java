package com.timxs.steam.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Steam 徽章信息（包含徽章列表和玩家经验信息）
 */
@Data
@Builder
public class BadgeInfo {
    
    /**
     * 徽章列表
     */
    private List<Badge> badges;
    
    /**
     * 玩家总经验值
     */
    private Integer playerXp;
    
    /**
     * 玩家等级
     */
    private Integer playerLevel;
    
    /**
     * 升级所需经验
     */
    private Integer xpNeededToLevelUp;
    
    /**
     * 当前等级所需经验
     */
    private Integer xpNeededCurrentLevel;

    /**
     * 获取徽章总数
     */
    public int getTotalBadges() {
        return badges != null ? badges.size() : 0;
    }

    /**
     * 获取游戏徽章数量
     */
    public long getGameBadgeCount() {
        if (badges == null) return 0;
        return badges.stream().filter(Badge::isGameBadge).count();
    }

    /**
     * 获取当前等级进度百分比
     */
    public int getLevelProgressPercent() {
        if (xpNeededCurrentLevel == null || xpNeededCurrentLevel == 0 
                || xpNeededToLevelUp == null) {
            return 0;
        }
        int currentProgress = xpNeededCurrentLevel - xpNeededToLevelUp;
        return (int) ((currentProgress * 100.0) / xpNeededCurrentLevel);
    }
}
