package com.timxs.steam.model.vo;

import com.timxs.steam.model.PlayerSummary;
import lombok.Data;

/**
 * 玩家摘要对外响应对象（REST API 用）。
 *
 * <p>字段统一 camelCase 命名，对外暴露稳定契约（内部 {@link PlayerSummary}
 * 用 snake_case 接收 Steam 原始数据）。
 */
@Data
public class PlayerSummaryVO {

    private String steamId;
    private String personaName;
    private String profileUrl;
    private String avatar;
    private String avatarMedium;
    private String avatarFull;
    private Integer personaState;
    private String gameExtraInfo;
    private Long gameId;
    private Long lastLogoff;

    public static PlayerSummaryVO from(PlayerSummary s) {
        if (s == null) {
            return null;
        }
        PlayerSummaryVO vo = new PlayerSummaryVO();
        vo.setSteamId(s.getSteamId());
        vo.setPersonaName(s.getPersonaName());
        vo.setProfileUrl(s.getProfileUrl());
        vo.setAvatar(s.getAvatar());
        vo.setAvatarMedium(s.getAvatarMedium());
        vo.setAvatarFull(s.getAvatarFull());
        vo.setPersonaState(s.getPersonaState());
        vo.setGameExtraInfo(s.getGameExtraInfo());
        vo.setGameId(s.getGameId());
        vo.setLastLogoff(s.getLastLogoff());
        return vo;
    }
}
