package com.timxs.steam.controller;

import com.timxs.steam.controller.dto.HeatmapResult;
import com.timxs.steam.model.DailyPlaytimeRecord;
import com.timxs.steam.service.PlaytimeTrackingService;
import com.timxs.steam.service.SteamSettingService;
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
 * 热力图 API 控制器
 */
@Component
@RequiredArgsConstructor
public class HeatmapController implements CustomEndpoint {

    private final PlaytimeTrackingService trackingService;
    private final SteamSettingService settingService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = "HeatmapV1alpha1";
        return org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route()
                // 公开 API - 查询热力图数据
                .GET("/heatmap/records", this::queryRecords,
                        builder -> builder.operationId("QueryHeatmapRecords")
                                .description("查询每日游戏时长记录（公开 API）")
                                .tag(tag)
                                .parameter(parameterBuilder().name("startDate").description("开始日期 (yyyy-MM-dd)").required(true))
                                .parameter(parameterBuilder().name("endDate").description("结束日期 (yyyy-MM-dd)").required(true))
                                .parameter(parameterBuilder().name("appId").description("游戏 ID（可选）").required(false))
                                .parameter(parameterBuilder().name("page").description("页码").required(false))
                                .parameter(parameterBuilder().name("size").description("每页大小").required(false))
                                .response(responseBuilder().implementation(ListResult.generateGenericClass(DailyPlaytimeRecord.class))))
                .build();
    }

    @Override
    public GroupVersion groupVersion() {
        return new GroupVersion("api.steam.halo.run", "v1alpha1");
    }

    /**
     * 查询每日游戏时长记录
     */
    private Mono<ServerResponse> queryRecords(ServerRequest request) {
        String startDate = request.queryParam("startDate").orElse("");
        String endDate = request.queryParam("endDate").orElse("");
        Long appId = parseLongOrNull(request.queryParam("appId").orElse(null));
        int page = parseIntOrDefault(request.queryParam("page").orElse(null), 1);
        int size = parseIntOrDefault(request.queryParam("size").orElse(null), 365);

        return trackingService.queryDailyRecords(startDate, endDate, appId, page, size)
                .flatMap(records -> ServerResponse.ok().bodyValue(records));
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
