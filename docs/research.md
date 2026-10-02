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


## Settings and new input modes (2026-10-02)

Inspected the legacy branches of [BehindYouV3](https://github.com/Polyfrost/BehindYouV3/blob/legacy/src/main/kotlin/org/polyfrost/behindyou/client/BehindYouConfig.kt)
and [OverflowAnimationsV2](https://github.com/Polyfrost/OverflowAnimationsV2/tree/legacy).
BehindYou uses OneConfig accordion trees, an always-visible master switch, native
keybinds and RadioButton controls. Overflow uses category metadata on properties to
create tabs. Our layout uses category metadata plus native accordion trees, with
`collapsed=true`, supported in the installed OneConfig 1.2.9 ConfigScreen bytecode.
Hidden flat aliases retain existing config values and share fields with the new controls.
DeepWiki was consulted for OneConfig controls/keybinds; source and installed SDK checks
were used to verify the actual implementation.

Raven-bS ClickAssist's left/right paths call AWT Robot mouseRelease/mousePress with
button masks 16/4 and ignore their synthetic mouse events. Raven AutoClicker calls
KeyBinding.onTick and ReflectionUtils.setButton. Feather Gen 2 names the vanilla queue
method KeyBinding.click; its held-state setter is KeyBinding.set. Our generated clicks
use that queue; button hold uses that setter without modifying OS/LWJGL mouse state.
This is **not the exact Raven ClickAssist input method**. No comparison can establish
that a server will accept automated input. The rate envelope is a configurable pacing
feature, not an anti-cheat guarantee.

The scoreboard gate follows vanilla GameGui's team-color sidebar slot (3 + color id),
falling back to slot 1. It checks the displayed title and last fifteen non-hidden scores,
including team prefixes/suffixes, strips formatting and matches a case-insensitive
substring. Empty text or no sidebar never matches. The mapping MCP verified Scoreboard
and Team method names; generated Feather source verified vanilla sidebar selection.

## Weighted profile redesign (2026-10-02)

The new layout constructs General first, before hidden compatibility aliases. This
matters because OneConfig's `buildCategories` uses insertion order even when earlier
properties are hidden. Each native accordion starts with a boolean property without
a visualizer, which `buildAccordionNode` recognizes as its embedded header switch.
Physical Assist, Spam Clicking, Button Hold and HUD follow under the ClickAssist tab.

The installed `1.8.9-ornithe-1.2.9.jar` was inspected with javap. Its keyboard event
mixin converts LWJGL codes through `KeyCodes.fromLegacy` before dispatching OneConfig
key events. `InputConstants.KEY_P` is SDL code 19, not LWJGL code 25 or GLFW code 80.
The middle mouse constant is 2. The new settings binding uses those actual installed
constants. `OneConfigUI.open(new ModConfigRoute("orven-bw.json", "General"))` opens the
mod page, and `HudManager.openEditor()` opens the native HUD editor.

Physical and spam rate profiles independently select 14 / 12.5 / 9.5 CPS with steady
slot weights 20 / 40 / 40 percent. A shuffled five-slot bag fixes those proportions;
150 ms interpolation avoids jumps. Both hold-spam and toggle-spam call the same
fractional-credit scheduler, with optional ±15% interval-weight variation. Warm-up uses
manual cadence; regular counting and freshness checks prevent synthetic self-activation
and continuation after manual clicking stops. The old one-extra-per-press rule and
rolling hard cap have been removed. This materially changes physical-assist behavior.

The one-time config migration copies previous timing/exhaustion/item filters into the
new independent controls, changes the previous default 1200 ms ramp to 1000 ms, and
preserves nondefault ramp durations, binds and other saved gates. The new tier values
start at the requested defaults. Existing explicit Preserve block breaking values
remain; new configs and the control's reset default are off. Old cap/chance keys remain
hidden for compatibility and no longer affect scheduling. HUD format compatibility
moves the letter behind the total and suppresses the old redundant CPS suffix.


## Held-click trigger and smaller accordions (2026-10-02)

Asked [DeepWiki about organizing the current OneConfig beta UI](https://deepwiki.com/search/we-use-oneconfig-v1-beta-129-o_6a15e88c-5dbe-4f3b-a74b-c7a4ff25a229),
then checked the actual OneConfig source and cached 1.2.9 SDK. DeepWiki recommended
categories/subcategories, smaller accordions and dependent controls. Its suggestion
of nested accordions is unsuitable here: `SettingIndex.buildAccordionNode` collects
only immediate Property children. The implementation uses sibling trees, two categories
(General first, ClickAssist), and four feature subcategories. `ConfigScreen.AccordionRow`
supports `collapsed=true` with a visible boolean header; `Property.addDisplayCondition`
provides reactive hiding. No unsupported full-width metadata or custom visualizer is used.

Feather MCP verified `ClientPlayNetworkHandler.getOnlinePlayers`. Named Minecraft source
confirms `Minecraft.getNetworkHandler`, `PlayerInfo.getProfile().getName`, and player
usernames. Nearby detection compares names to tab-list profiles, excludes self/dead/
spectators and uses squared distance <=16. Missing network information fails closed.
This is the user-selected NPC rule: listed player-shaped NPCs can still qualify.

Held Click polls existing LWJGL physical input for the current attack/use bindings;
Button Hold modifies KeyBinding state only and cannot start that timer. Timers require
continuous eligibility. The optional instant bypass requires entity + sword/stick +
nearby player regardless of disabled optional filters. It skips only the hold delay.
Vanilla item-use is preserved: the generated queue pauses while `hasItemInUse()` is true.
Both spam triggers use the same timing/profile implementation with one source per side.

Schema 3 retains field-sharing hidden root aliases, so prior bindings and profile values
survive the tree reorganization. Schema 2 advances without rerunning legacy migration.
