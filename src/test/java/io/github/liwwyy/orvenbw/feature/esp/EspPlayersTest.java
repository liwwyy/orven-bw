package io.github.liwwyy.orvenbw.feature.esp;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EspPlayersTest {
    @Test void formattingReadsColourAndResetsWithoutRemovingTheCodes() {
        assertEquals(0xff5555,EspPlayers.formattingColor("§c§lPlayer"));
        assertEquals(-1,EspPlayers.formattingColor("§c§rPlayer"));
        assertEquals(-1,EspPlayers.formattingColor("Player"));
        assertEquals(0xff5555,EspPlayers.formattingColor("§b[VIP] §c"));
        assertEquals(BedTeam.BLUE,BedTeam.fromColor(0x0000aa));
        assertEquals(BedTeam.PINK,BedTeam.fromColor(EspPlayers.formattingColor("§dPlayer")));
    }
}
