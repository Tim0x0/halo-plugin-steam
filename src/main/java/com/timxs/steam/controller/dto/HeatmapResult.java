package com.timxs.steam.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 热力图操作结果 DTO
 */
@Data
@AllArgsConstructor
public class HeatmapResult {
    /** 操作是否成功 */
    private boolean success;
    /** 处理的数量 */
    private int count;
    /** 结果消息 */
    private String message;
}
