#!/system/bin/sh
# SPDX-License-Identifier: GPL-3.0-only
# no-bg.sh — Block selected (and optionally all third-party) apps from background
# execution. Apps still open/work normally on demand; only background/boot activity
# is blocked. Persistent across reboots (appops stored in /data/system).
#
# Run from NATIVE Termux (rooted):
#   su -c "sh /sdcard/kurostream/no-bg.sh"

set -u

# Apps explicitly blocked from background execution (still work in foreground).
TARGETS="
com.whatsapp
com.Dominos
"

# Block ALL third-party (non-system) apps from background too? 1 = yes, 0 = no.
# Essentials below are always excluded so the device keeps working.
ALL_THIRD_PARTY=1

# Essentials that MUST keep background access (never touched).
EXCLUDE="
com.termux
app.lawnchair.play
com.google.android.inputmethod.latin
net.dinglisch.android.taskerm
"

deny() {
  pkg="$1"
  echo "=> $pkg"
  appops set "$pkg" RUN_IN_BACKGROUND deny
  appops set "$pkg" RUN_ANY_IN_BACKGROUND deny 2>/dev/null
}

for p in $TARGETS; do
  deny "$p"
done

if [ "$ALL_THIRD_PARTY" = "1" ]; then
  echo ">> All third-party apps (excluding essentials)..."
  if [ -r /data/system/packages.list ]; then
    grep -E '/data/app' /data/system/packages.list | awk '{print $1}' | while read -r p; do
      skip=0
      for e in $EXCLUDE; do
        if [ "$p" = "$e" ] || [ "${p#$e.}" != "$p" ]; then skip=1; break; fi
      done
      [ "$skip" = "1" ] && continue
      deny "$p"
    done
  else
    echo "  (could not read /data/system/packages.list; skipping third-party sweep)"
  fi
fi

echo "Done. Revert one app: su -c \"appops set PKG RUN_IN_BACKGROUND allow\""
