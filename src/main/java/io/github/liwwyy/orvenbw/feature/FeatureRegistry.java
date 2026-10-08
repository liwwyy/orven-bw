package io.github.liwwyy.orvenbw.feature;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class FeatureRegistry {
    private final List<ClientFeature> features = new ArrayList<>();
    private Object world;
    private Object player;
    private boolean active;

    public void reset() { for (ClientFeature feature : features) feature.reset(); }
    public void checkContext(Minecraft client) {
        syncContext(client);
        for (ClientFeature feature : features) feature.tick(client);
    }
    public void register(ClientFeature feature) { features.add(feature); }
    public void onInput(Minecraft client, int keyCode) {
        if (syncContext(client)) for (ClientFeature f : features) f.onInput(client, keyCode);
    }
    public void beforeInteractions(Minecraft client) {
        if (syncContext(client)) for (ClientFeature f : features) f.beforeInteractions(client);
    }
    private boolean syncContext(Minecraft client) {
        boolean global = io.github.liwwyy.orvenbw.OrvenBw.instance() == null || ScoreboardGate.allows(client, io.github.liwwyy.orvenbw.OrvenBw.instance().config());
        boolean worldChanged = world != client.world;
        boolean changed = worldChanged || player != client.player;
        boolean ready = global && client.world != null && client.player != null && client.screen == null && client.focused && !client.isPaused();
        if (!ready || changed) {
            if (active || changed) for (ClientFeature f : features)
                if (changed || !global || client.isPaused() || !f.ownsScreen(client)) f.contextLost(worldChanged);
        }
        world = client.world;
        player = client.player;
        active = ready;
        return ready;
    }
}
