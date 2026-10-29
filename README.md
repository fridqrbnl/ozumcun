# App Blocker

A small no-root Android firewall: tick an app, and that app loses internet access.
Made for blocking ads in games that also work offline.

## How it works
Without root, Android only lets an app control other apps' network access through
the VPN API. This app creates a *local* VPN that contains **only the apps you tick**.
Their packets go into the VPN and are dropped, so they have no internet. All other
apps bypass the VPN and work normally. Nothing is sent to any server.

## Build & install
1. Install Android Studio: https://developer.android.com/studio
2. File > Open > select this `AppBlocker` folder. Let Gradle sync (it downloads what it needs).
3. On the S24: Settings > About phone > Software information > tap "Build number" 7 times.
   Then Settings > Developer options > turn on "USB debugging".
4. Connect the phone by USB, pick it in the device list, and press Run.
   Or: Build > Build APK(s), copy `app/build/outputs/apk/debug/app-debug.apk` to the phone and open it.

## Use
1. Open App Blocker, turn on "Blocking enabled", and accept the VPN prompt.
2. Tap the game(s) you want to block. Changes apply immediately.
3. A key icon shows in the status bar while blocking is active.

### Keep it on after a reboot (recommended)
Settings > Connections > More connection settings > VPN > gear next to App Blocker >
turn on "Always-on VPN". Do NOT turn on "Block connections without VPN": that cuts
the internet for every app.

## Limitations
- Only one VPN can run at a time; starting another VPN app turns this off.
- Games that need an online check will not start while blocked.
- Some games show ads they cached earlier; clearing the game's cache once can help.
