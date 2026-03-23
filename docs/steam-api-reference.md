# Steam API 技术参考

本文档详细介绍 Halo Steam 插件使用的 Steam Web API 接口，供开发者搭建自定义 API 代理服务时参考。

## 目录

- [API 接口列表](#api-接口列表)
- [Steam Web API](#steam-web-api)
  - [ISteamUser/GetPlayerSummaries](#isteamusergetplayersummaries)
  - [IPlayerService/GetOwnedGames](#iplayerservicegetownedgames)
  - [IPlayerService/GetRecentlyPlayedGames](#iplayerservicegetrecentlyplayedgames)
  - [IPlayerService/GetSteamLevel](#iplayerservicegetsteamlevel)
  - [ISteamUserStats/GetPlayerAchievements](#isteamuserstatsgetplayerachievements)
  - [IPlayerService/GetBadges](#iplayerservicegetbadges)
- [Steam Store API](#steam-store-api)
  - [appdetails](#appdetails)
- [搭建自定义 API 代理](#搭建自定义-api-代理)
- [实现示例](#实现示例)

## API 接口列表

插件使用以下 Steam API 接口：

| 接口 | 用途 | 基础地址 |
|------|------|----------|
| `ISteamUser/GetPlayerSummaries/v2/` | 获取用户资料 | `api.steampowered.com` |
| `IPlayerService/GetOwnedGames/v1/` | 获取游戏库 | `api.steampowered.com` |
| `IPlayerService/GetRecentlyPlayedGames/v1/` | 获取最近游玩 | `api.steampowered.com` |
| `IPlayerService/GetSteamLevel/v1/` | 获取 Steam 等级 | `api.steampowered.com` |
| `ISteamUserStats/GetPlayerAchievements/v1/` | 获取成就进度 | `api.steampowered.com` |
| `IPlayerService/GetBadges/v1/` | 获取徽章信息 | `api.steampowered.com` |
| `/api/appdetails` | 获取游戏详情 | `store.steampowered.com` |

## Steam Web API

### 基础信息

- **基础地址**: `https://api.steampowered.com`
- **认证方式**: URL 参数 `key` (Steam API Key)
- **请求方法**: GET
- **响应格式**: JSON

### ISteamUser/GetPlayerSummaries

获取 Steam 用户基本资料。

**接口路径**: `/ISteamUser/GetPlayerSummaries/v2/`

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| key | string | 是 | Steam API Key |
| steamids | string | 是 | Steam ID，多个用逗号分隔 |

**请求示例**:
```
GET https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/?key=XXX&steamids=76561198000000000
```

**响应示例**:
```json
{
  "response": {
    "players": [
      {
        "steamid": "76561198000000000",
        "personaname": "用户昵称",
        "profileurl": "https://steamcommunity.com/id/xxx/",
        "avatar": "https://avatars.steamstatic.com/xxx.jpg",
        "avatarmedium": "https://avatars.steamstatic.com/xxx_medium.jpg",
        "avatarfull": "https://avatars.steamstatic.com/xxx_full.jpg",
        "personastate": 1,
        "communityvisibilitystate": 3,
        "profilestate": 1,
        "lastlogoff": 1704700000,
        "commentpermission": 1
      }
    ]
  }
}
```

**字段说明**:
- `personastate`: 在线状态 (0=离线, 1=在线, 2=忙碌, 3=离开, 4=睡觉, 5=想交易, 6=想玩)
- `communityvisibilitystate`: 配置可见性 (1=私密, 3=公开)

### IPlayerService/GetOwnedGames

获取用户拥有的游戏列表。

**接口路径**: `/IPlayerService/GetOwnedGames/v1/`

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| key | string | 是 | Steam API Key |
| steamid | string | 是 | Steam ID |
| include_appinfo | boolean | 否 | 是否包含游戏详细信息，默认 false |
| include_played_free_games | boolean | 否 | 是否包含玩过的免费游戏，默认 false |

**请求示例**:
```
GET https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/?key=XXX&steamid=76561198000000000&include_appinfo=1&include_played_free_games=1
```

**响应示例**:
```json
{
  "response": {
    "game_count": 150,
    "games": [
      {
        "appid": 730,
        "name": "Counter-Strike 2",
        "playtime_forever": 120000,
        "playtime_2weeks": 120,
        "img_icon_url": "xxx",
        "img_logo_url": "xxx",
        "has_community_visible_stats": true
      }
    ]
  }
}
```

### IPlayerService/GetRecentlyPlayedGames

获取最近游玩的游戏。

**接口路径**: `/IPlayerService/GetRecentlyPlayedGames/v1/`

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| key | string | 是 | Steam API Key |
| steamid | string | 是 | Steam ID |
| count | int | 否 | 返回数量，默认全部 |

**请求示例**:
```
GET https://api.steampowered.com/IPlayerService/GetRecentlyPlayedGames/v1/?key=XXX&steamid=76561198000000000&count=5
```

**响应示例**:
```json
{
  "response": {
    "total_count": 10,
    "games": [
      {
        "appid": 730,
        "name": "Counter-Strike 2",
        "playtime_2weeks": 120,
        "playtime_forever": 120000,
        "img_icon_url": "xxx",
        "img_logo_url": "xxx"
      }
    ]
  }
}
```

### IPlayerService/GetSteamLevel

获取用户 Steam 等级。

**接口路径**: `/IPlayerService/GetSteamLevel/v1/`

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| key | string | 是 | Steam API Key |
| steamid | string | 是 | Steam ID |

**请求示例**:
```
GET https://api.steampowered.com/IPlayerService/GetSteamLevel/v1/?key=XXX&steamid=76561198000000000
```

**响应示例**:
```json
{
  "response": {
    "player_level": 50
  }
}
```

### ISteamUserStats/GetPlayerAchievements

获取玩家在指定游戏中的成就进度。

**接口路径**: `/ISteamUserStats/GetPlayerAchievements/v1/`

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| key | string | 是 | Steam API Key |
| steamid | string | 是 | Steam ID |
| appid | long | 是 | 游戏 ID |
| l | string | 否 | 语言代码，如 `schinese` |

**请求示例**:
```
GET https://api.steampowered.com/ISteamUserStats/GetPlayerAchievements/v1/?key=XXX&steamid=76561198000000000&appid=730&l=schinese
```

**响应示例**:
```json
{
  "playerstats": {
    "steamID": "76561198000000000",
    "gameName": "Counter-Strike 2",
    "achievements": [
      {
        "apiname": "ACHIEVEMENT_1",
        "achieved": 1,
        "name": "成就名称",
        "description": "成就描述"
      }
    ],
    "success": true
  }
}
```

**错误处理**:
- `403`: 游戏详情因隐私设置不公开
- `400`: 游戏无成就系统

### IPlayerService/GetBadges

获取用户徽章信息。

**接口路径**: `/IPlayerService/GetBadges/v1/`

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| key | string | 是 | Steam API Key |
| steamid | string | 是 | Steam ID |

**请求示例**:
```
GET https://api.steampowered.com/IPlayerService/GetBadges/v1/?key=XXX&steamid=76561198000000000
```

**响应示例**:
```json
{
  "response": {
    "badges": [
      {
        "badgeid": 1,
        "level": 1,
        "completion_time": 1609459200,
        "xp": 5,
        "scarcity": 10
      }
    ],
    "player_xp": 5000,
    "player_level": 50,
    "player_xp_needed_to_level_up": 100,
    "player_xp_needed_current_level": 800
  }
}
```

## Steam Store API

### 基础信息

- **基础地址**: `https://store.steampowered.com`
- **认证方式**: 无需 API Key
- **请求方法**: GET
- **响应格式**: JSON

### appdetails

获取游戏详细信息（用于游戏卡片功能）。

**接口路径**: `/api/appdetails`

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| appids | long | 是 | 游戏 ID |
| l | string | 否 | 语言代码，如 `schinese` |
| cc | string | 否 | 国家代码，用于价格货币 |

**请求示例**:
```
GET https://store.steampowered.com/api/appdetails?appids=730&l=schinese&cn=CN
```

**响应示例**:
```json
{
  "730": {
    "success": true,
    "data": {
      "name": "Counter-Strike 2",
      "steam_appid": 730,
      "header_image": "https://cdn.cloudflare.steamstatic.com/steam/apps/730/header.jpg",
      "short_description": "游戏简介...",
      "is_free": true,
      "developers": ["Valve"],
      "publishers": ["Valve"],
      "genres": [
        {
          "id": "1",
          "description": "动作"
        }
      ],
      "price_overview": {
        "currency": "CNY",
        "initial": 0,
        "final": 0,
        "final_formatted": "免费"
      },
      "release_date": {
        "date": "2012 年 8 月 21 日",
        "coming_soon": false
      }
    }
  }
}
```

## 搭建自定义 API 代理

### 代理服务要求

自定义 API 代理需要满足以下要求：

1. **路径转发**: 代理服务需要将请求转发到对应的 Steam API 端点
2. **参数透传**: 保持所有查询参数不变，包括 `key` 参数
3. **响应透传**: 直接返回 Steam API 的原始响应，不做修改
4. **错误处理**: 保持原始的 HTTP 状态码和错误信息

### 端点映射

### 端点映射

代理服务需要根据请求路径转发到不同的目标域名：

| 请求路径匹配 | 转发目标 |
|------------|---------|
| `/ISteamUser/*` | `https://api.steampowered.com` |
| `/IPlayerService/*` | `https://api.steampowered.com` |
| `/ISteamUserStats/*` | `https://api.steampowered.com` |
| `/api/appdetails` | `https://store.steampowered.com` |

**转发示例**（假设代理地址为 `https://proxy.example.com`）：

```
代理收到:  https://proxy.example.com/ISteamUser/GetPlayerSummaries/v2/?key=xxx
转发到:   https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/?key=xxx

代理收到:  https://proxy.example.com/api/appdetails?appids=730
转发到:   https://store.steampowered.com/api/appdetails?appids=730
```

## 实现示例

### Node.js + Express 示例

```javascript
const express = require('express');
const axios = require('axios');
const app = express();

// Steam API 基础地址
const STEAM_WEB_API = 'https://api.steampowered.com';
const STEAM_STORE_API = 'https://store.steampowered.com';

// 根据路径选择目标
function getTargetUrl(path) {
  // Store API: /api/appdetails
  if (path.startsWith('/api/appdetails')) {
    return STEAM_STORE_API;
  }
  // Web API: /ISteamUser/, /IPlayerService/, /ISteamUserStats/
  return STEAM_WEB_API;
}

// 代理所有请求
app.use('*', async (req, res) => {
  try {
    const targetBase = getTargetUrl(req.path);
    const targetUrl = targetBase + req.originalUrl;

    // 转发请求
    const response = await axios.get(targetUrl, {
      params: req.query,
      validateStatus: false
    });

    res.status(response.status).json(response.data);
  } catch (error) {
    res.status(500).json({ error: 'Proxy error', message: error.message });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Steam API 代理服务运行在端口 ${PORT}`);
});
```

### Nginx 反向代理示例

```nginx
server {
    listen 80;
    server_name your-proxy-domain.com;

    # Steam Store API
    location /api/appdetails {
        proxy_pass https://store.steampowered.com;
        proxy_set_header Host store.steampowered.com;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_ssl_server_name on;
    }

    # Steam Web API (ISteamUser, IPlayerService, ISteamUserStats)
    location ~ ^/(ISteamUser|IPlayerService|ISteamUserStats)/ {
        proxy_pass https://api.steampowered.com;
        proxy_set_header Host api.steampowered.com;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_ssl_server_name on;
    }
}
```

### Cloudflare Workers 示例

```javascript
export default {
  async fetch(request) {
    const url = new URL(request.url);
    const path = url.pathname;

    // 根据路径选择目标
    let targetBase;
    if (path.startsWith('/api/appdetails')) {
      targetBase = 'https://store.steampowered.com';
    } else {
      targetBase = 'https://api.steampowered.com';
    }

    const targetUrl = targetBase + path + url.search;

    const response = await fetch(targetUrl, {
      method: request.method,
      headers: request.headers
    });

    return new Response(response.body, {
      status: response.status,
      headers: response.headers
    });
  }
};
```

## 常见问题

### Q: 代理服务是否需要同时支持两个域名？

是的。插件会调用两类 API：
- **Steam Web API** (`api.steampowered.com`) - 用于获取用户资料、游戏库等
- **Steam Store API** (`store.steampowered.com`) - 用于获取游戏详情

代理服务需要根据请求路径转发到对应的目标。

### Q: 代理服务是否需要缓存？

建议在代理服务层实现缓存，可以减少对 Steam API 的请求频率，提高响应速度。

### Q: 如何测试代理服务是否正常？

在插件配置中填写代理地址后，点击「验证配置」按钮。如果验证通过，说明代理服务工作正常。

## 参考资料

- [Steam Web API 官方文档](https://steamcommunity.com/dev)
- [Steam Web API 列表](https://steamapi.xpaw.me/)
- [ISteamWebAPIUtil](https://steamapi.xpaw.me/#ISteamWebAPIUtil)
