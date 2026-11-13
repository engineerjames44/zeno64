#!/bin/sh
# Switch off everything in app/src/main/assets/debloat-packages.txt on the TV. Safe to run again.
# Usage: tools/debloat.sh <tv-ip:5555>   (or set ZENO_TV=<tv-ip:5555>)
DEV=${1:-$ZENO_TV}
[ -n "$DEV" ] || { echo "Which TV? Pass its address (ip:5555) or set ZENO_TV." >&2; exit 1; }
cd "$(dirname "$0")"
grep -v '^#' ../app/src/main/assets/debloat-packages.txt | grep . | while read -r pkg; do
  printf '%-45s ' "$pkg"
  adb -s "$DEV" shell pm disable-user --user 0 "$pkg" 2>&1 < /dev/null | head -1
done
