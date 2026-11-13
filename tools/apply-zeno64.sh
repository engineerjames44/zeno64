#!/bin/sh
# Make the TV Zeno 64's: home takeover, screensaver, no launch delay, debloat.
# Install the app first (./gradlew assembleDebug, then adb install -r ...). Undo: restore-amazon.sh
# Usage: tools/apply-zeno64.sh <tv-ip:5555>   (or set ZENO_TV=<tv-ip:5555>)
DEV=${1:-$ZENO_TV}
[ -n "$DEV" ] || { echo "Which TV? Pass its address (ip:5555) or set ZENO_TV." >&2; exit 1; }
A() { adb -s "$DEV" shell "$@" < /dev/null; }
cd "$(dirname "$0")"
echo "home app:      $(A cmd package set-home-activity dev.jamescronin.zeno64/.MainActivity)"
A settings put secure enabled_accessibility_services dev.jamescronin.zeno64/dev.jamescronin.zeno64.HomeWatcher
A settings put secure accessibility_enabled 1
echo "home watcher:  on"
A settings put secure screensaver_components dev.jamescronin.zeno64/.ZenoDream
echo "screensaver:   Zeno's channel"
A settings put global alarm_manager_constants min_futurity=0
echo "launch delay:  off"
./debloat.sh "$DEV"
