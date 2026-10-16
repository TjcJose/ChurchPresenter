# `:recording` — Agent Notes

Rules, structure and commands for this module only. The repo-wide rules are in the root `AGENT.md`.

## What it is

The **Recording** tab: pick the mixer's audio input, record it to an mp3, and save the take under
`yyyy.MM.dd.Period.Speaker.Title.mp3`. `RecordingManager` holds the state for the whole run; the tab
is only a view over it.

A real Gradle module of this build: `include(":recording")`, `implementation(projects.recording)`,
consumed by `:composeApp`.

It takes `:shared-ui`, `:strings`, `:icons`, `:core-models`, `:settings` and `:theme`, and nothing of
`:composeApp`'s.

## Seams to the app

- **`RecordingManager` outlives the tab.** Recording must survive the operator switching tabs, so the
  manager is owned beside the app (like `STTManager` and `QAManager`) and the pane only reads it. The
  tab composable is disposed on a tab change; the manager is not.
- **The app passes the manager in** through `MainDesktop` → `MainDesktopScope` → `TabContentPane`.
  With no manager the pane draws nothing, which is what the headless app tests compose.

## Layout

- **`RecordingManager`** is the state machine — `IDLE`, `RECORDING`, `PAUSED`, `STOPPED` — and the
  elapsed clock. The capture engine (audio input, mp3 encoding) hangs off it.
- **`RecordingTab`** is the tab: the transport row (new / record / pause / stop) over the elapsed
  clock. Still to come: the mixer picker, the level meter and the save panel.
- **`RecordingFormat`** holds the pure formatting the tab prints.

## Package

**`org.churchpresenter.recording`.**

## Rules

- `internal` stops at the module edge. What `:composeApp` calls is public; nothing else is.
- **Tests and screenshots live here**, beside the code.
- Icon-only buttons carry a `contentDescription` — `ActionIconButton` takes one, and the app's
  accessibility suite fails on a clickable with no name.

## Commands

```bash
./gradlew :recording:test :recording:detekt
./gradlew :recording:jacocoTestCoverageVerification
```
