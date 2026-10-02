# orven-bw

**Bedwars/PvP utils to lower your cortisol!** Maintained by **liwwyy**.
A modular Minecraft 1.8.9 client mod for Ornithe Gen 2 and OneConfig v1.

## Use

Install `build/libs/orven-bw-0.1.0+mc1.8.9.jar` in the OneClient instance's
`minecraft/mods` folder. Do not install the sources jar.

Press **P** to open this mod's OneConfig page. The hotkey is configurable in General
and works when the mod is disabled. General comes first, followed by ClickAssist.
Each section has its enable switch embedded in the accordion header; settings start
collapsed. Physical ClickAssist, Spam Clicking and Button Hold enable independently.
The General mod switch and optional scoreboard filter govern all three.

- **General:** mod switch, settings hotkey, and optional sidebar text filter. The
  default matching text is `Red`; case and formatting are ignored.
- **Physical Assist:** boost manual clicking above 4 CPS. Controls include left/right
  assistance, items, entity targeting, a player within four blocks, creative mode,
  and initial boost delay. Preserve block breaking defaults to off.
- **Spam Clicking:** hold middle mouse by default, or select Toggle. Choose left/right
  output and configure this feature's own filters, weapons, rates and exhaustion.
  Click through blocks defaults to on; turn it off to pause left spam on blocks.
- **Button Hold:** separate unassigned left/right toggle binds hold the configured
  attack/use binding until toggled off. Vanilla handles mining and item use.
- **HUD:** visibility switch and shortcut to OneConfig's HUD editor.

## CPS profiles

Physical assistance and spam have independent decimal settings, with these defaults:

| Level | CPS | Share of steady-session time |
| --- | ---: | ---: |
| High | 14.0 | 20% |
| Medium | 12.5 | 40% |
| Low | 9.5 | 40% |

Each session ramps toward the high level over **one second**, then varies through
shuffled one-second levels with short smooth transitions. Interval variation is enabled
by default and applies to both Hold and Toggle spam. The old hard cap and boost-chance
controls have been replaced by these profiles.

Physical assistance estimates cadence during warm-up, so it need not wait for five
presses to fill the one-second counter. Once eligible, it starts with a positive boost
and gradually supplies the difference between your manual rate and the selected total
rate. It can generate more than one extra click per manual press. Stopping manual input
ends assistance; generated clicks cannot activate it.

Each feature has its own exhaustion controls: by default, after eight seconds, check
every four seconds with a 30% chance to slow toward 8–9 CPS for 600 ms.

Rates are targets, not a hard limit on manual input. Physical clicks are preserved.
Vanilla's 20 Hz tick loop limits dispatch precision; missed clicks are not replayed after
stalls. Item-use rules and server interactions still apply. Native held-use repeats and
input from other mods are not included in this mod's generated-click counter.

## HUD

Add **ClickAssist CPS** through OneConfig's HUD editor. It follows the last boosted
button by default; left, right and both displays are also available. Default output:

```text
7 + 6 = 13 R-Cps
```

Editable placeholders are `{base}`, `{boosted}`, `{total}` and `{button}`. These are
rolling one-second click counts, so they remain integers even though target settings
support decimals. The HUD window naturally lags changes to the current target rate.
Native placement, scale, font, colors, background, prefix/suffix and profiles remain
customizable. Old default formats and the old ` CPS` suffix are handled automatically.

Menus, unfocus, pausing, world/player changes, rebinding, profile changes and disabled
global gates clear active automation. Held state is released or restored to real input.
Generated clicks use Minecraft's vanilla keybinding queue; button hold uses its held
binding state. No OS mouse events or inventory clicks are generated.

## Build

Gradle 9.8.0, JDK 27, Loom 1.18.2, Ploceus 1.18.1, Loader 0.19.5 and Feather Gen 2
1.8.9+build.2 are configured. Output targets Java 25, matching the supplied beta runtime.
OneConfig 1.2.9 APIs are extracted from the installed beta for compilation and tests;
OneClient supplies them at runtime. Dependencies are not bundled in the mod.

```sh
JAVA_HOME=/usr/lib/jvm/java-27-temurin ./gradlew clean build
```

If the local sandbox prevents Gradle from starting, use the already populated cache:

```sh
python3 scripts/build-cached.py
```

The fallback runs the tests and uses Tiny Remapper's MixinExtension to build an
intermediary jar. It requires the prior Gradle/Loom caches and the installed beta jar.
See [validation notes](docs/validation.md) and [source research](docs/research.md).
