# Source/API checks

- [Ornithe development page](https://ornithemc.net/develop/) and
  [official Gen 2 Fabric template](https://github.com/OrnitheMC/ornithe-mod-template):
  build layout, loader conventions, Loom/Ploceus repositories.
- [PolySprint legacy build](https://github.com/saadndm/PolySprint/blob/legacy/build.gradle.kts):
  Gen 2 configuration, Feather dependency, OneConfig v1 coordinates, Java 25 runtime.
  This project uses a single-version Java build rather than its multi-version infrastructure.
- [Raven-bS](https://codeberg.org/strangerrrrs/raven-bS/), files
  `module/impl/combat/ClickAssist.java`, `AutoClicker.java`, and `module/impl/client/Settings.java`.
  ClickAssist generates one probabilistic extra OS mouse press with left/right filters and
  recursion suppression. Here, real presses queue delayed vanilla keybinding clicks;
  generated clicks never enter the physical-input hooks, so no recursion flag is needed.
  Its optional fixed right CPS threshold becomes a shared configurable per-channel threshold.
  Global Raven weapon options are available directly in this feature's settings.
- AutoClicker review: schedules repeated clicks while held, handles block breaking and optional
  inventory clicks, uses reflection for hovered slots, and randomizes delays. This release
  incorporates block-preservation and optional timing variation, but needs a new physical press
  for every boost and discards overdue work. Inventory automation and repeated held clicks
  are not part of ClickAssist.
- [EvergreenHUD legacy CPS HUD](https://github.com/Polyfrost/EvergreenHUD/blob/legacy/src/main/kotlin/org/polyfrost/evergreenhud/client/hud/CpsHud.kt):
  rolling input histories, editable HUD text and native OneConfig HUD integration.
- OneConfig APIs were queried through the **DeepWiki MCP** for `Polyfrost/OneConfig`, then
  verified against source and `javap` of the **actual installed 1.2.9 jars**. Java packages:
  `org.polyfrost.oneconfig.api.config.v1`, `...annotations`, `...api.hud.v1`.
  `Config.preload()`, `TextHud`'s five-argument constructor, `Hud.Category.getCOMBAT()`,
  `HudManager.register(Hud, String, String)`, and slider/switch/dropdown/text annotations
  were checked against the beta bytecode. No obsolete `cc.polyfrost.oneconfig` API is used.
- **ornithe_feather_gen2 MCP**: queried class/member names, owners, descriptors, and fields.
  The registered MCP was also launched via its SDK stdio client to get its updated structured
  lookup tools, because this conversation's tool schemas still describe the earlier server.
  Minecraft's generated Feather source was inspected to verify input hook order and targets:
  mouse/keyboard `KeyBinding.click` call sites, followed by
  `LocalClientPlayerEntity.hasItemInUse()` before vanilla consumes attack/use clicks.
- [Gradle current release](https://services.gradle.org/versions/current) and
  [Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html):
  9.8.0 and Java 27. Distribution SHA256 is pinned in the wrapper configuration.
  Compilation targets Java 25 so the existing pack can launch the result.

All checks were performed on 2026-10-01. Source references are retained locally under
ignored directories; dependency versions are pinned in the project build.

Inspected reference commits:

- Raven-bS: `44d82ec09b83d03f7c8af47cf97823f40bb33745`
- PolySprint legacy: `97c9a8007fd3e80f1c3e1b615774168833496ca3`
- EvergreenHUD legacy: `11d6a6021727ca5ab868b43929ac08c5cae87553`
- OneConfig source: `a7a541eef0855a636ccda32c279a0693e72968a5` (beta bytecode is authoritative)
