package com.timxs.steam.controller;

import com.timxs.steam.model.AchievementProgress;
import com.timxs.steam.model.BadgeInfo;
import com.timxs.steam.model.OwnedGame;
import com.timxs.steam.model.RecentGame;
import com.timxs.steam.model.SteamProfile;
import com.timxs.steam.model.SteamStats;
import com.timxs.steam.service.SteamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

/**
 * Steam REST API 控制器（公开接口）
 */
@Component
@RequiredArgsConstructor
public class SteamController implements CustomEndpoint {

    private final SteamService steamService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = "SteamV1alpha1Public";
        return org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route()
                .GET("/profile", this::getProfile,
                        builder -> builder.operationId("GetSteamProfile")
                                .description("获取 Steam 用户资料")
                                .tag(tag)
                                .response(responseBuilder().implementation(SteamProfile.class)))
                .GET("/games", this::getGames,
                        builder -> builder.operationId("GetSteamGames")
                                .description("获取 Steam 游戏库")
                                .tag(tag)
                                .parameter(parameterBuilder().name("page").description("页码").required(false))
                                .parameter(parameterBuilder().name("size").description("每页数量").required(false))
                                .parameter(parameterBuilder().name("sortBy").description("排序字段: playtime_forever(默认), name").required(false))
                                .response(responseBuilder().implementation(ListResult.generateGenericClass(OwnedGame.class))))
                .GET("/recent", this::getRecentGames,
                        builder -> builder.operationId("GetRecentGames")
                                .description("获取最近游玩的游戏")
                                .tag(tag)
                                .parameter(parameterBuilder().name("limit").description("返回数量").required(false))
                                .response(responseBuilder().implementationArray(RecentGame.class)))
                .GET("/stats", this::getStats,
                        builder -> builder.operationId("GetSteamStats")
                                .description("获取 Steam 统计数据")
                                .tag(tag)
                                .response(responseBuilder().implementation(SteamStats.class)))
                .GET("/achievements/{appid}", this::getAchievements,
                        builder -> builder.operationId("GetGameAchievements")
                                .description("获取指定游戏的成就进度")
                                .tag(tag)
                                .parameter(parameterBuilder().name("appid").description("游戏 ID").required(true))
                                .response(responseBuilder().implementation(AchievementProgress.class)))
                .GET("/badges", this::getBadges,
                        builder -> builder.operationId("GetSteamBadges")
                                .description("获取 Steam 徽章信息")
                                .tag(tag)
                                .response(responseBuilder().implementation(BadgeInfo.class)))
                .build();
    }

    @Override
    public GroupVersion groupVersion() {
        return new GroupVersion("api.steam.halo.run", "v1alpha1");
    }

    private Mono<ServerResponse> getProfile(ServerRequest request) {
        return steamService.getProfile()
                .flatMap(profile -> ServerResponse.ok().bodyValue(profile))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> getGames(ServerRequest request) {
        int page = Math.max(1, parseIntOrDefault(request.queryParam("page").orElse(null), 1));
        int size = Math.min(100, Math.max(1, parseIntOrDefault(request.queryParam("size").orElse(null), 20)));
        String sortBy = request.queryParam("sortBy").orElse("playtime_forever");

        return steamService.getOwnedGames(page, size, sortBy)
                .flatMap(games -> ServerResponse.ok().bodyValue(games));
    }

    private Mono<ServerResponse> getRecentGames(ServerRequest request) {
        int limit = Math.min(20, Math.max(1, parseIntOrDefault(request.queryParam("limit").orElse(null), 5)));

        return steamService.getRecentGames(limit)
                .flatMap(games -> ServerResponse.ok().bodyValue(games));
    }

    private Mono<ServerResponse> getStats(ServerRequest request) {
        return steamService.getFullStats()
                .flatMap(stats -> ServerResponse.ok().bodyValue(stats))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> getAchievements(ServerRequest request) {
        Long appId = parseLongOrNull(request.pathVariable("appid"));
        if (appId == null) {
            return ServerResponse.badRequest().build();
        }
        return steamService.getAchievementProgress(appId)
                .flatMap(progress -> ServerResponse.ok().bodyValue(progress));
    }

    private Mono<ServerResponse> getBadges(ServerRequest request) {
        return steamService.getBadges()
                .flatMap(badges -> ServerResponse.ok().bodyValue(badges))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Long parseLongOrNull(String value) {
        if (value == null) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
