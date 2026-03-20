package com.timxs.steam;

import com.timxs.steam.service.SteamSettingService;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.PropertyPlaceholderHelper;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.PluginContext;
import run.halo.app.theme.dialect.TemplateHeadProcessor;

@Component
@RequiredArgsConstructor
public class SteamGameCardHeadProcessor implements TemplateHeadProcessor {

    static final PropertyPlaceholderHelper PLACEHOLDER_HELPER =
            new PropertyPlaceholderHelper("${", "}");

    private final PluginContext pluginContext;
    private final SteamSettingService settingService;

    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
                              IElementModelStructureHandler structureHandler) {
        final IModelFactory modelFactory = context.getModelFactory();
        return getHeadTags()
                .map(headTags -> {
                    model.add(modelFactory.createText(headTags));
                    return headTags;
                })
                .then();
    }

    private Mono<String> getHeadTags() {
        final Properties properties = new Properties();
        properties.setProperty("version", pluginContext.getVersion());
        properties.setProperty("prefix",
                "/plugins/" + pluginContext.getName() + "/assets/static");

        return settingService.getDarkModeSelector()
                .defaultIfEmpty("html.dark")
                .map(selector -> {
                    properties.setProperty("selector", escapeJs(selector));
                    var headTags = """
                            <!-- plugin-steam game-card start -->
                            <script>window.__STEAM_DARK_SELECTOR__="${selector}";</script>
                            <script src="${prefix}/steam-game-card.js?v=${version}"></script>
                            <link rel="stylesheet" href="${prefix}/steam-game-card.css?v=${version}" />
                            <!-- plugin-steam game-card end -->
                            """;
                    return PLACEHOLDER_HELPER.replacePlaceholders(headTags, properties);
                });
    }

    private String escapeJs(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("'", "\\'")
                .replace("<", "\\u003c")
                .replace(">", "\\u003e")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
