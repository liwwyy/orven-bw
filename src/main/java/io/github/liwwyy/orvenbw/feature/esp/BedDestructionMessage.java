package io.github.liwwyy.orvenbw.feature.esp;
import java.util.regex.Pattern;

/** Server chat corroborates bed status; ordinary player chat must not remove waypoints. */
public final class BedDestructionMessage {
    private BedDestructionMessage() {}
    private static final Pattern HYPIXEL = Pattern.compile("(?i)^BED DESTRUCTION!\\s+(red|blue|green|yellow|aqua|white|pink|gr[ae]y)(?:\\s+team)?(?:'s|’s)?\\s+bed\\s+(?:was|has been)\\s+(?:destroyed|broken)\\b.*$");
    private static final Pattern PIKA = Pattern.compile("(?i)^(?:BED DESTRUCTION!\\s+)?(?:the\\s+)?(red|blue|green|yellow|aqua|white|pink|gr[ae]y)(?:\\s+team)?(?:'s|’s)?\\s+bed\\s+(?:was|has been)\\s+(?:destroyed|broken)\\s+by\\b.*$");
    public static BedTeam parse(String formatted) {
        String text = BedwarsSidebar.clean(formatted);
        // A chat prefix/colon distinguishes quoted player messages from anchored server announcements.
        if (text.indexOf(':') >= 0) return BedTeam.UNKNOWN;
        for (Pattern pattern : new Pattern[]{HYPIXEL,PIKA}) {
            var match = pattern.matcher(text); if (match.matches()) return BedTeam.fromName(match.group(1));
        }
        return BedTeam.UNKNOWN;
    }
}
