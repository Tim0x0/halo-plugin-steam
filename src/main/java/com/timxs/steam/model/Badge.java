package com.timxs.steam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Steam 徽章
 */
@Data
public class Badge {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    @JsonProperty("badgeid")
    private Integer badgeId;
    
    @JsonProperty("level")
    private Integer level;
    
    @JsonProperty("completion_time")
    private Long completionTime;
    
    @JsonProperty("xp")
    private Integer xp;
    
    @JsonProperty("scarcity")
    private Integer scarcity;
    
    @JsonProperty("appid")
    private Long appId;
    
    @JsonProperty("communityitemid")
    private String communityItemId;
    
    @JsonProperty("border_color")
    private Integer borderColor;

    /**
     * 获取格式化的完成时间
     */
    public String getCompletionTimeFormatted() {
        if (completionTime == null || completionTime == 0) {
            return null;
        }
        LocalDateTime dateTime = Instant.ofEpochSecond(completionTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        return dateTime.format(DATE_FORMATTER);
    }

    /**
     * 是否为游戏徽章
     */
    public boolean isGameBadge() {
        return appId != null && appId > 0;
    }
    
    /**
     * 是否为闪卡徽章
     */
    public boolean isFoil() {
        return borderColor != null && borderColor == 1;
    }
    
    /**
     * 获取徽章显示名称
     */
    public String getDisplayName() {
        if (isGameBadge()) {
            return "游戏徽章 #" + appId;
        }
        return "社区徽章 #" + badgeId;
    }
}
