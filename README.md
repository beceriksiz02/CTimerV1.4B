# Premium Timer — Android project

## What's genuinely implemented

**Reliability (Phase 1):**
- Timestamp-based timing — survives screen lock, backgrounding, Doze, process death, reboot
- Room database as the single source of truth; nothing is ever computed by a UI loop
- Foreground service + persistent notification with Pause/Resume/Stop/+1min actions
- Boot recovery, keep-screen-on toggle (Settings), graceful handling of denied notification permission

**Precision, speed, and history (Phase 2):**
- 4 display precisions: HH:MM:SS, MM:SS, MM:SS.t (tenths), MM:SS.tt (hundredths) — tap the
  time on the fullscreen view to cycle, or pick explicitly from Customize
- Decimal digits refresh at a speed matched to the precision chosen: ~120Hz for hundredths,
  ~30fps for tenths, 200ms for plain HH:MM:SS (no reason to redraw faster when the visible
  digit only changes once a second). This has zero effect on timing accuracy — it only
  changes how often the always-correct value gets redrawn.
- Session history: every stopped/completed run over 3 seconds is logged with start time,
  duration, planned vs actual, completed/abandoned status, and project/tags
- History screen: today / this week / this month totals, completed vs abandoned counts,
  average session length, longest session — no fabricated "productivity score"
- CSV export of full history via the system share sheet

**Customization (Phase 3):**
- 5 genuinely different visual styles per timer: Digital, Analog (clock face + tick marks),
  Minimal (bare numerals), Thick Disk (filled pie), Segmented Ring
- 5 font choices (Modern, Geometric, Monospace/Digital, Editorial Serif, Rounded) — built on
  Android's built-in generic font families, so there is zero risk of a missing-font crash
- Solid color backgrounds (pitch black by default) or a custom photo from your gallery
- Custom accent color per timer
- All of the above persists to the database (survives closing/reopening the app) via a
  non-destructive Room migration — existing timers and history are never wiped by an update

**Workflow (Phase 4):**
- Optional project + tags on any countdown timer, carried through into history
- In-fullscreen +1/+5/+10 minute buttons and a "Finish" (finish-early) action for countdowns,
  in addition to the existing notification actions
- Settings screen: keep-screen-on toggle, and "Reset all app data" gated behind an explicit
  confirmation dialog that names exactly what gets deleted

## What was deliberately left out, and why

These are real gaps, not oversights — each one was cut because doing it properly (rather than
half-working) would have taken meaningfully longer than the time available, and a half-working
version of any of these risks being worse than not having it:

- **Image crop/zoom/pan/rotate/blur/brightness controls** — the gallery photo picker works and
  the chosen photo displays as the background, but you can't yet reposition or edit it after
  picking. A real crop/pan UI needs its own gesture-handling surface to do safely.
- **Drag-to-reposition layout editor** (moving/resizing individual elements freely) — this is
  the single largest remaining item in the spec and deserves dedicated time, not a rushed pass.
- **Gradient backgrounds/progress, HSL/RGB/alpha color inputs** — only solid swatches + a photo
  right now.
- **Pomodoro/routine sequences with auto-start chaining** — genuinely reliable auto-chaining
  needs its own state machine layered on top of TimeEngine; adding it hastily risked breaking
  the countdown/stopwatch reliability this whole project is built around, so it was skipped
  rather than rushed.
- **Home-screen widgets, Quick Settings tile, scheduled/reminder starts.**
- **Weekly/monthly visual timeline graphs** (the numbers exist in History; only the chart is missing).

## How things were verified without a full Android build

This environment can't reach Google's Maven repositories, so the full Gradle/Android build has
never actually been run here — that step needs Android Studio on your machine. What WAS verified,
concretely, not just reasoned about:
- `TimeEngine`'s core math was extracted into standalone Kotlin, compiled with a real Kotlin
  compiler, and executed against the 7 reliability scenarios from the original spec — all pass.
- `formatWithPrecision`'s output was compiled and executed against edge cases (zero, negative,
  hour rollover, tenths/hundredths truncation) — all pass.
- Every Kotlin file was checked for brace/parenthesis balance.
- Every changed function signature was grep-verified against every call site for matching
  parameter names/order.
- The FileProvider authority string was checked against `applicationId` for CSV export to work.

## How to build the APK (one-time, ~5 minutes)

1. Install **Android Studio** (free): https://developer.android.com/studio
2. Unzip `PremiumTimer.zip` anywhere on your computer.
3. Open Android Studio → **Open** → select the unzipped `PremiumTimer` folder.
4. Let it sync (downloads the Gradle wrapper + dependencies — first time only, needs internet).
5. **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
6. Click **"locate"** in the notification when done, or find it at
   `app/build/outputs/apk/debug/app-debug.apk`.
7. Copy that `.apk` to your phone and install it (allow "install from unknown sources" once).

If step 4 shows any red error text in the sync log, send it back — that's the one class of
problem that genuinely cannot be ruled out without a real compiler+SDK in the loop.
