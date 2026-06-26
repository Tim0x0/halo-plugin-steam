package com.timxs.steam.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Steam 商店条目信息（来自 IStoreBrowseService/GetItems）
 *
 * <p>用于批量补全游戏的真实封面地址与本地化名称。新发布的游戏其封面只存在于
 * 带哈希的 store_item_assets 路径下，无法靠 appId 拼接，必须通过 GetItems 获取。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreItem {

    /** 游戏 ID */
    private Long appId;

    /** 本地化游戏名称（按请求语言返回，可能为 null） */
    private String name;

    /** 真实封面图地址（已拼接为完整 URL，可能为 null） */
    private String headerImage;

    /** 商店是否可见（false = 当前地区或状态下不可见，如地区锁、已下架、审核限制等） */
    private Boolean visible;
}
