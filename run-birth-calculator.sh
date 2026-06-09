#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")"

# Swing/AWT is still X11 based on most Java builds, so Wayland compositors
# normally see this app through XWayland.
export _JAVA_AWT_WM_NONREPARENTING=1
export GDK_BACKEND=x11

mkdir -p out
javac -d out BirthCalculator.java
exec java \
  -cp out \
  -Dsun.awt.X11.XWMClass=BirthCalculator \
  -Dawt.useSystemAAFontSettings=on \
  -Dswing.aatext=true \
  BirthCalculator
