# orven-bw

**Bedwars/PvP utils to lower your cortisol!** Maintained by **liwwyy**.
A modular Minecraft 1.8.9 client mod for Ornithe Gen 2 and OneConfig v1.

## Install and settings

Install `build/libs/orven-bw-Ornithe-0.3.0+mc1.8.9.jar` in your OneClient instance's
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
- **Advanced:** shared fitted clicking behavior and the first-boost delay, plus
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

## Sample-fitted clicking profiles (0.3.0)

ClickAssist → Advanced → **Clicking behavior** replaces the old three-level bag,
independent jitter, fixed ramp and periodic exhaustion controls. Existing saved values
remain hidden compatibility keys; activation conditions and enabled states are preserved.

- **Humble** (default): fitted tempo states, variable startup shapes, bursts, dips, and
  correlated short/long interval categories. It learns from Wren’s fastest 60% of whole
  sustained bouts, keeping the slower portions inside those bouts. Rare 21–22 left-CPS
  target excursions are extrapolated: the recorded sample itself peaks at 20 left CPS.
- **Performative**: preserves interval-category rhythm but compresses tempo excursions
  toward the measured median (14 left / 6 right) by a factor of 0.35.
- **Separate left/right behavior** (default on): use distinct fitted button models.
  Turning it off uses independent left-model instances for both buttons, with right
  target CPS reduced by 10% after the shared target ceiling is applied.
- **Target CPS ceiling**: decimal slider, default 22, range 1–22. Physical input is
  never suppressed. Generated rolling totals are bounded by the ceiling rounded up,
  so a fractional target can alternate integer counts across one-second windows.

A bout requires ≥10 presses over ≥2 seconds, split at native clock resets or gaps
>750 ms. Rank by `(presses − 1) / duration`, retaining `ceil(60% × eligible bouts)`.
The fitted model retains 27 left bouts (1,291 presses, 91.9 active seconds) and 30 right
bouts (963 presses, 121.4 active seconds). liwwyy is a cross-check only, not blended in.
Startup types are building, steady and fast-then-settling; a universal low-to-high ramp
is not imposed. Short samples cannot establish long-session fatigue.

Tempo state residence times, transitions, interval-category transitions and quantile
curves are bundled as aggregates in `assets/orvenbw/click-profile-model.json`.
Runtime sampling generates new intervals rather than replaying recorded sequences.
No raw logs, session IDs or event timestamps are packaged in the jar.

The scheduler queues up to two genuinely due clicks per button per tick through vanilla
`KeyBinding.click`. It drops excess debt and work after stalls exceeding 250 ms.
Physical boosting supplies only the shortfall below the modeled total rate. Button hold
keeps its existing vanilla held-state behavior. Minecraft still processes input on ticks;
modeled deadlines, actual queues, packets and registered hits are different measurements.

Reproduce the fit from local samples (Python standard library only):

```sh
python3 scripts/fit_click_profiles.py \
  --samples-directory /home/user/Projects/orven-bw/click_logs \
  --output .reference/click-profile-report.json \
  --model-output src/main/resources/assets/orvenbw/click-profile-model.json
python3 -m unittest discover -s scripts -p 'test*.py'
```

The report includes exact sample hashes, selection summaries, liwwyy cross-checks,
five-fold validation split by whole bouts, and intended-versus-queued scheduler
simulations. The initial held-out CPS percentile error is ≤1 CPS, interval-category
share error ≤3.89 percentage points, and adjacent-interval correlation error ≤0.115.
Startup type shares also fall within the small sample’s 95% Wilson intervals.
These checks measure agreement with the two samples, not server acceptance.
Keep samples and reports local. The fit command rewrites the aggregate model only when
`--model-output` is provided; omit it for read-only fitting and report generation.

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
release (patch for fixes, minor for features). The current standard version is **0.3.0**. The
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

## Profile deadline logging

Each profile-generated queue row includes `profile`, `intended_ns_text`,
`intended_elapsed_ns_text`, `queued_ns_text` and `dispatch_lateness_ns_text`.
These are lossless decimal nanosecond strings. They supplement the existing actual
observation timestamps; they never masquerade as `native_event_ns` mouse input.
The viewer offers **Native mouse timing**, **Modeled deadlines (generated only)**,
and **Minecraft observation timing**. Old logs without deadlines remain readable.

The local viewer defaults to `click_logs`, discovering named subfolders, so `/liwwyy`
and `/wren` retain independent caches. Start with `python3 scripts/click_debug_viewer.py`;
use `--log` or `--directory` to inspect other data. Queue observations measure mod
submissions, not confirmed server hits.

Named sample pages default to All sessions so their charts cover the complete sample, matching the offline fit. Single-log viewing still defaults to the newest session.

Startup baselines are fitted separately for building, steady and settling starts. The first-quarter fit subtracts the initial press already emitted at activation, avoiding an artificial extra-click bias in the ramp.
