package com.timxs.steam.model.vo;

import com.timxs.steam.model.BadgeInfo;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 徽章集合对外响应对象（REST API 用）。
 *
 * <p>字段统一 camelCase；badges 元素映射为 {@link BadgeVO}，
 * 与内部模型 {@link BadgeInfo} 解耦。
 */
@Data
public class BadgeInfoVO {

    /** 徽章列表 */
    private List<BadgeVO> badges;
    /** 玩家总经验值 */
    private Integer playerXp;
    /** 玩家等级 */
    private Integer playerLevel;
    /** 升级所需经验 */
    private Integer xpNeededToLevelUp;
    /** 当前等级所需经验 */
    private Integer xpNeededCurrentLevel;
    /** 徽章总数 */
    private Integer totalBadges;
    /** 游戏徽章数量 */
    private Long gameBadgeCount;
    /** 当前等级进度百分比 */
    private Integer levelProgressPercent;

    public static BadgeInfoVO from(BadgeInfo b) {
        if (b == null) {
            return null;
        }
        BadgeInfoVO vo = new BadgeInfoVO();
        vo.setBadges(b.getBadges() == null ? null
                : b.getBadges().stream().map(BadgeVO::from).collect(Collectors.toList()));
        vo.setPlayerXp(b.getPlayerXp());
        vo.setPlayerLevel(b.getPlayerLevel());
        vo.setXpNeededToLevelUp(b.getXpNeededToLevelUp());
        vo.setXpNeededCurrentLevel(b.getXpNeededCurrentLevel());
        vo.setTotalBadges(b.getTotalBadges());
        vo.setGameBadgeCount(b.getGameBadgeCount());
        vo.setLevelProgressPercent(b.getLevelProgressPercent());
        return vo;
    }
}
