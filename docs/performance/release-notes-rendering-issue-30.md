# Release Notes rendering — issue #30

Issue: https://github.com/hosizoraru/KeiOS/issues/30. The reporter uses an OPPO Reno 11 on Android 16
and KeiOS 1.17.0 / 11700999. The uploaded screenshot shows the page dimmed after opening KernelSU
Release Notes, with the entire Sheet panel absent. There are no device logs in the issue.
The maintainer reports that Xiaomi 17 Pro / HyperOS 4 does not reproduce the failure, reinforcing
the need for acceptance on the affected device rather than treating a successful software render
as proof of an OPPO fix.

## Reproduced size hazard

Source baseline: `1fbbc7cb58f2c26e6a935881d8e004b6dfb0a53d`. The public KernelSU v3.3.1 release body
is about 6.4 KB with 58 newline characters. The parser retains its headings, lists, and links. The
problematic layout placed the entire eager Markdown column inside one `SheetSectionCard`.

The native-graphics Robolectric fixture at API 35, 360×804 dp / xxhdpi measures that card at **16,131
physical pixels high** in a **2,412-pixel-high window**. The card's `LiquidSurface` uses the backdrop
effect/export path with those element dimensions. This is an oversized render-target hazard: the
parent's scroll clipping does not make the child material a viewport-sized element.

This reproduces the unsafe geometry, not the OPPO's hardware failure. Software rendering can still
show the original text; without that phone's HWUI logs or GPU capture, its exact failing limit and
driver behavior remain unconfirmed.

## Change

The body card is capped at three quarters of the window height. Its Markdown uses a lazy block
list; contiguous table rows remain one table. Eager and lazy rendering share the same text, link,
selection, table, quote, task, and code views. The outer Sheet scroller remains, so the summary and
actions can scroll into view at small detents. Short bodies keep their natural height.
The Sheet's opening detent, height range, and drag implementation remain unchanged; the cap applies
to the document card, not to the draggable panel.

`LiquidSheetContentScroll` tracks the outer and inner scroll sources by identity. A scrolled inner
list must remain visible to drag arbitration even if the parent registers later, and removing one
container must preserve the other's position. The lazy Sheet helper also accepts explicit padding,
so the card owns its content insets without adding a second navigation-bar or shadow inset.

No glass effect parameters, colors, typography, fetching strategies, or dependencies change.
Copy and translation still use the complete raw Markdown instead of only composed/visible rows.

## Verification

The native fixtures measure the final body material at 1,809 px on the phone, including 1.5× font
scale, and 1,200 px on a 1,000×800 dp tablet. Tests cover reading the end of the real KernelSU body,
opening its final changelog link, copying its complete Markdown, collapsing and swiping the summary
to regain document space, dragging the panel down and up with its bottom anchored, retaining list
ownership of a downward drag mid-document, rich Markdown, and empty/fallback bodies.

The short-body light/dark fallback captures have byte-identical RGBA pixels before and after.
These captures establish those fixtures' appearance; they do not establish GPU behavior on the
reported OPPO. Existing Sheet, Markdown-parser, and release-state tests also cover the shared paths.
The focused check passed 115 tests (30 app, 85 Sheet) and Release Kotlin compilation. The additional
drag coverage passed all 10 Release Notes rendering tests. No device/emulator target was available
for this change; no on-device GPU or frame-timing result is claimed.
The updated quick Compose QA gate also passed: debug compilation, parser tests, shared
recomposition regressions, all 10 rendering cases, and whitespace checks.

Run the focused checks:

```bash
./gradlew \
  :app:testDebugUnitTest \
  --tests os.kei.ui.page.main.github.sheet.GitHubReleaseNotesRenderingTest \
  --tests os.kei.ui.page.main.github.sheet.GitHubReleaseNotesDetailStateTest \
  --tests os.kei.ui.page.main.widget.markdown.AppMarkdownParserTest \
  :ui-liquid-glass:testDebugUnitTest \
  --tests 'os.kei.ui.page.main.widget.sheet.*' \
  :app:compileReleaseKotlin --max-workers=2

scripts/qa/compose_audit_gate.sh --quick --offline
```

Set `KEIOS_ISSUE30_CAPTURE_DIR` to capture the rendered fixtures and body sizes. The cloud uses
cached dependencies with `--offline`; local logs, original failing XML, image comparisons, and
final reports live in ignored `.planning/issue-30-release-notes/`.

The quick Compose QA gate includes these regression classes. Final on-device acceptance should
open KernelSU v3.3.1 on the reported OPPO, scroll to the end and back, resize the Sheet, select other
releases, and check copy/translation with the same display and material settings. Do not infer a
device frame-rate improvement from the JVM geometry checks.
