package io.github.liwwyy.orvenbw;

import io.github.liwwyy.orvenbw.config.OrvenConfig;
import io.github.liwwyy.orvenbw.feature.FeatureRegistry;
import io.github.liwwyy.orvenbw.feature.clickassist.ClickAssistFeature;
import io.github.liwwyy.orvenbw.hud.ClickAssistHud;
import io.github.liwwyy.orvenbw.feature.hiteffects.HitEffectsFeature;
import net.fabricmc.api.ClientModInitializer;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OrvenBw implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("orven-bw");
    private static OrvenBw instance;
    private final FeatureRegistry features = new FeatureRegistry();
    private OrvenConfig config;
    private ClickAssistFeature clickAssist;
    private HitEffectsFeature hitEffects;
    private io.github.liwwyy.orvenbw.debug.ClickDebugLog debugLog;

    @Override public void onInitializeClient() {
        config = new OrvenConfig();
        config.preload();
        debugLog = new io.github.liwwyy.orvenbw.debug.ClickDebugLog(
                net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("orven-bw/click-debug.jsonl"),
                () -> config.debugEnabled, error -> LOGGER.warn("Click debug log could not be written", error));
        Runtime.getRuntime().addShutdownHook(new Thread(debugLog::close, "orven-bw-debug-shutdown"));
        clickAssist = new ClickAssistFeature(config);
        features.register(clickAssist);
        hitEffects = new HitEffectsFeature(config);
        features.register(hitEffects);
        org.polyfrost.oneconfig.api.config.v1.ConfigManager.addProfileChangeListener(name -> { features.reset(); config.migrate(); });
        instance = this;
        org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager.register(config.settingsBind);
        org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager.register(config.spamLeftBind);
        org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager.register(config.spamRightBind);
        org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager.register(config.toggleModBind);
        org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager.register(config.holdLeftBind);
        org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager.register(config.holdRightBind);
        HudManager.register(new ClickAssistHud(), "orven-bw.json", "combat");
        LOGGER.info("orven-bw initialized (maintainer: liwwyy, Feather Gen 2 build 2)");
    }

    public io.github.liwwyy.orvenbw.debug.ClickDebugLog debugLog() { return debugLog; }
    public static OrvenBw instance() { return instance; }
    public FeatureRegistry features() { return features; }
    public OrvenConfig config() { return config; }
    public HitEffectsFeature hitEffects() { return hitEffects; }
    public ClickAssistFeature clickAssist() { return clickAssist; }
}
