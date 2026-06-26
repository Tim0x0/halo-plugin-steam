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
- [热力图与时长追踪](#热力图与时长追踪)
- [编辑器配置](#编辑器配置)
- [代理配置](#代理配置)
- [访问 Steam 页面](#访问-steam-页面)
- [主题集成](#主题集成)
- [API 接口](#api-接口)
- [常见问题](#常见问题)
- [附录](#附录)
  - [Steam API 技术参考](steam-api-reference.md) - 详细的接口说明和代理服务搭建指南

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
   - **活跃信息刷新间隔**：后台刷新「资料 + 最近游玩」的间隔（分钟），默认 10
   - **库藏信息刷新间隔**：后台刷新「游戏库 + 徽章」的间隔（分钟），默认 60
   - **缓存过期时间**：仅用于「按需查询」的游戏卡片详情（分钟），默认 10
   - **API 请求超时时间**：后台拉取数据 / 验证配置时的超时（秒），默认 8，可设 5-60
3. 点击「验证配置」确认配置有效
4. 点击「保存」

### 关于数据刷新（重要）

插件采用**后台定时预热**机制：后台按设定的间隔自动从 Steam 拉取数据并存入缓存，页面访问时**只读缓存**——所以页面打开很快、不会因 Steam 慢而卡住或超时。

- **活跃信息刷新间隔**（默认 10 分钟）：刷新「资料 + 最近游玩」，这两类变化快、成本低，建议设短；
- **库藏信息刷新间隔**（默认 60 分钟）：刷新「游戏库 + 徽章」，游戏库较大、变化慢，建议设长；
- 插件启动后会很快自动拉取一次（刚启动那几秒页面可能显示「加载中」，属正常）；
- 点「刷新缓存」会清空并立即在后台重新拉取一次。

### 关于缓存

- 「缓存过期时间」**只作用于「按需查询」的数据**（主要是文章里插入的游戏卡片详情）；页面列表的更新由上面的「刷新间隔」控制，不受此项影响。
- 拉取失败时会**保留上一次的旧数据**，所以只要成功拉过一次，之后即使 Steam 临时故障，页面也始终有数据可看（顶多是旧的）。

### 关于超时时间

- 「API 请求超时时间」用于**后台拉取数据**和**验证 API Key** 时；页面读的是已缓存数据，**不受此项影响**。
- 国内服务器若拉取经常超时，建议**配置 API 代理**（见下文），或适当增大超时时间。

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

- 开启「显示最近游玩成就进度」后，后台在刷新「最近游玩」缓存时会为每个最近游玩的游戏额外请求成就 API
- 成就结果会随最近游玩列表一起缓存；页面访问缓存命中时不会为每个游戏重新请求成就 API
- 这会增加后台预热耗时和 Steam API 调用量，最近游玩游戏很多时建议适当调大「活跃信息刷新间隔」
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
- **ECharts JS 地址**：热力图依赖 ECharts 渲染，默认使用 BootCDN（`https://cdn.bootcdn.net/ajax/libs/echarts/5.4.3/echarts.min.js`），可替换为自有服务器或其他 CDN 地址
- **数据保留天数**：设置历史数据的保留时长（默认 365 天），过期数据会自动清理。

## 编辑器配置

在「编辑器配置」标签页中，可以配置富文本编辑器中 Steam 游戏卡片的行为。

### Steam 游戏卡片

插件为 Halo 富文本编辑器提供了 Steam 游戏卡片扩展。在编辑文章时，可以插入一个游戏卡片来展示 Steam 游戏的详细信息（封面、描述、价格、类型标签、个人游玩数据等）。

**使用方法：**
1. 在富文本编辑器中，使用游戏卡片扩展
2. 输入游戏的 App ID 或 Steam 商店链接
3. 卡片会自动获取并展示游戏信息

### 显示语言

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| 暗色模式选择器 | 用于游戏卡片自适应主题检测暗色模式的 CSS 选择器 | `html.dark` |
| 游戏显示语言 | 游戏名称、描述与价格货币的语言 | 自动 |

**语言选项说明：**

- **自动**：分两种场景 ——
  - **页面列表**（`/steam` 的游戏库、最近游玩）：按 **Halo 后台「系统 → 博客设置 → 基本设置 → 首选语言」** 显示。因为这些数据在后端批量获取、所有访客共享一份缓存，无法按单个访客切换，所以统一取你给 Halo 站点设置的语言。后台设的是「简体中文」，这里就是中文名。
  - **编辑器游戏卡片**：按 **访客浏览器语言** 显示（这条数据按每个访客的请求单独获取）。
- **指定语言**：选择具体语言后，所有场景都用该语言，不受站点设置/浏览器影响。

> 💡 「自动」模式下，只要你在 Halo 后台把「首选语言」设成「简体中文」，页面列表就会显示中文名，无需手动指定。读不到站点语言时回退简体中文。

| 语言 | 名称/描述语言 | 价格货币（仅详情卡片） |
|------|----------|----------|
| 简体中文 | 简体中文 | 人民币 (¥) |
| 繁體中文 | 繁体中文 | 新台币 (NT$) |
| English | 英文 | 美元 ($) |
| 日本語 | 日文 | 日元 (¥) |
| 한국어 | 韩文 | 韩元 (₩) |
| Deutsch | 德文 | 欧元 (€) |
| Français | 法文 | 欧元 (€) |

> 💡 修改语言设置后，页面列表需要刷新缓存或等待下一轮后台预热；编辑器游戏卡片等按需详情会按「缓存过期时间」重新拉取。

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

> 💡 如果你想自己搭建 API 代理服务，请参阅 [Steam API 技术参考](steam-api-reference.md)，其中包含了详细的接口说明和实现示例。

### 图片加速

Steam 图片服务器在国内访问可能较慢，可以配置 CDN 加速：

- **游戏图片加速域名**：填入你的 CDN 域名（如 `https://my-cdn.com`）后，封面、图标、头像的图片地址会把域名替换为该 CDN（仅替换域名，路径不变）；留空则使用 Steam 原始地址。

> ⚠️ 封面、图标、头像来自 Steam 不同的图片域名，但插件**只替换域名、保留原路径**。配置反代/CDN 时保持 Steam 原始路径（如 `store_item_assets/`、`steamcommunity/` 等）按路径分流即可，**填一个加速域名就够**，无需为每类图单独配置。

> 💡 游戏封面现已自动通过 Steam 官方接口（`IStoreBrowseService/GetItems`）获取真实地址 —— 新发布游戏的封面带内容哈希、无法靠 AppID 拼接，因此旧的「游戏封面图加速地址」模板已弃用。如需加速，统一使用上面的「游戏图片加速域名」即可。

> ⚠️ `logoUrl`（游戏 Logo）因 Steam 接口已基本不再下发 Logo 哈希而弃用，主题中请改用 `headerImageUrl`（封面）。

### 关于「不可用」徽章

游戏库和最近游玩里的游戏卡片，如果某款游戏在 Steam 商店**已不可见**（GetItems 接口返回 `visible=false`，原因可能是地区锁、已下架、审核限制等，Steam 接口无法细分），会显示一个灰色的「**不可用**」徽章，封面则用占位图。

- 这只代表"在当前地区/状态下访问不到"，**不一定是全球下架**。
- 此判定依赖 GetItems 接口，网络异常时不会误标（保守跳过）。

## 访问 Steam 页面

插件安装并配置完成后，访问 `/steam` 即可查看 Steam 信息页面。

页面包含以下内容：
- 用户资料卡片（头像、昵称、在线状态、等级、徽章）
- 统计数据（游戏总数、总游玩时长、最近两周游玩时长）
- 最近游玩的游戏
- 游戏库列表（支持分页）

## 主题集成

如果你想在主题中集成 Steam 数据，可以使用 Finder API。

### 可用方法

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `steamFinder.getProfile()` | `SteamProfile` | 获取用户资料（头像、昵称、状态、等级） |
| `steamFinder.getRecentGames(limit)` | `List<RecentGame>` | 获取最近游玩的游戏 |
| `steamFinder.getOwnedGames(page, size)` | `ListResult<OwnedGame>` | 获取游戏库（分页，按游玩时长排序） |
| `steamFinder.getStats()` | `SteamStats` | 获取统计数据（游戏总数、总时长等） |
| `steamFinder.getBadges()` | `BadgeInfo` | 获取徽章信息（等级、徽章列表） |

### 示例代码

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

<!-- 获取游戏库（分页） -->
<th:block th:with="gameList=${steamFinder.getOwnedGames(1, 12)}">
    <div th:if="${gameList != null}" th:each="game : ${gameList.items}">
        <img th:src="${game.headerImageUrl}" th:alt="${game.name}">
        <span th:text="${game.name}">游戏名</span>
        <span th:text="${game.playtimeFormatted}">总游玩时长</span>
    </div>
    <!-- 分页信息 -->
    <span th:if="${gameList != null}" th:text="'第 ' + ${gameList.page} + ' 页，共 ' + ${gameList.totalPages} + ' 页'">分页</span>
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

### 游戏对象字段

最近游玩与游戏库的游戏对象共享下列字段（最近游玩额外多「最近两周时长」与「成就」）：

| 字段 | 说明 |
|------|------|
| `appId` / `name` | 游戏 ID / 名称（名称优先取商店接口的本地化结果） |
| `imgIconUrl` / `iconUrl` | Steam 原始图标 hash / 拼接并套用 CDN 后的图标 URL |
| `headerImageUrl` / `realHeaderImage` | 展示封面 URL（已套用 CDN）/ Steam 原始真实封面 URL；封面缺失时为 `null`，模板需兜底 |
| `playtimeForever` / `playtimeFormatted` | 总游玩时长（原始分钟数 / 格式化文本） |
| `rtimeLastPlayed` / `lastPlayedFormatted` | 最后游玩时间戳 / 格式化日期 |
| `delisted` | GetItems 明确返回 `visible=false` 时为 `true`，表示当前地区/状态下商店不可见；GetItems 缺席不会被判为不可用 |
| `inLibrary` | （仅最近游玩）是否存在于当前游戏库缓存；`false` 表示库外游戏，冷启动或游戏库预热失败时可能暂不标识 |
| `achievementProgressText` / `achievementsLocked` | （仅最近游玩）成就进度文本 / 是否因隐私设置锁定 |

> 💡 `headerImageUrl` 为 `null` 不一定代表不可用，也可能是 GetItems 未返回封面、首次拉取失败且没有旧封面可回退。建议模板对 `headerImageUrl` 判空显示占位图，并只在 `delisted=true` 时显示「不可用」标识。

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

### 页面模板变量

如果使用插件提供的 `/steam` 页面模板，以下变量可直接在模板中使用：

| 变量 | 类型 | 说明 |
|------|------|------|
| `title` | `String` | 页面标题（来自配置） |
| `games` | `UrlContextListResult<OwnedGame>` | 游戏库列表（带分页 URL） |
| `gamesLimit` | `int` | 游戏库总数量限制 |
| `recentGamesLimit` | `int` | 最近游玩显示数量 |
| `enableGameLink` | `boolean` | 是否启用游戏链接跳转 |
| `showHeatmap` | `boolean` | 是否显示热力图 |
| `heatmapDays` | `int` | 热力图显示天数 |
| `heatmapColorTheme` | `String` | 热力图颜色主题（steam/github/fire/purple） |
| `heatmapShowLegend` | `boolean` | 是否显示热力图图例 |
| `echartsUrl` | `String` | ECharts 脚本地址（热力图依赖，可换成自有 CDN）|

模板示例：

```html
<!-- 使用页面变量 -->
<h1 th:text="${title}">Steam 游戏库</h1>

<!-- 游戏列表（使用 games 变量） -->
<div th:each="game : ${games.items}">
    <a th:if="${enableGameLink}" th:href="${game.storeUrl}">
        <span th:text="${game.name}">游戏名</span>
    </a>
</div>

<!-- 分页导航 -->
<a th:if="${games.hasPrevious()}" th:href="${games.prevUrl}">上一页</a>
<a th:if="${games.hasNext()}" th:href="${games.nextUrl}">下一页</a>

<!-- 热力图（需要前端 JS 配合） -->
<div th:if="${showHeatmap}" id="heatmap"
     th:data-days="${heatmapDays}"
     th:data-theme="${heatmapColorTheme}"
     th:data-legend="${heatmapShowLegend}">
</div>
```

## API 接口

插件提供了 REST API 接口，可以获取 Steam 数据和热力图数据。

公开接口基础路径为 `/apis/api.steam.timxs.com/v1alpha1`，无需认证：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/profile` | GET | 获取用户资料，失败时返回 404 |
| `/games` | GET | 获取游戏库，支持分页和排序；未配置或无数据时返回空分页 |
| `/recent` | GET | 获取最近游玩；未配置或无数据时返回空数组 |
| `/stats` | GET | 获取统计数据 |
| `/achievements/{appid}` | GET | 获取指定游戏成就进度 |
| `/badges` | GET | 获取徽章信息 |
| `/game-detail/{appId}` | GET | 获取游戏详情（富文本游戏卡片使用，可带 `lang` 参数） |
| `/heatmap/records` | GET | 查询每日游戏时长记录 |

### 获取游戏列表

**接口**: `GET /apis/api.steam.timxs.com/v1alpha1/games`

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页数量，默认 20，最大 100 |
| sortBy | string | 否 | 排序字段：`playtime_forever`（默认）或 `name` |

**示例**:
```bash
# 获取第一页游戏，按游戏时长排序
GET /apis/api.steam.timxs.com/v1alpha1/games?page=1&size=20

# 获取游戏列表，按名称排序
GET /apis/api.steam.timxs.com/v1alpha1/games?sortBy=name
```

### 获取最近游玩

**接口**: `GET /apis/api.steam.timxs.com/v1alpha1/recent`

**参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | int | 否 | 返回数量，默认 5，最大 20 |

最近游玩响应元素包含游戏库字段，并额外包含 `playtime2Weeks`、`playtime2WeeksFormatted`、`achievementProgressText`、`achievementsLocked`、`inLibrary`。

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

**说明**: 立即清空 Steam 数据缓存，并在后台触发一次预热刷新。刚刷新后的短时间内页面可能暂无数据，预热完成后会恢复显示。

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
| `GET /apis/api.steam.timxs.com/v1alpha1/game-detail/{appId}?lang=xxx` | 获取游戏详情（支持 `lang` 参数指定语言） |

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
5. 适当增大 API 请求超时时间（默认 8 秒，可设 5-60 秒）

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

插件已改为「后台定时预热 + 页面只读缓存」，正常情况下页面应秒开。若仍慢：

**可能原因与解决方法：**
1. **刚启动那几秒**：后台还没拉完第一次，页面短暂显示「加载中」属正常，稍等即可；
2. **图片加载慢**：配置「游戏图片加速域名」；
3. **后台拉取一直失败（导致没数据）**：配置 Steam API 代理（国内服务器尤其需要）、确认 API Key / Steam ID 正确、Steam 隐私设为公开。

### Q: 热力图没有数据

**可能原因：**
1. 刚开启「启用游戏时长追踪」，还没到追踪时间
2. 开启追踪后还没有玩过游戏
3. 追踪功能未正确启用

**解决方法：**
1. 开启追踪后需等待至少 1 小时（定时任务在每小时第 59 分钟执行）
2. 可以点击「手动追踪时长」按钮立即执行一次追踪测试
3. 确认「启用游戏时长追踪」开关已开启并保存

### Q: 热力图只显示部分日期

这是正常现象。热力图数据从开启追踪功能后才开始记录，Steam API 不提供历史每日数据。

**说明：**
- 热力图只能展示开启功能后记录的数据
- 服务器停机期间的游戏时长无法准确追踪
- 建议保持服务器稳定运行以获得连续的数据

### Q: 手动追踪时长提示「处理了 0 款游戏」

**可能原因：**
1. 距离上次追踪时间太短，没有新的游戏时长变化
2. 这段时间内没有玩任何游戏

**说明：**
- 追踪功能只记录游戏时长的变化量
- 如果没有玩游戏，则不会产生新的记录
- 首次发现的游戏不会记录当前累计时长，从下次追踪开始计算

### Q: Steam API Key 被封禁

Steam API Key 有调用频率限制。如果短时间内请求过多，可能会被临时封禁。

**解决方法：**
1. 增大刷新间隔（活跃信息 / 库藏信息刷新间隔），降低后台拉取频率
2. 等待一段时间后自动解封
3. 如果持续被封，可以重新申请一个 API Key
