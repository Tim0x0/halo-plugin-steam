package com.timxs.steam.model.vo;

import com.timxs.steam.model.Badge;
import lombok.Data;

/**
 * 徽章对外响应对象（REST API 用）。
 *
 * <p>与内部模型 {@link Badge} 解耦：字段统一 camelCase 命名，
 * 不暴露 Steam 原始的 snake_case（badgeid/completion_time/appid 等）。
 */
@Data
public class BadgeVO {

    /** 徽章 ID */
    private Integer badgeId;
    /** 徽章等级 */
    private Integer level;
    /** 完成时间戳（秒） */
    private Long completionTime;
    /** 经验值 */
    private Integer xp;
    /** 稀有度 */
    private Integer scarcity;
    /** 关联游戏 AppID（游戏徽章才有） */
    private Long appId;
    /** 社区物品 ID */
    private String communityItemId;
    /** 边框颜色（1 = 闪卡/foil） */
    private Integer borderColor;
    /** 徽章图片地址（来自徽章配置映射，未配置则为 null） */
    private String imageUrl;
    /** 徽章名称（来自徽章配置映射） */
    private String badgeName;
    /** 格式化的完成时间（按服务器时区） */
    private String completionTimeFormatted;
    /** 是否为游戏徽章 */
    private Boolean gameBadge;
    /** 是否为闪卡徽章 */
    private Boolean foil;
    /** 显示名称（游戏徽章/社区徽章的兜底名称） */
    private String displayName;

    public static BadgeVO from(Badge b) {
        if (b == null) {
            return null;
        }
        BadgeVO vo = new BadgeVO();
        vo.setBadgeId(b.getBadgeId());
        vo.setLevel(b.getLevel());
        vo.setCompletionTime(b.getCompletionTime());
        vo.setXp(b.getXp());
        vo.setScarcity(b.getScarcity());
        vo.setAppId(b.getAppId());
        vo.setCommunityItemId(b.getCommunityItemId());
        vo.setBorderColor(b.getBorderColor());
        vo.setImageUrl(b.getImageUrl());
        vo.setBadgeName(b.getBadgeName());
        vo.setCompletionTimeFormatted(b.getCompletionTimeFormatted());
        vo.setGameBadge(b.isGameBadge());
        vo.setFoil(b.isFoil());
        vo.setDisplayName(b.getDisplayName());
        return vo;
    }
}
