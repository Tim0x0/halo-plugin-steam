package com.timxs.steam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Steam 用户基本资料
 */
@Data
public class PlayerSummary {
    
    @JsonProperty("steamid")
    private String steamId;
    
    @JsonProperty("personaname")
    private String personaName;
    
    @JsonProperty("profileurl")
    private String profileUrl;
    
    @JsonProperty("avatar")
    private String avatar;
    
    @JsonProperty("avatarmedium")
    private String avatarMedium;
    
    @JsonProperty("avatarfull")
    private String avatarFull;
    
    @JsonProperty("personastate")
    private Integer personaState;
    
    @JsonProperty("gameextrainfo")
    private String gameExtraInfo;
    
    @JsonProperty("gameid")
    private Long gameId;
    
    @JsonProperty("lastlogoff")
    private Long lastLogoff;

    /**
     * 获取在线状态文本
     */
    public String getStatusText() {
        if (personaState == null) {
            return PersonaState.OFFLINE.getText();
        }
        return PersonaState.getTextByCode(personaState);
    }

    /**
     * 判断是否正在游戏中
     */
    public boolean isPlaying() {
        return gameExtraInfo != null && !gameExtraInfo.isEmpty();
    }
}
