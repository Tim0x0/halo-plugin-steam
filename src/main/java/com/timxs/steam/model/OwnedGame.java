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
     * 图标地址模板：每次读取时由 Service 层重设，无需随缓存保留，故标记 transient
     */
    private transient String iconTemplate;
    /** 图片加速域名（留空表示不加速）：每次读取时重设，无需随缓存保留 */
    private transient String imageCdn;
    /**
     * 真实封面地址（来自 GetItems，原始未加速）。补全较重、仅在拉取时进行，需随缓存保留，
     * 故不标 transient（当前内存缓存存对象引用；将来若改为序列化缓存也不会丢失）。
     */
    private String realHeaderImage;
    /** 商店不可见（仅 GetItems 明确返回 visible=false 时设置；缺席不视为不可用）。需随缓存保留 */
    private boolean delisted;

    /**
     * 获取游戏封面图片 URL (460x215)
     *
     * <p>封面只来自 GetItems 返回的真实地址（新游戏封面无法靠 appId 拼接）；
     * 缺失时返回 null，由前端占位兜底。
     */
    public String getHeaderImageUrl() {
        if (realHeaderImage == null || realHeaderImage.isBlank()) {
            return null;
        }
        return applyCdn(realHeaderImage);
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
        String url = template
                .replace("{appid}", String.valueOf(appId))
                .replace("{hash}", imgIconUrl);
        return applyCdn(url);
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
        String url = template
                .replace("{appid}", String.valueOf(appId))
                .replace("{hash}", imgLogoUrl);
        return applyCdn(url);
    }

    /**
     * 应用图片加速域名替换（未配置则原样返回）
     */
    private String applyCdn(String url) {
        if (imageCdn == null || imageCdn.isBlank()) {
            return url;
        }
        return SteamSettingService.replaceStoreImageDomain(url, imageCdn);
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
