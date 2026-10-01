package io.github.liwwyy.orvenbw;

import io.github.liwwyy.orvenbw.config.OrvenConfig;
import io.github.liwwyy.orvenbw.feature.FeatureRegistry;
import io.github.liwwyy.orvenbw.feature.clickassist.ClickAssistFeature;
import io.github.liwwyy.orvenbw.hud.ClickAssistHud;
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

    @Override public void onInitializeClient() {
        config = new OrvenConfig();
        config.preload();
        clickAssist = new ClickAssistFeature(config);
        features.register(clickAssist);
        instance = this;
        HudManager.register(new ClickAssistHud(), "orven-bw.json", "combat");
        LOGGER.info("orven-bw initialized (maintainer: liwwyy, Feather Gen 2 build 2)");
    }

    public static OrvenBw instance() { return instance; }
    public FeatureRegistry features() { return features; }
    public OrvenConfig config() { return config; }
    public ClickAssistFeature clickAssist() { return clickAssist; }
}
