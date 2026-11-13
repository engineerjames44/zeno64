---
name: Zeno 64
description: A 1990s weather-channel information screen at dusk, run by Zeno, for one Fire TV.
colors:
  sky-top: "#1F1D6E"
  sky-mid: "#5A3E8A"
  sky-low: "#E0893B"
  panel: "#2E3DA6"
  panel-lit: "#3D4FC4"
  inset: "#26338F"
  bevel-light: "#8593EE"
  bevel-dark: "#12175A"
  ticker-ink: "#15185A"
  yellow: "#FFE14D"
  paper: "#F4F1E6"
  dim: "#B4BDF6"
  shadow-ink: "#0A0C30"
typography:
  page-title:
    fontFamily: "Jersey 15"
    fontSize: "26sp"
  clock:
    fontFamily: "Jersey 15"
    fontSize: "24sp"
  channel-name:
    fontFamily: "Jersey 15"
    fontSize: "28sp"
  body:
    fontFamily: "Jersey 15"
    fontSize: "22sp"
  label:
    fontFamily: "Jersey 15"
    fontSize: "18sp"
  ticker:
    fontFamily: "Jersey 15"
    fontSize: "20sp"
  hero-number:
    fontFamily: "Jersey 15"
    fontSize: "72sp"
rounded:
  none: "0dp"
spacing:
  gutter: "40dp"
  panel-gap: "16dp"
  inset: "16dp"
  row: "58dp"
components:
  channel-row:
    textColor: "{colors.paper}"
    height: "58dp"
  channel-row-focused:
    backgroundColor: "{colors.panel-lit}"
    textColor: "{colors.yellow}"
    height: "58dp"
  panel:
    backgroundColor: "{colors.panel}"
    rounded: "{rounded.none}"
  inset-box:
    backgroundColor: "{colors.inset}"
    rounded: "{rounded.none}"
    padding: "12dp 16dp"
  ticker:
    backgroundColor: "{colors.ticker-ink}"
    textColor: "{colors.paper}"
    height: "44dp"
  ticker-label:
    backgroundColor: "{colors.yellow}"
    textColor: "{colors.ticker-ink}"
---

# Design System: Zeno 64

## Overview

**Creative North Star: "The Station at Dusk."** Every screen is one page of a 1990s cable information channel
broadcasting from a sunset: a header with the station badge (Zeno), a two-line yellow title and the clock; one
big bevelled panel; a crawl along the bottom. The look is borrowed from the era, never from any channel's brand.
It is viewed from 2-3 m on a 960 x 540 dp canvas, driven by a D-pad. Calm, glanceable, a little playful, and
obviously hand-made: Zeno is a 32x32 sprite drawn pixel by pixel, the weather icons are 12x12 pixel grids, and
nothing is generated imagery.

## Colors

- **Dusk sky** (sky-top -> sky-mid -> sky-low, stops 0 / 0.45 / 0.86 / 1): the ground behind everything. The
  orange must stay visible in the band between panel and ticker; panels never cover the whole sky.
- **Royal panel blue** (panel) with **inset** for boxes inside a panel and **panel-lit** for the focused row.
- **Bevel light / bevel dark**: the only edges in the system. Light top-left, dark bottom-right, always.
- **Signal yellow** (yellow): titles, the focused row, chart bars, the ticker label. Never a background for text
  blocks except the ticker label.
- **Phosphor paper** (paper) for body text and values; **periwinkle dim** (dim) for secondary text on blue
  (tinted from the panel hue, never grey). **Shadow ink** only for the text drop shadow.

## Typography

One face, Jersey 15 (SIL OFL, `licenses/Jersey15-OFL.txt`), one weight; never faux-bold. Sizes in code are
scaled by 1.18 because the face sets small. Every line of text carries a hard 2 dp character-generator drop
shadow in shadow-ink, except text on the yellow ticker label. Page titles are two stacked lines
("Current / Conditions"); there are no eyebrow labels above titles. Ticker separators use the bullet U+2022,
which the face contains; any glyph must be checked against the font before use.

## Layout

960 x 540 dp, 40 dp side gutters. Header 64 dp high at 24 dp from the top; 16 dp to the panel; panel fills the
rest; 24 dp dusk band; 44 dp ticker; 14 dp sky below it. Inside a panel, groups sit in inset boxes 16 dp apart.
List screens split the panel 500 dp / rest with a 3 dp bevel-dark divider: list left, Zeno plus context right.

## Elevation & Depth

Flat. Depth comes only from bevels (3-4 dp light/dark edges) and the hard text shadow. No blur shadows, no glass.
Faint CRT scanlines (1 dp black at 8% every 3 dp) lie over every screen.

## Shapes

Square corners everywhere. Pixel geometry: arrows, icons, bars and Zeno are built from square blocks; scaled
bitmaps use nearest-neighbour filtering only.

## Components

- **Channel row**: number, name, optional description. Focus = panel-lit bevel, yellow number and name, 2 dp-pixel
  arrow. Up/down moves; OK opens.
- **Broadcast page**: badge (Zeno at 1.5 dp per pixel in a bevelled box), two-line title, clock, panel, ticker.
- **Ticker**: yellow "ZENO" label, then a continuous crawl at 60 dp/s with no gap between loops.
- **Inset box**: inset fill, 3 dp bevel, yellow title, contents.
- **Block bar**: a row of 6 dp blocks with 2 dp gaps, yellow = value, bevel-dark = remaining track, direct label
  on the right. **Block column chart**: same blocks stacked upward, dashed paper line for the average.
- **Zeno sprite**: idle, blink, look left, look right; blinks every 2.5-5 s, one-pixel stepped bob every 0.7 s,
  glances at whatever the remote just moved to.
- **Channel cycle**: hard cuts between pages every 10 s, no transitions.

## Do's and Don'ts

- Do keep one data colour (yellow) on a dark track, with direct labels; single-series charts need no legend.
- Do keep motion stepped and pixel-sized: hops, blinks and hard cuts, not easing curves.
- Don't add rounded corners, blur shadows, gradients on panels, or a second typeface.
- Don't use emoji or font glyphs as icons; draw them on a pixel grid.
- Don't put eyebrow labels above titles; the badge already identifies the station.
- Don't use the Weather Channel name, logo, or any real channel's branding.
