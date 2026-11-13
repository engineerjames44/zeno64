#!/bin/sh
# Put the TV back exactly as Amazon shipped it (as of 2026-11-12), leaving the Zeno 64 app installed.
# To remove the app too: adb -s <device> uninstall dev.jamescronin.zeno64
# Usage: tools/restore-amazon.sh <tv-ip:5555>   (or set ZENO_TV=<tv-ip:5555>)
DEV=${1:-$ZENO_TV}
[ -n "$DEV" ] || { echo "Which TV? Pass its address (ip:5555) or set ZENO_TV." >&2; exit 1; }
A() { adb -s "$DEV" shell "$@" < /dev/null; }
cd "$(dirname "$0")"
echo "home app:      $(A cmd package set-home-activity com.amazon.tv.launcher/.ui.HomeActivity_vNext)"
A settings delete secure enabled_accessibility_services >/dev/null
A settings put secure accessibility_enabled 0
echo "home watcher:  off"
A settings put secure screensaver_components com.amazon.ftv.screensaver/.app.services.ScreensaverService
echo "screensaver:   Amazon's"
A settings delete global alarm_manager_constants >/dev/null
echo "launch delay:  back to normal"
./undo-debloat.sh "$DEV"
