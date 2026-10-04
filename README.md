# orven-bw

**Bedwars/PvP utils to lower your cortisol!** Maintained by **liwwyy**.
A modular Minecraft 1.8.9 client mod for Ornithe Gen 2 and OneConfig v1.

## Install and settings

Install `build/libs/orven-bw-0.1.0+mc1.8.9.jar` in your OneClient instance's
`minecraft/mods` folder. Do not install the sources jar. This build targets Java 25
and the previously supplied OneConfig 1.2.9 beta APIs.

Press **P** to open General. New profiles start with the mod **off**; existing profiles
retain their saved enabled state. General comes first and ClickAssist comes second.
The icon-bearing ClickAssist button in General opens that page directly.

General includes the mod switch, a collapsed **Global Keybinds** menu for opening
settings and toggling the whole mod, and **Conditions** with optional scoreboard
filtering. Matching text defaults to `Red`, ignores colors/case, and checks the sidebar
title and visible lines. The settings key works while the mod is disabled.

ClickAssist is arranged in this order:

- **Physical CPS Boost:** visible enable and left/right switches. A single Filters
  accordion contains minimum manual CPS (strictly above 4 by default), nearby player,
  Creative mode, entity targeting, block-breaking preservation and right block-only
  filtering. Holding once does not activate physical boosting.
- **Spam click button:** visible enable, separate left/right activation binds, and
  Hold/Toggle mode. Left defaults to middle mouse; right is unassigned. Filters are
  separate from mouse-hold conditions. Both sides can run independently.
- **Mouse button hold click:** visible independent enable and left/right switches.
  Left defaults on, right off. Conditions require an entity target and a living player
  within four blocks, and Advanced requires a sword/stick by default. The 250-ms timer
  starts after all filters pass, resets when eligibility/input stops, and can be changed.
  Turn off Entity target to allow blocks/air. Optional Instant activation bypasses the
  delay only with entity + sword/stick + nearby player; the ramp still applies.
- **Button Hold:** embedded enable switch with unassigned left/right toggle binds.
  Latches vanilla attack/use state until toggled off and suppresses generated clicks on
  its side. Its synthetic state cannot activate mouse-hold spam.
- **Hit effects:** floating observed-hit count and damage below target health, without
  a heart icon. Normal hits are green, combos of three orange, combos of five purple,
  and critical hits gold. Customize combo reset, animation duration and vertical offset.
- **Advanced:** shared CPS levels, click timing, variation, ramp and exhaustion, plus
  separate weapon filters for each clicking mode. Groups start collapsed.
- **HUD:** visibility and shortcut to OneConfig's HUD editor.

Weapon filters apply to physical/activation-key **left** clicks; their right block-only
filters remain separate. Mouse-hold sword/stick filtering applies to both selected sides.
Right generated clicks pause during blocking, eating and charging to preserve vanilla
item use. Activation-key spam takes priority over mouse-hold spam on the same side;
physical assistance runs when neither spam trigger is active. One scheduler runs per side.

Nearby detection excludes self, dead players, spectators and usernames absent from the
server tab list. It compares profile usernames ignoring case, not decorated display
names. Missing network/tab information fails the check. NPCs placed in the tab list can
still qualify under this deliberately narrow rule.

## Shared CPS profile

| Level | Default CPS | Approximate steady-session share |
| --- | ---: | ---: |
| High | 14.0 | 20% |
| Medium | 12.5 | 40% |
| Low | 9.5 | 40% |

Each side has its own active session using the same Advanced settings. A one-second
ramp approaches the high level. A shuffled five-slot bag selects one high, two medium
and two low levels. With variation enabled, level values vary within ±6%, dwell times
range from 650–1400 ms, and transitions blend smoothly. Slow rate variation and bounded
triangular interval variation avoid repeated identical patterns. Exhaustion is applied
last to the varied rate: by default, after eight seconds, check every four seconds with
30% chance to target 8–9 CPS for 600 ms. All values are configurable.

Physical assistance supplies the difference between manual rate and the target. Manual
clicks are preserved. Rates are targets; Minecraft's 20 Hz input processing limits
spacing precision. Stalls drop pending generated work rather than replaying clicks.
These are bounded timing variations, not a guarantee of human behavior or server acceptance.

Schema 4 migrates the previously active spam profile if physical assistance was disabled;
otherwise it selects the physical profile. Old independent values remain stored as hidden
compatibility keys. The old activation bind moves to the matching new left/right bind.
Migration runs once and preserves weapon filters and existing global state.

## CPS HUD

Add **ClickAssist CPS** through OneConfig's HUD editor. Default output:

```text
7 + 6 = 13 | 4 CPS
```

The calculation follows the busier side's **recent physical + generated** clicks. The
final pair always means left total | right total over one second; neither side's total
is replaced by the dominant side. Selection uses a 250-ms activity window and updates
at 50-ms intervals, rather than sticking to the last generated button. The uppercase
`CPS` suffix is optional. Native placement, scale, font, colors and profiles remain editable.

Format placeholders: `{base}`, `{boosted}`, `{total}` follow the dominant side;
`{left}` and `{right}` are the separate raw totals. Counts are integers even though CPS
targets support decimals. Vanilla held-use repeats and input injected by other mods are
not included in this mod's generated-click counter.

## Hit-effect measurements

An attempted local attack opens a 750-ms confirmation window. A positive observed
health decrease registers one hit and its damage; attempted swings alone do not count.
Critical eligibility follows vanilla's local attack conditions. Healing never counts,
and target changes/context resets clear the chain. Combo expiry does not prematurely
cancel a longer configured floating animation.

The vanilla client cannot prove the damage source or separate several hits coalesced
into one server health update. The overlay therefore reports observed correlated health
changes, not invented damage or a server-confirmed per-attacker tally. Servers that do
not synchronize entity health cannot supply accurate hit/damage measurements here.

## Build

Gradle 9.8.0, JDK 27, Loom 1.18.2, Ploceus 1.18.1, Loader 0.19.5 and Feather Gen 2
1.8.9+build.2 are configured. Output targets Java 25. OneConfig APIs are compile-only;
no dependency or reference code is bundled in the mod.

With the original OneConfig beta jar available:

```sh
JAVA_HOME=/usr/lib/jvm/java-27-temurin ./gradlew clean build
```

Supply a different copy of **1.2.9** with `-PoneconfigJar=/absolute/path/to/the.jar`.
If the original instance has been removed but the previously extracted exact APIs remain
under `.reference/oneconfig-beta`, use:

```sh
JAVA_HOME=/usr/lib/jvm/java-27-temurin ./gradlew clean build -x prepareOneConfig
```

Reference sources, including Raven-bS and the supplied hit-show fragment, are under
ignored `.reference/`. Original icons are under ignored `icons/`; the ClickAssist SVG is
copied into tracked mod resources so builds remain reproducible.
See [validation notes](docs/validation.md) and [source research](docs/research.md).
