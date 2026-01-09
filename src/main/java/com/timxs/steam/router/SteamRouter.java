package com.timxs.steam.router;

import com.timxs.steam.finders.SteamFinder;
import com.timxs.steam.model.OwnedGame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.TemplateNameResolver;
import run.halo.app.theme.router.ModelConst;
import run.halo.app.theme.router.PageUrlUtils;
import run.halo.app.theme.router.UrlContextListResult;

import java.util.Map;

import static run.halo.app.theme.router.PageUrlUtils.totalPage;

/**
 * Steam 前端路由
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SteamRouter {

    private final SteamFinder steamFinder;
    private final TemplateNameResolver templateNameResolver;
    private final ReactiveSettingFetcher settingFetcher;

    @Bean
    RouterFunction<ServerResponse> steamTemplateRoute() {
        return RouterFunctions.route()
                .GET("/steam", this::handleSteamPage)
                .GET("/steam/page/{page:\\d+}", this::handleSteamPage)
                .build();
    }

    private Mono<ServerResponse> handleSteamPage(ServerRequest request) {
        return templateNameResolver.resolveTemplateNameOrDefault(request.exchange(), "steam")
                .flatMap(templateName -> ServerResponse.ok().render(templateName,
                        Map.of(
                                ModelConst.TEMPLATE_ID, templateName,
                                "title", getPageTitle(),
                                "gamesLimit", getGamesLimit(),
                                "enableGameLink", getEnableGameLink(),
                                "games", getGamesList(request)
                        )
                ));
    }

    private int getPageNum(ServerRequest request) {
        String page = request.pathVariables().get("page");
        return NumberUtils.toInt(page, 1);
    }

    private Mono<Integer> getGamesLimit() {
        return settingFetcher.get("page")
                .map(setting -> {
                    var node = setting.get("gamesLimit");
                    return node != null ? node.asInt(0) : 0;
                })
                .defaultIfEmpty(0);
    }

    private Mono<Boolean> getEnableGameLink() {
        return settingFetcher.get("page")
                .map(setting -> {
                    var node = setting.get("enableGameLink");
                    return node == null || node.asBoolean(true);
                })
                .defaultIfEmpty(true);
    }

    private Mono<String> getPageTitle() {
        return settingFetcher.get("page")
                .map(setting -> {
                    var pageTitleNode = setting.get("pageTitle");
                    return pageTitleNode != null ? pageTitleNode.asText("Steam 游戏库") : "Steam 游戏库";
                })
                .defaultIfEmpty("Steam 游戏库");
    }

    private Mono<UrlContextListResult<OwnedGame>> getGamesList(ServerRequest request) {
        String path = request.path();
        int pageNum = getPageNum(request);

        return settingFetcher.get("page")
                .map(setting -> {
                    var pageSizeNode = setting.get("pageSize");
                    return pageSizeNode != null ? pageSizeNode.asInt(12) : 12;
                })
                .defaultIfEmpty(12)
                .flatMap(pageSize -> steamFinder.getOwnedGames(pageNum, pageSize)
                        .map(list -> new UrlContextListResult.Builder<OwnedGame>()
                                .listResult(list)
                                .nextUrl(PageUrlUtils.nextPageUrl(path, totalPage(list)))
                                .prevUrl(PageUrlUtils.prevPageUrl(path))
                                .build()
                        )
                );
    }
}
