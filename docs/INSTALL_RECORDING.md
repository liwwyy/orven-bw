# orven-bw Click Recording 0.5.1

This build records your mouse clicks automatically. Click assistance, spam clicking,
mouse-hold clicking, Button Hold, hit effects and the CPS HUD all start disabled.

## Install

1. Use **OneClient 1.8.9** with OneConfig **1.2.18 or newer** and Java **25 or newer**.
2. Close Minecraft and open that instance's `mods` folder.
3. Remove any existing **orven-bw** jar. Keep the other OneClient dependencies installed.
4. Add **orven-bw-Ornithe-0.5.1+mc1.8.9.jar**, then launch normally.

No settings changes are required. The regular and recording jars have the same mod ID;
install just one of them at a time.

## Record and share

Play and click normally. The recorder captures mouse presses and releases in gameplay
and menus. Logging works with the main mod and Click Assist switches off.

After recording, close Minecraft and send this file from the instance folder:

```text
config/orven-bw/click-debug.jsonl
```

The file appears after the first recorded click. It contains native mouse-event timing,
millisecond observation timestamps, button states and session IDs. In gameplay it also
records consumed click actions and outgoing interaction packet submissions, linked by
origin and action IDs where available. These are client-side timestamps, not server
receipt times. Restarts append to this one file. Menu clicks are included; the chart
viewer lets you select the gameplay portion with its time-range controls.

## Optional settings

Press **P** to open **orven-bw — Click Recording**. Its settings are separate from the
regular build, so an old enabled Click Assist profile does not affect first use.
Other features remain available but require deliberate enabling; turning on the main
switch alone does not turn on assistance. Your settings choices persist across restarts.

Debug mode and **Clear debug cache** are at **ClickAssist → Advanced → Debugging**.
Clearing empties the existing file, and recording continues there when Debug mode is on.
Keep the clicking utilities off while collecting an unassisted sample. If enabled, their
queued clicks are logged separately as artificial events.

## Analyse

The viewer is available on both **main** and **debug/click-recording**. It uses only
Python's standard library:

```sh
python3 scripts/click_debug_viewer.py --log /path/to/click-debug.jsonl
```

Open http://127.0.0.1:8765. Compare native input, planned artificial clicks, observed
queue clicks, consumed actions and outgoing packet submissions with the stage selector.
Native mouse timing uses event-to-event differences rather than Minecraft's buffered
input processing times. Hover details identify missing-native-time fallback records.
