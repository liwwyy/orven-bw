# orven-bw

**Bedwars/PvP utils to lower your cortisol!** Maintained by **liwwyy**.
A modular Minecraft 1.8.9 client mod for Ornithe Gen 2 and OneConfig v1.

## Install and settings

Install `build/libs/orven-bw-Ornithe-0.2.0+mc1.8.9.jar` in your OneClient instance's
`minecraft/mods` folder. Do not install the sources jar. This build targets Java 25
and the published OneConfig 1.2.18 SDK APIs.

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
  within four blocks, and Advanced requires a listed item by default. Crouch cancel is on by default; holding
  your sneak binding or crouching cancels this mode and restarts the eligible timer. The 250-ms timer
  starts after all filters pass, resets when eligibility/input stops, and can be changed.
  Turn off Entity target to allow blocks/air. Optional Instant activation bypasses the
  delay only with entity + allowed item + nearby player; the ramp still applies.
- **Button Hold:** embedded enable switch with unassigned left/right toggle binds.
  Latches vanilla attack/use state until toggled off and suppresses generated clicks on
  its side. Its synthetic state cannot activate mouse-hold spam.
- **Hit effects:** floating observed-hit count and damage below target health, without
  a heart icon. Normal hits are green, combos of three orange, combos of five purple,
  and critical hits gold. Customize combo reset, animation duration and vertical offset.
- **Advanced:** shared CPS levels, click timing, variation, ramp and exhaustion, plus
  native editable item lists for each clicking mode, then Debugging at the bottom.
  Groups start collapsed.
- **HUD:** visibility and shortcut to OneConfig's HUD editor.

Weapon filters apply to physical/activation-key **left** clicks; their right block-only
filters remain separate. Mouse-hold item filtering applies to both selected sides.
Each filter uses OneConfig’s native item picker: add or remove custom items directly.
Defaults include a sword, stick and raw beef (`minecraft:beef`). Choosing any sword
enables all swords; other items match exactly. Existing weapon selections migrate to
item lists once, with raw beef added.
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
ramp approaches the high level. **Rocky gradual ramp** is enabled by default: randomized
bursts and short plateaus start around 3.1–4.6 CPS, reach 6.3–9.7 CPS, then approach
the configured high level. Each session draws fresh knots. Physical boosting respects
the manual-rate floor, and low configured targets clamp these example ranges. Disable
Rocky gradual ramp to use the existing linear rise. A shuffled five-slot bag selects one high, two medium
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
Migration runs once and preserves weapon filters and existing global state. Schema 5
adds the editable item lists while retaining the shared CPS profile.

## CPS HUD

Add **ClickAssist CPS** through OneConfig's HUD editor. Default output:

```text
7 + 6 = 13 | 4 CPS
```

The calculation follows the busier side's **recent physical + generated** clicks. The
final pair always means left total | right total over one second; neither side's total
is replaced by the dominant side. Selection uses a 250-ms activity window and updates
at 50-ms intervals, rather than sticking to the last generated button. The uppercase
`CPS` suffix is optional. **Hide when unused** and **HUD hide timeout (ms)** are in
the HUD’s editor settings; the timeout defaults to 1000 ms after the last counted click.
A new click shows it immediately, and the editor preview remains visible. Native placement, scale, font, colors and profiles remain editable.

Format placeholders: `{base}`, `{boosted}`, `{total}` follow the dominant side;
`{left}` and `{right}` are the separate raw totals. Counts are integers even though CPS
targets support decimals. Vanilla held-use repeats and input injected by other mods are
not included in this mod's generated-click counter.

## Click debugging and charts

Enable **ClickAssist → Advanced → Debugging → Debug mode** to append to one file:
`config/orven-bw/click-debug.jsonl`. Debugging is off by default and can record physical
mouse input while the main mod is disabled, including clicks in game menus. Each restart
appends a new session ID to the same file. **Clear debug cache** empties that file in
order with pending writes; recording can then continue in it.

Records include epoch timestamps in milliseconds, monotonic/elapsed nanoseconds, button,
action, sequence number, session and source. Real mouse press/release events also retain
LWJGL’s native event timestamp. Remapped keyboard attack/use presses are identified
separately. Generated events identify `cps_boost`, `spam_click`, `mouse_hold_click`, or
`button_hold`; Button Hold also records held-state transitions. Recording timestamps
happen on the input thread; a background writer appends and flushes each record.

Generated `queue` records measure submissions to vanilla’s keybinding queue. They do
not prove a server accepted an attack. Vanilla repeated use caused by a held key and
other mods’ injected input are not counted as additional generated events.

Run the dependency-free viewer from this repository:

```sh
python3 scripts/click_debug_viewer.py
```

Open <http://127.0.0.1:8765>. By default it looks in:
`/home/user/.local/share/Polyfrost/OneClient/clusters/1.8.9 OC/config/orven-bw/`.
For another instance use `--log /path/to/click-debug.jsonl` or `--directory /path/to/config/orven-bw`.
The viewer reads the log without changing it. It refreshes live and retains the latest
20,000 records; increase this with `--max-events` for longer comparisons.

Filter by session, button and generating method. Scroll to zoom, drag to pan, or use the
range sliders. Hover a click for its timestamp and method. Charts show the click timeline,
rolling one-second CPS, same-button inter-click intervals and a 10-ms interval histogram.
Physical/generated counts, median interval and interval variation summarize the visible
range. Export filtered JSON to keep an excerpt. These measurements help inspect timing
patterns; they cannot determine what a server’s anti-cheat will accept.

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

Dependencies resolve from public Maven repositories; no local OneClient installation,
ignored reference folder, beta extraction, or repository secrets are needed to build.

```sh
JAVA_HOME=/usr/lib/jvm/java-27-temurin ./gradlew clean build
```

The version lives in `gradle.properties` as `mod_version=x.x.x`. Bump it for each new
release (patch for fixes, minor for features). The current version is **0.2.0**. The
runtime jar is `orven-bw-Ornithe-{version}+mc1.8.9.jar`; the mod metadata uses the same
version with the Minecraft suffix. Sources jars are for development only.

GitHub Actions builds and tests pushes to `main`, version tags, pull requests and manual
runs. Successful main/tag builds publish a release named `v{mod_version}` with the
runtime jar and its SHA-256 checksum. An existing release is retained; bump `mod_version`
to publish the next one. Explicit version tags must match `gradle.properties`. Pull
requests build artifacts without publishing. Actions use JDK 27 and target Java 25.

Reference sources, including Raven-bS and the supplied hit-show fragment, are under
ignored `.reference/`. Original icons are under ignored `icons/`; the ClickAssist SVG is
copied into tracked mod resources so builds remain reproducible.
See [validation notes](docs/validation.md) and [source research](docs/research.md).
