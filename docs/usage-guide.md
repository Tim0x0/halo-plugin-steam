# Steam 信息展示插件使用教程

本文档详细介绍如何安装、配置和使用 Steam 信息展示插件。

## 目录

- [安装插件](#安装插件)
- [获取 Steam API Key](#获取-steam-api-key)
- [获取 Steam ID](#获取-steam-id)
- [Steam 隐私设置（重要）](#steam-隐私设置重要)
- [基本配置](#基本配置)
- [页面配置](#页面配置)
- [徽章配置](#徽章配置)
- [代理配置](#代理配置)
- [访问 Steam 页面](#访问-steam-页面)
- [主题集成](#主题集成)
- [API 接口](#api-接口)
- [常见问题](#常见问题)

## 安装插件

1. 从 [GitHub Releases](https://github.com/Tim0x0/halo-plugin-steam/releases) 下载最新版本的 JAR 文件
2. 登录 Halo 后台，进入「插件」页面
3. 点击「安装插件」，上传下载的 JAR 文件
4. 安装完成后，点击「启用」按钮

## 获取 Steam API Key

1. 访问 [Steam 开发者页面](https://steamcommunity.com/dev/apikey)
2. 登录你的 Steam 账号
3. 填写域名（可以填写你的博客域名）
4. 同意 Steam Web API 使用条款
5. 点击「注册」获取 API Key
6. 复制生成的 API Key

> ⚠️ 请妥善保管你的 API Key，不要泄露给他人。

## 获取 Steam ID

Steam ID 是一个 17 位数字，格式如 `76561198000000000`。

### 方法一：通过 Steam 个人资料页面

1. 打开 Steam 客户端或网页版
2. 点击你的头像，进入个人资料页面
3. 查看浏览器地址栏，URL 格式为：
   - `https://steamcommunity.com/profiles/76561198000000000` → 数字部分即为 Steam ID
   - `https://steamcommunity.com/id/xxx` → 需要使用方法二转换

### 方法二：使用在线工具

1. 访问 [SteamID.io](https://steamid.io/)
2. 输入你的 Steam 个人资料 URL 或用户名
3. 查找 `steamID64` 字段，即为 17 位 Steam ID

## Steam 隐私设置（重要）

插件需要访问你的 Steam 公开数据，请确保隐私设置正确：

1. 打开 Steam 客户端或网页版
2. 进入「个人资料」→「编辑个人资料」→「隐私设置」
3. 将以下选项设为「公开」：
   - **我的个人资料**：公开
   - **游戏详情**：公开（否则无法获取游戏库和成就数据）
   - **好友列表**：可选
   - **库存**：可选

> ⚠️ 如果「游戏详情」设为私密或仅好友可见，插件将无法获取游戏库、最近游玩和成就数据。

## 基本配置

1. 在 Halo 后台，进入「插件」→「Steam 信息展示」→「设置」
2. 填写以下配置：
   - **Steam API Key**：上一步获取的 API Key
   - **Steam ID**：17 位数字格式的 Steam ID
   - **缓存过期时间**：数据缓存时间（分钟），建议 10-30 分钟
   - **API 请求超时时间**：Steam API 请求超时时间（秒），默认 8 秒，最大 9 秒
3. 点击「验证配置」确认配置有效
4. 点击「保存」

### 关于缓存

- 插件会缓存 Steam API 返回的数据，减少重复请求
- 缓存过期后会自动重新获取数据
- 修改配置后，**已缓存的数据不会立即更新**，需要点击「刷新缓存」或等待缓存过期
- 建议缓存时间设置为 10-30 分钟，过短会增加 API 调用频率

### 关于超时时间

- 主题模板调用（Finder API）受 Halo 框架限制，最大超时 9 秒
- REST API 调用不受此限制
- 如果经常出现超时，可以适当增加超时时间，或配置代理

## 页面配置

在「页面配置」标签页中，可以自定义 Steam 页面的显示效果：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| 页面标题 | Steam 页面的标题 | Steam 游戏库 |
| 每页显示数量 | 游戏库分页每页显示的游戏数量 | 12 |
| 游戏库总数量限制 | 最多显示的游戏数量，设为 0 则不限制 | 50 |
| 最近游玩显示数量 | 最近游玩区域显示的游戏数量 | 5 |
| 显示最近游玩成就进度 | 开启后显示成就完成进度 | 关闭 |
| 点击游戏卡片跳转 Steam 商城 | 开启后点击游戏卡片跳转到 Steam 商城 | 关闭 |
| 包含免费游戏 | 开启后游戏库会包含免费游戏 | 开启 |
| 隐藏的游戏 | 从游戏库中隐藏指定的游戏 | 空 |

### 关于成就进度

- 开启「显示最近游玩成就进度」后，会为每个最近游玩的游戏额外请求成就 API
- 这会**显著增加页面加载时间**（每个游戏约 0.5-1 秒）
- 成就进度显示格式：`🏆 已完成/总数`
- 如果游戏成就因隐私设置不公开，显示 `🔒`
- 如果游戏没有成就系统，不显示任何内容

### 关于隐藏游戏

- 如果你想隐藏某些游戏（如不想显示的游戏），可以在「隐藏的游戏」中添加游戏 ID
- 支持两种格式：
  - 直接输入游戏 ID：`730`
  - 粘贴完整 Steam 商店链接：`https://store.steampowered.com/app/730/`
- 添加的游戏不会在游戏库和统计中显示
- 隐藏的游戏不影响数据统计，只是在前端不显示

### 关于免费游戏

- Steam 免费游戏（如 CS2、Dota 2、Team Fortress 2 等）默认包含在游戏库中
- 关闭此选项后，只显示付费购买的游戏
- 修改后需要刷新缓存才能生效

## 徽章配置

Steam 徽章分为两种：
- **游戏徽章**：通过收集游戏卡牌合成，插件自动显示 🎮 图标
- **系统徽章**：社区徽章、活动徽章等，插件默认显示 🏅 图标

如果你想为系统徽章显示实际的图片，可以在「徽章配置」中添加映射：

1. 进入「徽章配置」标签页
2. 点击「添加映射」
3. 填写以下信息：
   - **Badge ID**：徽章 ID
   - **徽章名称**：便于识别（可选）
   - **图片 URL**：徽章图片地址

### 如何获取 Badge ID

1. 打开你的 Steam 个人资料页面
2. 点击「徽章」进入徽章列表
3. 点击某个徽章，查看浏览器地址栏
4. URL 格式为 `https://steamcommunity.com/id/xxx/badges/13`，最后的数字 `13` 就是 Badge ID

### 如何获取徽章图片 URL

1. 在 Steam 徽章页面，找到你想要的徽章
2. 右键点击徽章图片，选择「复制图片地址」
3. 将地址粘贴到「图片 URL」字段

### 常见系统徽章 ID 参考

| Badge ID | 徽章名称 |
|----------|----------|
| 1 | 社区大使 (Community Ambassador) |
| 2 | Pillar of Community |
| 13 | Steam 年度回顾 (Steam Replay) |
| 17 | Steam 大奖 (Steam Awards) |
| 21 | Steam 冬促 |
| 23 | Spring Cleaning |

> 💡 不同年份的活动徽章可能有不同的 Badge ID，请以实际页面 URL 为准。

## 热力图与时长追踪

v0.2.0 版本新增了游戏时长热力图功能，类似于 GitHub 贡献墙，可视化展示你每天的游戏投入。

### 开启时长追踪

要使用热力图功能，必须先开启时长追踪：

1. 进入「统计」配置标签页
2. 开启「启用游戏时长追踪」开关
3. 点击保存

**⚠️ 重要提示**：
- 插件通过定时任务（每小时）记录游戏时长变化。
- **首次开启后，需等待至少 1 小时**才会产生第一个数据点。
- 你也可以点击下方的「手动追踪时长」按钮立即执行一次追踪（用于测试）。
- Steam API 不提供历史每日数据，因此**热力图只能从开启本功能后开始记录**。

### 配置热力图

在「统计」标签页中，你可以自定义热力图的显示效果：

- **在页面显示热力图**：开启后，热力图将显示在 Steam 页面顶部统计栏下方。
- **显示天数**：设置热力图显示的时间跨度（30 - 730 天）。
- **颜色主题**：内置 4 种配色风格：
  - Steam 蓝色（默认）
  - GitHub 绿色
  - 火焰橙色
  - 紫色梦幻
- **显示图例**：是否显示热力图的颜色图例（Less、More 等）
- **数据保留天数**：设置历史数据的保留时长（默认 365 天），过期数据会自动清理。

## 代理配置

如果你的服务器无法直接访问 Steam API，可以配置代理：

### HTTP 代理

1. 在「代理配置」中启用「Steam API 代理」
2. 选择「HTTP 代理」
3. 填写代理主机和端口

### 自定义 API 地址

1. 在「代理配置」中启用「Steam API 代理」
2. 选择「自定义 API 地址」
3. 填写第三方 Steam API 代理服务地址

### 图片加速

Steam 图片服务器在国内访问可能较慢，可以配置 CDN 加速：

- **游戏封面图加速地址**：替换游戏封面图的 CDN 地址
  - 默认：`https://cdn.cloudflare.steamstatic.com/steam/apps/{appid}/header.jpg`
  - 支持占位符：`{appid}` 会被替换为游戏 ID
  
- **游戏图标加速地址**：替换游戏图标的 CDN 地址
  - 默认：`https://media.steampowered.com/steamcommunity/public/images/apps/{appid}/{hash}.jpg`
  - 支持占位符：`{appid}` 游戏 ID，`{hash}` 图标哈希值

> 💡 如果你有自己的图片代理服务，可以将地址替换为你的代理地址。

## 访问 Steam 页面

插件安装并配置完成后，访问 `/steam` 即可查看 Steam 信息页面。

页面包含以下内容：
- 用户资料卡片（头像、昵称、在线状态、等级、徽章）
- 统计数据（游戏总数、总游玩时长、最近两周游玩时长）
- 最近游玩的游戏
- 游戏库列表（支持分页）

## 主题集成

如果你想在主题中集成 Steam 数据，可以使用 Finder API：

```html
<!-- 获取用户资料 -->
<th:block th:with="profile=${steamFinder.getProfile()}">
    <img th:src="${profile?.summary?.avatarFull}" alt="头像">
    <span th:text="${profile?.summary?.personaName}">用户名</span>
    <span th:text="${profile?.statusText}">在线状态</span>
</th:block>

<!-- 获取最近游玩 -->
<th:block th:with="recentGames=${steamFinder.getRecentGames(5)}">
    <div th:if="${recentGames != null}" th:each="game : ${recentGames}">
        <img th:src="${game.headerImageUrl}" th:alt="${game.name}">
        <span th:text="${game.name}">游戏名</span>
        <span th:text="${game.playtime2WeeksFormatted}">游玩时长</span>
    </div>
</th:block>

<!-- 获取统计数据 -->
<th:block th:with="stats=${steamFinder.getStats()}">
    <span th:if="${stats != null}" th:text="${stats.totalGames}">游戏总数</span>
    <span th:if="${stats != null}" th:text="${stats.totalPlaytimeFormatted}">总游玩时长</span>
</th:block>

<!-- 获取徽章信息 -->
<th:block th:with="badges=${steamFinder.getBadges()}">
    <span th:if="${badges != null}" th:text="${badges.playerLevel}">等级</span>
    <span th:if="${badges != null}" th:text="${badges.totalBadges}">徽章数</span>
</th:block>
```

### 错误处理

Finder API 在请求失败时返回 `null`，建议在模板中做判空处理：

```html
<th:block th:with="profile=${steamFinder.getProfile()}">
    <div th:if="${profile != null}">
        <!-- 正常显示内容 -->
    </div>
    <div th:if="${profile == null}">
        <!-- 显示错误提示或占位内容 -->
        <span>Steam 数据加载失败</span>
    </div>
</th:block>
```

## API 接口

插件提供了 REST API 接口，可以获取 Steam 数据和热力图数据。

### 获取游戏列表

**接口**: `GET /apis/api.steam.timxs.com/v1alpha1/games`

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页数量，默认 20 |
| sortBy | string | 否 | 排序字段：`playtime_forever`（默认）或 `name` |

**示例**:
```bash
# 获取第一页游戏，按游戏时长排序
GET /apis/api.steam.timxs.com/v1alpha1/games?page=1&size=20

# 获取游戏列表，按名称排序
GET /apis/api.steam.timxs.com/v1alpha1/games?sortBy=name
```

### 获取热力图记录

**接口**: `GET /apis/api.steam.timxs.com/v1alpha1/heatmap/records`

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| startDate | string | 是 | 开始日期，格式 `yyyy-MM-dd` |
| endDate | string | 是 | 结束日期，格式 `yyyy-MM-dd` |
| appId | long | 否 | 游戏 ID，不传则查询所有游戏 |
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页大小，默认 365 |

**返回数据**:
```json
{
  "items": [
    {
      "spec": {
        "steamId": "76561198000000000",
        "date": "2024-01-15",
        "appId": 730,
        "gameName": "Counter-Strike 2",
        "playtimeMinutes": 120,
        "startTime": "2024-01-15T10:00:00Z",
        "endTime": "2024-01-15T12:00:00Z"
      }
    }
  ],
  "page": 1,
  "size": 365,
  "total": 100
}
```

**示例**:
```bash
# 查询 CS2 (appId=730) 在 2024-01-15 这天的数据
GET /apis/api.steam.timxs.com/v1alpha1/heatmap/records?appId=730&startDate=2024-01-15&endDate=2024-01-15

# 查询某游戏在某个日期范围内的数据
GET /apis/api.steam.timxs.com/v1alpha1/heatmap/records?appId=730&startDate=2024-01-01&endDate=2024-01-31

# 查询所有游戏在某个月的数据
GET /apis/api.steam.timxs.com/v1alpha1/heatmap/records?startDate=2024-01-01&endDate=2024-01-31
```

### 管理 API

管理 API 需要管理员权限，用于后台管理操作。

**接口基础路径**: `/apis/console.api.steam.timxs.com/v1alpha1`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/verify` | POST | 验证 Steam API 配置（用于设置页面的验证功能） |
| `/refresh` | POST | 刷新 Steam 数据缓存 |
| `/heatmap/track` | POST | 手动触发游戏时长追踪 |
| `/heatmap/cleanup` | POST | 手动触发热力图数据清理 |

#### 刷新缓存

**接口**: `POST /apis/console.api.steam.timxs.com/v1alpha1/refresh`

**说明**: 立即清空 Steam 数据缓存，下次请求时会重新从 Steam API 获取数据。

**返回**:
```json
{
  "success": true,
  "message": "Steam 数据缓存已清空"
}
```

#### 手动追踪时长

**接口**: `POST /apis/console.api.steam.timxs.com/v1alpha1/heatmap/track`

**说明**: 手动触发一次游戏时长追踪（用于测试），不需要等待定时任务。

**返回**:
```json
{
  "success": true,
  "count": 5,
  "message": "追踪完成，处理了 5 款游戏"
}
```

#### 手动清理数据

**接口**: `POST /apis/console.api.steam.timxs.com/v1alpha1/heatmap/cleanup`

**说明**: 手动触发清理过期的热力图数据（根据「数据保留天数」配置）。

**返回**:
```json
{
  "success": true,
  "count": 100,
  "message": "清理完成，删除了 100 条记录"
}
```

### 其他接口

插件还提供以下接口（详见代码或 Swagger 文档）：

| 接口 | 说明 |
|------|------|
| `GET /apis/api.steam.timxs.com/v1alpha1/profile` | 获取用户资料 |
| `GET /apis/api.steam.timxs.com/v1alpha1/recent` | 获取最近游玩 |
| `GET /apis/api.steam.timxs.com/v1alpha1/stats` | 获取统计数据 |
| `GET /apis/api.steam.timxs.com/v1alpha1/achievements/{appid}` | 获取游戏成就 |
| `GET /apis/api.steam.timxs.com/v1alpha1/badges` | 获取徽章信息 |

## 常见问题

### Q: 页面显示「部分 Steam 数据加载失败」

**可能原因：**
- Steam API 服务器连接超时
- API Key 或 Steam ID 配置错误
- 服务器网络无法访问 Steam API
- Steam 隐私设置不是公开

**解决方法：**
1. 检查 Steam 隐私设置，确保「我的个人资料」和「游戏详情」为公开
2. 检查插件配置中的 API Key 和 Steam ID 是否正确
3. 点击「验证配置」确认配置有效
4. 如果服务器在国内，尝试配置代理
5. 适当增加 API 请求超时时间（最大 9 秒）

### Q: 成就进度显示🔒图标

这表示该游戏的成就数据因隐私设置不公开。

**解决方法：**
1. 打开 Steam 隐私设置
2. 将「游戏详情」设为「公开」
3. 刷新插件缓存

### Q: 最近游玩没有显示成就进度

**可能原因：**
1. 未开启「显示最近游玩成就进度」选项
2. 该游戏没有成就系统（如部分独立游戏）
3. 成就数据因隐私设置不公开（会显示🔒）

### Q: 游戏库中没有某些游戏

**可能原因：**
1. 「包含免费游戏」选项未开启（免费游戏不显示）
2. Steam 隐私设置中「游戏详情」不是公开
3. 数据还在缓存中，未更新

**解决方法：**
1. 检查「包含免费游戏」选项
2. 确认 Steam 隐私设置
3. 点击「刷新缓存」重新获取数据

### Q: 徽章图片不显示

**可能原因：**
1. 未在「徽章配置」中添加对应的徽章映射
2. 图片 URL 错误或无法访问
3. 该徽章是游戏徽章（游戏徽章显示🎮图标，不支持自定义图片）

**解决方法：**
1. 确认是系统徽章而非游戏徽章
2. 检查 Badge ID 是否正确
3. 检查图片 URL 是否可以在浏览器中正常打开

### Q: 页面加载很慢

**可能原因：**
1. Steam API 响应慢
2. 开启了「显示最近游玩成就进度」
3. 图片加载慢

**解决方法：**
1. 配置 Steam API 代理
2. 关闭「显示最近游玩成就进度」或减少最近游玩显示数量
3. 配置图片 CDN 加速
4. 适当增加缓存时间，减少 API 调用频率

### Q: Steam API Key 被封禁

Steam API Key 有调用频率限制。如果短时间内请求过多，可能会被临时封禁。

**解决方法：**
1. 增加缓存过期时间，减少 API 调用频率
2. 等待一段时间后自动解封
3. 如果持续被封，可以重新申请一个 API Key
