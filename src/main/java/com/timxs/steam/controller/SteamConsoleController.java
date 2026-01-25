package com.timxs.steam.controller;

import com.timxs.steam.controller.dto.HeatmapResult;
import com.timxs.steam.service.SteamService;
import com.timxs.steam.service.PlaytimeTrackingService;
import com.timxs.steam.service.SteamSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

/**
 * Steam Console API 控制器（后台管理）
 */
@Component
@RequiredArgsConstructor
public class SteamConsoleController implements CustomEndpoint {

    private final SteamService steamService;
    private final PlaytimeTrackingService trackingService;
    private final SteamSettingService settingService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = "SteamV1alpha1Console";
        return org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route()
                .POST("/verify", this::verifyConfig,
                        builder -> builder.operationId("VerifySteamConfig")
                                .description("验证 Steam API 配置（用于 verificationForm）")
                                .tag(tag)
                                .requestBody(requestBodyBuilder()
                                        .implementation(VerifyRequest.class))
                                .response(responseBuilder()
                                        .implementation(VerifyRequest.class)))
                .POST("/refresh", this::refreshCache,
                        builder -> builder.operationId("RefreshSteamCache")
                                .description("刷新 Steam 数据缓存")
                                .tag(tag)
                                .response(responseBuilder()
                                        .implementation(RefreshResponse.class)))
                .POST("/heatmap/track", this::manualTrack,
                        builder -> builder.operationId("ManualTrackPlaytime")
                                .description("手动触发游戏时长追踪（需要管理员权限）")
                                .tag(tag)
                                .response(responseBuilder()
                                        .implementation(HeatmapResult.class)))
                .POST("/heatmap/cleanup", this::manualCleanup,
                        builder -> builder.operationId("ManualCleanupHeatmap")
                                .description("手动触发热力图数据清理（需要管理员权限）")
                                .tag(tag)
                                .response(responseBuilder()
                                        .implementation(HeatmapResult.class)))
                .build();
    }

    @Override
    public GroupVersion groupVersion() {
        return new GroupVersion("console.api.steam.timxs.com", "v1alpha1");
    }

    /**
     * 验证配置（符合 verificationForm 规范）
     * 成功返回原数据，失败返回 RFC 7807 错误
     */
    private Mono<ServerResponse> verifyConfig(ServerRequest request) {
        return request.bodyToMono(VerifyRequest.class)
                .flatMap(req -> {
                    if (req.getApiKey() == null || req.getApiKey().isBlank()) {
                        return createProblemResponse(400, "验证失败", "API Key 不能为空");
                    }
                    if (req.getSteamId() == null || req.getSteamId().isBlank()) {
                        return createProblemResponse(400, "验证失败", "Steam ID 不能为空");
                    }
                    
                    return steamService.validateApiKey(req.getApiKey(), req.getSteamId())
                            .flatMap(result -> {
                                if (result.isSuccess()) {
                                    return ServerResponse.ok()
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .bodyValue(req);
                                } else {
                                    // 直接使用 Steam API 返回的状态码和消息
                                    return createProblemResponse(result.getStatusCode(), "验证失败", result.getMessage());
                                }
                            });
                });
    }

    /**
     * 创建 RFC 7807 Problem Details 响应
     */
    private Mono<ServerResponse> createProblemResponse(int statusCode, String title, String detail) {
        ProblemDetail problem = new ProblemDetail();
        problem.setTitle(title);
        problem.setStatus(statusCode);
        problem.setDetail(detail);
        
        return ServerResponse.status(statusCode)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyValue(problem);
    }

    @lombok.Data
    public static class VerifyRequest {
        private String apiKey;
        private String steamId;
    }

    @lombok.Data
    public static class ProblemDetail {
        private String title;
        private int status;
        private String detail;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class RefreshResponse {
        private boolean success;
        private String message;
    }

    /**
     * 刷新缓存
     */
    private Mono<ServerResponse> refreshCache(ServerRequest request) {
        return steamService.refreshCache()
                .then(ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new RefreshResponse(true, "Steam 数据缓存已清空")))
                .onErrorResume(e -> ServerResponse.status(500)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new RefreshResponse(false, e.getMessage())));
    }

    /**
     * 手动触发时长追踪
     */
    private Mono<ServerResponse> manualTrack(ServerRequest request) {
        return settingService.isHeatmapEnabled()
            .flatMap(enabled -> {
                if (!enabled) {
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(new HeatmapResult(false, 0, "游戏时长追踪功能未启用，请先在设置中开启"));
                }
                
                return trackingService.trackAllGames()
                    .flatMap(count -> ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(new HeatmapResult(true, count, "追踪完成，处理了 " + count + " 款游戏")))
                    .onErrorResume(e -> {
                        String errorMsg = e.getMessage();
                        String message;
                        if (errorMsg != null && errorMsg.contains("Did not observe")) {
                            message = "Steam API 请求超时，请检查网络连接或增加超时时间";
                        } else if (errorMsg != null && errorMsg.contains("Steam ID")) {
                            message = errorMsg;
                        } else {
                            message = "追踪失败: " + (errorMsg != null ? errorMsg : "未知错误");
                        }
                        return ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(new HeatmapResult(false, 0, message));
                    });
            });
    }

    /**
     * 手动触发数据清理
     */
    private Mono<ServerResponse> manualCleanup(ServerRequest request) {
        return settingService.isHeatmapEnabled()
            .flatMap(enabled -> {
                if (!enabled) {
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(new HeatmapResult(false, 0, "游戏时长追踪功能未启用，请先在设置中开启"));
                }
                
                return trackingService.cleanupExpiredData()
                    .flatMap(count -> ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(new HeatmapResult(true, count, "清理完成，删除了 " + count + " 条记录")))
                    .onErrorResume(e -> ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(new HeatmapResult(false, 0, "清理失败：" + e.getMessage())));
            });
    }
}
