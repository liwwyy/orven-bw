package io.github.liwwyy.orvenbw.feature.esp;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BedDestructionMessageTest {
    @Test void anchoredAnnouncementsMatchWhileQuotedChatDoesNot() {
        assertEquals(BedTeam.RED,BedDestructionMessage.parse("§lBED DESTRUCTION! §cRed Bed §7was destroyed by player!"));
        assertEquals(BedTeam.BLUE,BedDestructionMessage.parse("Blue's bed has been destroyed by player"));
        assertEquals(BedTeam.UNKNOWN,BedDestructionMessage.parse("[VIP] Player: BED DESTRUCTION! Red Bed was destroyed by player!"));
        assertEquals(BedTeam.UNKNOWN,BedDestructionMessage.parse("I heard the Red bed was destroyed"));
    }
}
