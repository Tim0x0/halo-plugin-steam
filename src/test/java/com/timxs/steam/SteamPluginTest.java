package com.timxs.steam;

import com.timxs.steam.cache.CacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.PluginContext;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SteamPluginTest {

    @Mock
    PluginContext context;

    @Mock
    CacheService cacheService;

    @InjectMocks
    SteamPlugin plugin;

    @Test
    void contextLoads() {
        when(cacheService.evictAll()).thenReturn(Mono.empty());
        
        plugin.start();
        plugin.stop();
    }
}
