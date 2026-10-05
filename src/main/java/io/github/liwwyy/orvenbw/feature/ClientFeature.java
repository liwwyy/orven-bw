package io.github.liwwyy.orvenbw.feature;

import net.minecraft.client.Minecraft;

/** Small lifecycle boundary for future independent utilities. All calls are on the client thread. */
public interface ClientFeature {
    default void tick(Minecraft client) {}
    default boolean ownsScreen(Minecraft client) { return false; }
    default void onInput(Minecraft client, int keyCode) {}
    default void beforeInteractions(Minecraft client) {}
    default void reset() {}
}
