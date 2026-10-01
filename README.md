# orven-bw

A modular client utility mod for **Minecraft 1.8.9, Ornithe Gen 2, and OneConfig v1**.
Maintained by **liwwyy**. This first release adds ClickAssist and a customizable CPS HUD.

## Use

Copy `build/libs/orven-bw-0.1.0+mc1.8.9.jar` into the OneClient instance's `minecraft/mods`
folder. The source jar is for development and should not be installed.

In OneConfig, open **orven-bw → ClickAssist** and enable assistance. It starts disabled.
The HUD can be added through OneConfig's HUD editor under **orven-bw / ClickAssist CPS**.
It inherits native placement, scale, font, colors, background, prefix/suffix, and profiles.
Choose left, right, or both buttons and optionally hide it when idle. Its editable format is:

```text
{base} + {boosted} = {total}
```

The default text displays `7 + 6 = 13 CPS`, for example. All three values use a rolling
one-second window. Base CPS counts real presses of the configured attack/use bindings,
including remapped mouse buttons or keyboard keys, excluding keyboard repeat events.
Boosted CPS counts extra keybinding clicks actually queued by this mod. Neither counter
claims successful hits or block placements; vanilla/server interaction rules still apply.

## ClickAssist settings

| Setting | Default | Behavior |
| --- | --- | --- |
| Enabled | Off | Master feature switch |
| Total CPS cap | 13 | Per-button physical + assisted CPS budget |
| Activation CPS | 4 | Boost only when physical CPS is **strictly greater** than this |
| Requires player within reach | Off | Another living non-spectator player within a four-block sphere |
| Disable in creative | On | No assistance in creative mode |
| Left click / chance | On / 80% | At most one extra click for an eligible physical press |
| Weapon only | On | Swords and sticks by default; axes, rods, hoes and shovels are opt-in |
| Only while targeting an entity | Off | Requires an entity under the crosshair |
| Preserve block breaking | On | No extra attack clicks when targeting a block |
| Right click / chance | Off / 80% | Independent assisted use-click channel |
| Blocks only | On | Right assistance requires holding a block |
| Boost delay | 40 ms | Tick-dispatched delay; slider 10–150 ms |
| Vary boost timing | On | Up to ±25% delay variation |
| Show HUD | On | Gate for the independently editable HUD |

The activation threshold applies separately to both channels and replaces Raven-bS's
fixed optional right-click “Above 5 cps” setting. A nearby player need not be under the
crosshair. Spectators and dead players do not qualify; self never qualifies.

Menus, unfocus, pausing, world/player replacement, and keybinding changes clear the
histories and pending work. Assistance pauses while using an item, when dead, or when
spectating. It never simulates OS mouse input, overwrites held-key state, removes vanilla
hit delays, or clicks inventory slots. Holding a button alone never creates more boosts.
Queued clicks older than 250 ms are discarded instead of replayed after a stall.

The cap limits **added clicks**. Physical clicks are always preserved, so a burst of manual
input can exceed the configured total; the mod then adds no more clicks. Native held-use
repeats and clicks from other mods are not counted in this mod's physical/boosted HUD.
The vanilla 20 Hz input loop bounds timing precision and dispatches at most one extra
click per button per tick.

## Build

The build uses **Gradle 9.8.0**, **JDK 27**, **Loom 1.18.2**, **Ploceus 1.18.1**,
**Fabric Loader 0.19.5**, and **Feather Gen 2 1.8.9+build.2**. Java output targets **25**,
matching the supplied OneClient beta runtime. A JDK 27 installation must be available to
Gradle; the local installations are declared in `gradle.properties`.

OneClient supplies OneConfig **1.2.9**. This beta is not published in Polyfrost's public
Maven repository (the latest listed Ornithe artifact during setup was 1.2.7). The build
extracts the exact compile-only API jars from the installed beta into the ignored
`.reference/oneconfig-beta/` directory. Those dependencies are never bundled or copied
back into your modpack. Python 3 is used only for this extraction step.

```sh
JAVA_HOME=/usr/lib/jvm/java-27-temurin ./gradlew clean build
```

For a different installation location:

```sh
JAVA_HOME=/path/to/jdk-27 ./gradlew build \
  -PoneconfigJar='/path/to/OneConfig-1.8.9-ornithe-1.2.9.jar'
```

Required runtime: Minecraft 1.8.9, Ornithe Gen 2 / Fabric Loader ≥0.19.5, Java ≥25,
OneConfig v1 ≥1.2.9, and Fabric Language Kotlin ≥1.14.1+kotlin.2.4.20.
The beta pack already supplies these and Compose through OneConfig's dependencies.
No Fabric API or OSL module is required by this implementation.

## Development

`feature/FeatureRegistry` owns feature lifecycle and input routing. Future utilities implement
`ClientFeature`, register with the registry, and get their own OneConfig category. ClickAssist's
scheduler is independent of Minecraft and has deterministic tests for activation, caps,
probability, timing, expiration, stall recovery, channel isolation and resets.

```sh
JAVA_HOME=/usr/lib/jvm/java-27-temurin ./gradlew test
```

Raven-bS was cloned into `raven-Bs/` for reference and is ignored by Git. Other reference
repositories and local beta APIs are also ignored. ClickAssist and AutoClicker were reviewed
for behavior; the mod implementation is original and does not include Raven-bS code.
A standalone AutoClicker and the remaining Raven-bS modules are future features.
See [research notes](docs/research.md) and [validation notes](docs/validation.md).
