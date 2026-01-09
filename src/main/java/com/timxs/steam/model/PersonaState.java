package com.timxs.steam.model;

/**
 * Steam 用户在线状态枚举
 */
public enum PersonaState {
    OFFLINE(0, "离线"),
    ONLINE(1, "在线"),
    BUSY(2, "忙碌"),
    AWAY(3, "离开"),
    SNOOZE(4, "打盹"),
    LOOKING_TO_TRADE(5, "想要交易"),
    LOOKING_TO_PLAY(6, "想要游戏");

    private final int code;
    private final String text;

    PersonaState(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    /**
     * 根据状态码获取状态枚举
     */
    public static PersonaState fromCode(int code) {
        for (PersonaState state : values()) {
            if (state.code == code) {
                return state;
            }
        }
        return OFFLINE;
    }

    /**
     * 根据状态码获取状态文本
     */
    public static String getTextByCode(int code) {
        return fromCode(code).getText();
    }
}
