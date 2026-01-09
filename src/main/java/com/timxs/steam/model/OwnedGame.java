package com.timxs.steam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.timxs.steam.service.SteamSettingService;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Steam 拥有的游戏
 */
@Data
public class OwnedGame {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @JsonProperty("appid")
    private Long appId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("playtime_forever")
    private Integer playtimeForever;
    
    @JsonProperty("img_icon_url")
    private String imgIconUrl;
    
    @JsonProperty("img_logo_url")
    private String imgLogoUrl;
    
    @JsonProperty("rtime_last_played")
    private Long rtimeLastPlayed;
    
    /**
     * 实例级别的 URL 模板（由 Service 层设置）
     */
    private transient String headerTemplate;
    private transient String iconTemplate;

    /**
     * 获取游戏封面图片 URL (460x215)
     */
    public String getHeaderImageUrl() {
        String template = (headerTemplate != null && !headerTemplate.isBlank()) 
                ? headerTemplate : SteamSettingService.DEFAULT_HEADER_TEMPLATE;
        return template.replace("{appid}", String.valueOf(appId));
    }
    
    /**
     * 获取游戏图标 URL (32x32)
     */
    public String getIconUrl() {
        if (imgIconUrl == null || imgIconUrl.isBlank()) {
            return null;
        }
        String template = (iconTemplate != null && !iconTemplate.isBlank())
                ? iconTemplate : SteamSettingService.DEFAULT_ICON_TEMPLATE;
        return template
                .replace("{appid}", String.valueOf(appId))
                .replace("{hash}", imgIconUrl);
    }
    
    /**
     * 获取游戏 Logo URL
     */
    public String getLogoUrl() {
        if (imgLogoUrl == null || imgLogoUrl.isBlank()) {
            return null;
        }
        String template = (iconTemplate != null && !iconTemplate.isBlank())
                ? iconTemplate : SteamSettingService.DEFAULT_ICON_TEMPLATE;
        return template
                .replace("{appid}", String.valueOf(appId))
                .replace("{hash}", imgLogoUrl);
    }

    /**
     * 获取格式化的游玩时长
     */
    public String getPlaytimeFormatted() {
        return formatPlaytime(playtimeForever != null ? playtimeForever : 0);
    }

    /**
     * 获取格式化的最后游玩日期 (yyyy-MM-dd)
     */
    public String getLastPlayedFormatted() {
        if (rtimeLastPlayed == null || rtimeLastPlayed == 0) {
            return null;
        }
        LocalDate date = Instant.ofEpochSecond(rtimeLastPlayed)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return date.format(DATE_FORMATTER);
    }

    /**
     * 格式化游玩时长（分钟转换为 Xh Ym 格式）
     */
    public static String formatPlaytime(int minutes) {
        if (minutes <= 0) {
            return "0m";
        }
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (hours > 0) {
            return hours + "h " + mins + "m";
        }
        return mins + "m";
    }

}
