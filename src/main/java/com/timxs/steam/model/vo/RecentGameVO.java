package com.timxs.steam.model.vo;

import com.timxs.steam.model.RecentGame;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 最近游玩对外响应对象（REST API 用）。
 *
 * <p>在 {@link OwnedGameVO} 基础上增加最近两周时长、成就进度与库外标识。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RecentGameVO extends OwnedGameVO {

    /** 最近两周游玩时长（原始分钟数） */
    private Integer playtime2Weeks;
    /** 格式化后的最近两周时长 */
    private String playtime2WeeksFormatted;
    /** 成就进度文本（如 "15/30"），锁定或无成就时为 null */
    private String achievementProgressText;
    /** 成就数据是否不可用（隐私设置等导致） */
    private Boolean achievementsLocked;
    /** 是否存在于当前游戏库（false 表示库外游戏） */
    private Boolean inLibrary;

    public static RecentGameVO from(RecentGame g) {
        if (g == null) {
            return null;
        }
        RecentGameVO vo = new RecentGameVO();
        fill(vo, g);
        vo.setPlaytime2Weeks(g.getPlaytime2Weeks());
        vo.setPlaytime2WeeksFormatted(g.getPlaytime2WeeksFormatted());
        vo.setAchievementProgressText(g.getAchievementProgressText());
        vo.setAchievementsLocked(g.getAchievementsLocked());
        vo.setInLibrary(g.getInLibrary());
        return vo;
    }
}
