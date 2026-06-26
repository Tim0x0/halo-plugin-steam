package com.timxs.steam.model.vo;

import com.timxs.steam.model.SteamProfile;
import lombok.Data;

/**
 * 用户资料对外响应对象（REST API 用）。
 */
@Data
public class ProfileVO {

    private PlayerSummaryVO summary;
    private Integer steamLevel;
    private String statusText;
    private Boolean playing;

    public static ProfileVO from(SteamProfile p) {
        if (p == null) {
            return null;
        }
        ProfileVO vo = new ProfileVO();
        vo.setSummary(PlayerSummaryVO.from(p.getSummary()));
        vo.setSteamLevel(p.getSteamLevel());
        vo.setStatusText(p.getStatusText());
        vo.setPlaying(p.isPlaying());
        return vo;
    }
}
