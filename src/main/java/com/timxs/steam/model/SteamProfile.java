package com.timxs.steam.model;

import lombok.Data;

/**
 * Steam 完整用户资料（包含等级）
 */
@Data
public class SteamProfile {
    
    private PlayerSummary summary;
    
    private Integer steamLevel;

    /**
     * 获取状态文本描述
     */
    public String getStatusText() {
        if (summary == null) {
            return PersonaState.OFFLINE.getText();
        }
        if (summary.isPlaying()) {
            return "正在游玩: " + summary.getGameExtraInfo();
        }
        return summary.getStatusText();
    }

    /**
     * 判断是否正在游戏
     */
    public boolean isPlaying() {
        return summary != null && summary.isPlaying();
    }
}
