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

## Full settings/HUD redesign and hit effects (2026-10-04)

Queried the **oneconfig-v1 MCP** first, including its official
[Buttons](https://docsv1.polyfrost.org/configuration/available-options/buttons),
[Decorations](https://docsv1.polyfrost.org/configuration/available-options/decorations)
and [Keybind option](https://docsv1.polyfrost.org/configuration/available-options/keybind-option)
references. Its answer explicitly could not verify the newer tree/route APIs.
Cross-checked with [DeepWiki](https://deepwiki.com/search/verify-current-oneconfig-v1-be_992cd713-7e22-4148-9874-00c6dad51d78)
and the OneConfig source/cached exact 1.2.9 bytecode.

Verified `@Button.icon`, collector icon/runnable metadata, SettingLabel icon rendering,
`OneConfigUI.open(ModConfigRoute)`, Info headings and native keybind actions. The docs'
method-style Info example does not match the field annotation used by this SDK.
Include-only fields begin hidden, so visible options explicitly remove hidden metadata
and get supported Visualizer classes and descriptive title/description metadata.
Node.description is a public field in 1.2.9, not a getDescription method or ordinary
metadata entry. Null key/mouse arrays on unassigned/mouse-only SDK binds are normalized
when copying old bindings, while new callbacks remain attached to their own sides.

The layout now uses visible top-level mod/feature switches, two categories in insertion
order and sibling collapsed trees. All timing parameters are shared. Schema 4 chooses
an active spam profile when physical assistance was disabled, otherwise the physical
profile; previous independent values remain in hidden keys. Left/right keybind spam has
independent held/latched state; mouse-hold spam is independently enabled.

The HUD's default calculation follows combined recent input (250 ms), with fixed
left | right one-second totals and optional uppercase CPS. This is modeled after
EvergreenHUD's paired totals without locking the calculation to the last generated side.

Inspected the user-supplied `.reference/hit_show.java`; it is a partial Forge rendering
fragment, not a complete hit detector. New code independently implements reticle-relative
health text and a sliding/fading hit/damage popup without its heart icon. Feather MCP
verified ClientPlayerInteractionManager.attackEntity (m_41462154) and GameGui.render
(m_94668477). Generated Minecraft source verified their signatures, attack packet flow,
TextRenderer.drawWithShadow, Window dimensions, health access and vanilla critical
eligibility conditions. Attacks open a 750-ms window for observed health decreases;
health updates cannot prove attribution or divide coalesced hits. No Forge code was
copied into the Ornithe implementation.

Randomized sessions select weighted CPS levels, bounded level offsets and dwell times,
blend transitions, apply slow correlated rate variation, then apply exhaustion last.
The fractional scheduler adds symmetric triangular interval variation. Configurable
bounds, tick dispatch and stall cancellation remain; this is not a human/anti-cheat guarantee.
Raven-bS is now under ignored `.reference/raven-Bs`; original icons are ignored under
`icons/`, with the supplied ClickAssist icon copied into tracked runtime resources.

## Native item picker and click debug follow-up

The oneconfig-v1 documentation MCP was consulted for ItemList; it did not return the
needed API detail. DeepWiki was consulted for Polyfrost/OneConfig and confirmed the
native annotation's String[]/List<String> support. The checked-out annotation source and
`javap` of the actual cached **config-impl-1.2.9.jar** verify `@ItemList` and its
`Visualizer.ItemListVisualizer`; these installed beta APIs determine compatibility.
Item values use registry identifiers and matching remains the consuming mod's job.
The item's MCP-verified Minecraft source registers raw beef as **minecraft:beef**;
`raw_beef` is not its 1.8.9 registry name. Any selected `SwordItem` enables every
`SwordItem`, while other registry items require identity equality.

Ornithe mappings/source inspection verified `Entity.isSneaking`, `Item.REGISTRY`,
`net.minecraft.resource.Identifier`, and the raw/cooked beef distinction. Crouch cancel
also checks the currently bound physical sneak key, so it respects remapping.
Real mouse events are observed at LWJGL `Mouse.next` return and retain native nanosecond
and observation millisecond timestamps. Artificial clicks are recorded at the existing
`KeyBinding.click` submission sites, with the active mode identified separately.

## Public CI dependencies and releases

Polyfrost's published Maven metadata now lists OneConfig 1.2.18 for the modular SDK
and 1.8.9 Ornithe platform. The earlier 1.2.9 beta is absent from those published
versions. Builds now resolve the 1.2.18 compile-only SDK from
https://repo.polyfrost.org/releases/ instead of extracting jars from a local instance.
The platform dependency is remapped by Loom; the config/HUD/UI/internal modules and
their dependencies come from public Maven repositories, including Google's AndroidX
repository for Compose dependencies. OneConfig is still supplied by OneClient at runtime
and is not bundled in the mod jar. The declared minimum is now 1.2.18.

DeepWiki was asked about category navigation; the SDK's internal module supplies
ModConfigRoute. The existing General and ClickAssist page shortcuts are retained.
GitHub action versions were checked against their official release APIs; actionlint
validates the build/release workflow. Release version/name come from gradle.properties,
with x.x.x validation and tag/version agreement enforced before compiling.
