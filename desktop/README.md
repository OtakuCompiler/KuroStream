# KuroStream Desktop

Native desktop application built with **Jetpack Compose Multiplatform**.

Produces installers for **Windows** (.exe / .msi), **macOS** (.dmg), and **Linux** (.deb / .AppImage).

## Build

```bash
bash gradlew :desktop:packageExe           # Windows installer
bash gradlew :desktop:packageDmg           # macOS
bash gradlew :desktop:packageDeb           # Linux .deb
bash gradlew :desktop:packageAppImage      # Linux .AppImage
```

Output: `desktop/build/compose/binaries/main/{exe,dmg,deb,appimage}/`

## Development

```bash
bash gradlew :desktop:run                  # Run from source
```

## Architecture

Shares `:domain`, `:data`, `:playback`, `:extensions`, `:ui` modules with the Android app.
Only the UI shell (`desktop/src/main/kotlin/.../ui/`) is platform-specific.

The desktop UI mirrors the Arctic Fuse 3 layout used on Android TV but adapts
it for mouse/keyboard + windowed mode:
- Left sidebar with navigation icons (Home, Movies, Series, Settings)
- Main content area with hero carousel + content rows
- Dark theme (#121212) with crimson accent (#E94560)
