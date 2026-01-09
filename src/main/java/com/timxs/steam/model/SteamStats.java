package com.timxs.steam.model;

import lombok.Builder;
import lombok.Data;

/**
 * Steam 统计数据
 */
@Data
@Builder
public class SteamStats {
    
    private int totalGames;
    private int totalPlaytimeMinutes;
    private int recentPlaytimeMinutes;
    
    /**
     * 获取格式化的总游玩时长
     */
    public String getTotalPlaytimeFormatted() {
        return formatPlaytime(totalPlaytimeMinutes);
    }
    
    /**
     * 获取格式化的最近两周游玩时长
     */
    public String getRecentPlaytimeFormatted() {
        return formatPlaytime(recentPlaytimeMinutes);
    }
    
    private String formatPlaytime(int minutes) {
        if (minutes <= 0) {
            return "0 小时";
        }
        int hours = minutes / 60;
        if (hours >= 1000) {
            return String.format("%,d 小时", hours);
        }
        return hours + " 小时 " + (minutes % 60) + " 分钟";
    }
}
