# Zeno 64

> Inferred from James's direction in chat on 2026-11-12 (no separate interview). Edit freely.

## What it is
A home screen and screensaver for James's own Insignia Fire TV (Fire OS 7.7, Android 9), run by Zeno,
his AI agent on a Raspberry Pi. It replaces Amazon's ad-filled home screen and screensaver. Personal,
non-commercial; goes on jamescronin.dev as a portfolio project once James has used it.

## Who and where
James, on the couch, 2-3 m from a 43" TV, with a D-pad remote (up/down/left/right, OK, Back, Home).
Evening use, often in a dim room. Glanceable from across the room; nothing needs a keyboard.

## Jobs
1. Get to Netflix or YouTube in one or two presses.
2. Glance at the time, the weather, and anything Zeno has to say.
3. Reach the TV settings that matter (Wi-Fi, apps, remote, updates) without Amazon's home screen.
4. When idle, become Zeno's channel instead of an ad.

## Brand commitments (pinned by James)
- The look of early-90s cable weather channel information screens ("Local Forecast" era, weather.com/retro
  as reference): sunset gradient, bevelled blue panels, yellow headings, ticker, clock. Borrow the era, never
  the Weather Channel's branding.
- Zeno is a hand-made 32x32 pixel sprite (tools/zeno_sprite.py). No AI-generated imagery anywhere.
- Must not read as generic AI output.

## Constraints
- 960 x 540 dp layout (1080p at 2x), ~40 dp safe margins.
- Amazon's launcher can't be disabled without root; Zeno 64 takes over Home with an accessibility watcher.
- Some Amazon settings screens are locked to Amazon-signed apps (Display & Sounds, Preferences).
