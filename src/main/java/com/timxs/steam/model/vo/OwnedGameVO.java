package com.timxs.steam.model.vo;

import com.timxs.steam.model.OwnedGame;
import lombok.Data;

/**
 * 游戏库对外响应对象（REST API 用）。
 *
 * <p>与内部模型 {@link OwnedGame} 解耦：字段统一 camelCase 命名，
 * 不暴露内部渲染字段（iconTemplate、imageCdn）。
 */
@Data
public class OwnedGameVO {

    /** 游戏 ID */
    private Long appId;
    /** 游戏名称 */
    private String name;
    /** 总游玩时长（原始分钟数） */
    private Integer playtimeForever;
    /** 格式化后的总游玩时长（如 "640h 43m"） */
    private String playtimeFormatted;
    /** Steam 图标 hash（原始素材） */
    private String imgIconUrl;
    /** 拼接好的游戏图标地址 */
    private String iconUrl;
    /** 最后游玩时间戳（原始，秒） */
    private Long rtimeLastPlayed;
    /** 格式化后的最后游玩日期（如 "2026-06-20"） */
    private String lastPlayedFormatted;
    /** 封面地址（已应用 CDN 加速，缺失时为 null） */
    private String headerImageUrl;
    /** 原始封面地址（来自 Steam 商店，未加速；配置 CDN 时与 headerImageUrl 不同） */
    private String realHeaderImage;
    /** 商店是否不可见（地区锁/已下架/审核限制） */
    private Boolean delisted;

    public static OwnedGameVO from(OwnedGame g) {
        if (g == null) {
            return null;
        }
        OwnedGameVO vo = new OwnedGameVO();
        fill(vo, g);
        return vo;
    }

    /** 填充 OwnedGame 公共字段，供子类 VO 复用 */
    protected static void fill(OwnedGameVO vo, OwnedGame g) {
        vo.setAppId(g.getAppId());
        vo.setName(g.getName());
        vo.setPlaytimeForever(g.getPlaytimeForever());
        vo.setPlaytimeFormatted(g.getPlaytimeFormatted());
        vo.setImgIconUrl(g.getImgIconUrl());
        vo.setIconUrl(g.getIconUrl());
        vo.setRtimeLastPlayed(g.getRtimeLastPlayed());
        vo.setLastPlayedFormatted(g.getLastPlayedFormatted());
        vo.setHeaderImageUrl(g.getHeaderImageUrl());
        vo.setRealHeaderImage(g.getRealHeaderImage());
        vo.setDelisted(g.isDelisted());
    }
}
