# `:hymn` — Agent Notes

Rules, structure and commands for this module only. The repo-wide rules are in the root `AGENT.md`.

## What it is

The **Hymn** tab: a church's hymnbook as files — one image folder per hymn, named by hymn number
(which may carry text, "129/", "051(甲)/"), each folder holding that hymn's slides as image files;
a piano mp3 and a vocal mp3 per hymn; and a `HymnTitle.txt` that maps number → title. The tab
lists them like Songs, plays the mp3s through VLC, and shows and pushes each hymn's slides exactly
like a presentation.

A real Gradle module of this build: `include(":hymn")`,
`implementation(projects.hymn)`. `:composeApp` is its only consumer.

It takes `:shared-ui`, `:strings`, `:icons`, `:core-models`, `:settings`, `:theme`, `:diagnostics`
and `:slides` — `:slides` carries the *whole* presentation experience: `PresentationViewModel`
loads a hymn's slides straight from its image folder (`loadImageFolder` — no rasterisation), and
`SlidesOutput` pushes them live over the same path the Presentation tab uses
(`Presenting.PRESENTATION`; a hymn *is* a presentation, so it needs no `Presenting` value of its
own). Nothing of `:composeApp`'s.

## Package

`org.churchpresenter.hymn`. State live with the tabs: the modules UI stays in whichever
subpackage `HymnTab` needs, `ui` for composables.

## What lives here

| File | Owns |
|---|---|
| `HymnItem.kt` | `HymnItem` — number, title, image folder, piano/vocal mp3s (files, not paths) |
| `HymnTitleParser.kt` | `object HymnTitleParser` — the `HymnTitle.txt` → number→title map |
| `HymnLibrary.kt` | `object HymnLibrary` — scanning the folders into a sorted `List<HymnItem>`, and the number↔folder-name matching rules |
| `HymnAudio.kt` | `HymnAudioKind`, `HymnMediaPlayer`, `openHymnMediaPlayer()` (VLC), `HymnAudioPlayer` — one mp3 at a time |
| `HymnViewModel.kt` | `HymnViewModel` — list, search, selection, audio state, rescan on settings change |
| `HymnTab.kt` (+ `HymnTabParts.kt`) | The tab: left list (search + columns: Number, Title, Add to Schedule, Piano, Vocal), right slide pane with next/previous and Go Live |
| `HymnSlidePane.kt` | The Presentation-style right pane over a `PresentationViewModel` |

## Rules

- **The mp3 files are matched by number, never by title.** A hymn's folder name is its number;
  a number like `051(甲)` may also match an mp3 named `051` when no exact file exists — see
  `HymnLibrary`'s doc. Keep the matching here and here only.
- **`HymnTitle.txt` is parsed tolerantly**: the real file is a `const groups = [ '001 咏', … ]`
  JavaScript array literal, but the parser must also read a plain `number title` line file. It only
  splits the first run of whitespace, because titles contain commas and other punctuation.
- **Open VLC is one step a test can replace**: `HymnAudioPlayer` takes the factory, and the
  tab/viewmodel never reach vlcj directly.
- **A hymn's slides go live through `SlidesOutput` with `Presenting.PRESENTATION`** — never invent
  a new presenting mode. The setter call, `setLiveSlide`/`setNextSlide`/`setSelectedSlide` and the
  notes mirror `:slides`' `goLiveAtSlide` exactly; `presentationShowSlide` is skipped, since a
  plain-image hymn has no deck (and so no build steps to play).
- **Slide display reuses `PresentationViewModel`** (`:slides`). A hymn's slides are the image files
  in its folder, loaded via `PresentationViewModel.loadImageFolder` in file-name order — no deck
  to parse or rasterise, so `deck` stays null and Next/Previous page the images directly. The
  thumbnail grid draws each slide the way the Pictures tab does: decoded downscaled through
  `PictureDecoder.decodeScaledOrNull` and cropped to fill a tile, with a loading/"unreadable"
  state — not the Presentation tab's `SlideThumbnail`.

## Verifying this module

```shell
./gradlew :hymn:test
./gradlew :hymn:detekt
```

The JaCoCo verification floors are `BRANCH` 0.80 and `COMPLEXITY` 0.77; everything else inherits
the repo's 0.85.