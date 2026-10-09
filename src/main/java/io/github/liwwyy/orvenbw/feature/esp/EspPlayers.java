package io.github.liwwyy.orvenbw.feature.esp;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.scoreboard.team.Team;

public final class EspPlayers {
    private EspPlayers() {}
    public static boolean listed(Minecraft mc, PlayerEntity player) {
        return mc.getNetworkHandler() != null && mc.getNetworkHandler().getOnlinePlayers().stream()
                .anyMatch(info -> player.getName().equalsIgnoreCase(info.getProfile().getName()));
    }
    public static int color(PlayerEntity player) {
        if (player == null) return -1;
        Team team = player.getScoreboardTeam() instanceof Team t ? t : null;
        var formatting = team == null ? null : team.getColor();
        var display = player.getDisplayName();
        return resolveColor(team == null ? null : team.getPrefix(), formatting == null ? null : formatting.getId(),
                display == null ? null : display.getFormattedString(), player.getName());
    }
    public static int resolveColor(String prefix, Integer id, String display, String name) {
        int color = formattingColor(prefix);
        if (color >= 0) return color;
        if (id != null && id >= 0) return palette(id);
        if (display == null) return -1;
        int start = name == null ? -1 : display.indexOf(name);
        return formattingColor(start >= 0 ? display.substring(0,start) : display);
    }
    public static int formattingColor(String text) {
        if (text == null) return -1;
        int found = -1;
        // Effective colour at the end of the prefix: rank brackets may have their own colour.
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) != '§') continue;
            char code = Character.toLowerCase(text.charAt(++i));
            int index = "0123456789abcdef".indexOf(code);
            if (index >= 0) found = palette(index);
            else if (code == 'r') found = -1;
        }
        return found;
    }
    public static int palette(int index) {
        int[] palette = {0x000000,0x0000aa,0x00aa00,0x00aaaa,0xaa0000,0xaa00aa,0xffaa00,0xaaaaaa,
                0x555555,0x5555ff,0x55ff55,0x55ffff,0xff5555,0xff55ff,0xffff55,0xffffff};
        return index >= 0 && index < 16 ? palette[index] : -1;
    }
}
