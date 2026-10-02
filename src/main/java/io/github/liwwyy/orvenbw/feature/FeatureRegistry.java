package io.github.liwwyy.orvenbw.feature;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public final class FeatureRegistry {
    private final List<ClientFeature> features = new ArrayList<>();
    private Object world;
    private Object player;
    private boolean active;

    public void checkContext(Minecraft client) { syncContext(client); }
    public void register(ClientFeature feature) { features.add(feature); }
    public void onInput(Minecraft client, int keyCode) {
        if (syncContext(client)) for (ClientFeature f : features) f.onInput(client, keyCode);
    }
    public void beforeInteractions(Minecraft client) {
        if (syncContext(client)) for (ClientFeature f : features) f.beforeInteractions(client);
    }
    private boolean syncContext(Minecraft client) {
        boolean ready = (io.github.liwwyy.orvenbw.OrvenBw.instance() == null || ScoreboardGate.allows(client, io.github.liwwyy.orvenbw.OrvenBw.instance().config())) && client.world != null && client.player != null && client.screen == null && client.focused && !client.isPaused();
        if (!ready || world != client.world || player != client.player) {
            if (active || world != client.world || player != client.player) for (ClientFeature f : features) f.reset();
        }
        world = client.world;
        player = client.player;
        active = ready;
        return ready;
    }
}
