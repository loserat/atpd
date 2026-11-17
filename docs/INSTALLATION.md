# Installation, updates and rollback

[Documentation](README.md)

## Build

Use Python 3, `curl`, JDK 21, Android SDK platform 34, Build Tools 36.1.0 and Platform Tools. Set `JAVA_HOME`, `ANDROID_HOME` or `ANDROID_SDK_ROOT` when installations are outside the default locations.

```sh
python3 app/fetch_deps.py
python3 tests/check_palette.py
python3 app/build.py
```

`fetch_deps.py` downloads LibVLC 3.7.7 from Maven Central and verifies the pinned SHA256. The build packages `armeabi-v7a` and produces `dist/TV-Dashboard-0.14.0.apk`; generated dependencies and APKs are ignored by Git.

## Signing and retained data

The first build creates an ignored local test signing key. Retain that key for updates to that installation. A different signature cannot update an existing installation. For an existing own key, configure `NICKOS_KEYSTORE` and `NICKOS_SIGN_PASSWORD`; the expected alias remains `leisetv`. Provide the password locally rather than putting it in repository files.

The Android package stays `de.leisetv`. Updating in place preserves the application sandbox, signing identity and camera/Hue configuration keys. Do not uninstall merely to bypass a signature mismatch when existing data must be retained.

Version 0.11.3 fixes Android-12 HTTPS authentication failures caused by missing Keystore permissions for Conscrypt's raw RSA callbacks. On Android 12 / API 31 and later it uses a new local HTTPS identity; an existing installation on those versions needs its new certificate confirmed in the browser. Older Android installations keep their existing HTTPS identity. TLS remains enabled, and camera/Hue encryption keys are unaffected. Android's [Keystore documentation](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.Builder#setEncryptionPaddings(java.lang.String...)) explains the padding authorizations required by RSA TLS server keys.

An older APK alone is not a portable backup of camera/Hue settings. Those files are encrypted with Android Keystore keys tied to the application/device. Copying encrypted files to another device does not recreate those keys.

## Install an authorized device

Select the actual device explicitly, especially when an emulator is also running:

```sh
adb devices
adb -s DEVICE install -r dist/TV-Dashboard-0.14.0.apk
adb -s DEVICE shell am start -n de.leisetv/.MainActivity
```

Verify the installed version, local HTTPS login, camera entries and Hue room before continuing. Current validation also compares the installed APK hash with the local candidate and checks that screensaver activation is unchanged.

## Home assignment

Keep the existing launcher installed. Record its Home component before making a permanent assignment:

```sh
adb -s DEVICE shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
adb -s DEVICE shell cmd package set-home-activity --user 0 de.leisetv/.MainActivity
```

The command depends on Android/manufacturer support. To return, assign the previously recorded component through the supported system selection/command. Do not guess a stock launcher component for another model.

On the tested TPM191E / Android 12, setting the Home role reported success, but the Google launcher's higher activity priority still won. After explicit owner approval, the Google launcher package was disabled for user 0 using Android's supported user-level package setting. It remains installed with its data; the Google Home interface is unavailable while disabled. An individual-activity change was rejected by Android. This is device-specific and is not an automatic installation step: recovery from a dashboard problem may require ADB.

For that verified installation, the original package state was `default`. Its recorded recovery is:

```sh
adb -s DEVICE shell pm default-state --user 0 com.google.android.tvlauncher
adb -s DEVICE shell cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity
adb -s DEVICE shell input keyevent KEYCODE_HOME
```

Those identifiers and original state are specific to the tested TV; verify another TV independently. Home assignment passed seven technical checks, including a Home event from Android settings and the rendered dashboard, followed by the owner's physical-remote confirmation. Reboot/standby persistence and executing the recovery sequence remain separate checks.

## Shared project baseline and per-TV settings

All TVs use the same application source. Since 0.11.7 the reference setup is the primary TV's presentation, with other locations configured independently through local web settings. There is no separate second-TV build or automatic copying of its settings.

| Setting | Fresh-install baseline | Web section for customization |
| --- | --- | --- |
| Theme / layout | Nordlicht / featured upper row | Design |
| Visible destination order | Ambilight, Cameras, Hue, Television, Devices, System | Design → Design & Anordnung |
| Camera, clock and date visibility | On | Startseite |
| Hue / UniFi functionality | On; connections must be configured separately | Startseite → Funktionen an diesem TV |
| App favorites | Hidden; ARD/ZDF retained as initial choices, up to four selected apps | Design → Profile → Apps für Mediathek |
| Language / clock and text fonts | German / default system fonts | Design → Sprache & Schrift |

For the second-TV arrangement, choose the installed apps under **Design → Profile → Apps für Mediathek**, save, then apply **Mediathek**. It hides cameras, Hue and UniFi and places the chosen apps after Ambilight. **Standard** restores the shared presentation and hides favorites without discarding their selection. Names remain independent; both presets reset their documented appearance/language/key defaults without pairing providers. [Custom profiles and scope](PROFILES.md).

An update retains existing per-TV settings, including old tile orders and a second TV's disabled integrations or enabled media tiles. Only missing preferences use the baseline. **Design → Reset** changes the appearance draft only; it does not reset integration flags, app visibility or provider connections. Existing camera-first orders are preserved until explicitly rearranged or reset and saved.

For future project changes, use the primary-TV baseline as the reference and keep location-specific additions optional in web settings. Verify retained primary-TV configuration and the optional paths separately before describing a version as synchronized. A GitHub push alone is not an installed TV update.

Personal camera URLs, SSIDs, bridge/controller addresses, application keys and display names are not part of the shared defaults. An enabled integration indicates dashboard availability, not a preconfigured or verified connection.

## Configure locally

Open **System → Web-Einstellungen** on the TV. The page shows its HTTPS address and generated access PIN. Port 8766 serves HTTPS; 8765 redirects. Confirm the locally generated certificate in your browser on first use. Standby availability is not guaranteed.

Under **Startseite → Funktionen an diesem TV**, disable Hue and/or UniFi when absent at that location, then choose **Funktionen speichern**. Existing installations default to enabled. This changes the dashboard's per-device presentation and provider eligibility, not the lights/router themselves; retained connections and shortcuts return when enabled again. Passive TV connectivity and Ambilight remain independent.

Under **Design → Profile → Apps für Mediathek**, select up to four installed TV apps and **App-Auswahl speichern**. With Standard active, choices stay hidden until Mediathek is applied. With Mediathek active, saving updates the visible tiles immediately. Use **App-Liste aktualisieren** after installing/removing an app. A retained missing app is labelled unavailable and can be deselected; it is never installed automatically. Rearrange positional favorites under **Design & Anordnung**. Old six/eight/ten-item orders retain their relative order and gain the remaining reserved positions, for fourteen in total. [Profiles and types](PROFILES.md).

Add camera RTSPS links in the web page. Existing links are retained when their fields are left empty. Removing a dashboard camera does not delete the Protect camera or its recordings.

Pair Hue from **Hue → Raum & Bridge**, press the physical Bridge button, and explicitly select the room. The key stays on the TV. HDMI sources appear under **Geräte** when Android exposes them; choosing a card delegates the actual source to the TV's app.

## Rollback

Preserve a prior same-key APK and the previous Home component. Android may reject an older version-code APK; do not work around that by deleting retained data. A same-key rebuild of a known-good source revision with an appropriately higher version code is a safer application rollback path. Recheck local settings, cameras, Hue room, Home assignment and screensaver activation afterwards.

Device input/display settings and camera-provider recordings are not migrated by this dashboard update.

## Language, fonts and design

Open the local web **Design** tab. **Design & Anordnung** contains themes and tile order. **Sprache & Schrift** stores one shared German/English language and independent clock/text font choices. Default, condensed, serif and monospace use Android system fonts; no font download is needed. Web browsers use corresponding local fallback families, so exact glyphs can differ. Save each section separately; selecting a draft only changes its preview.

Reload an already-open web page after updating the APK to receive the new Design tab and translation catalog. Other open browser sessions receive the shared language on reload; the saving session changes immediately. This changes the dashboards, not Android/Philips or third-party app languages.

Reload any already-open browser page after updating. On phones/tablets, swipe the sticky section strip sideways to reach all eight sections (Hue is hidden when disabled); expandable guidance retains the setup instructions.

Since 0.11.2, use the separate **Shortcuts** tab to choose each remote colour key. The default reset changes only the preview/draft; save explicitly to apply it. Existing assignments are retained when updating. Reload an existing browser page to receive the eighth tab.

## Current profile indicator (0.13.0)

Reload an already-open IP dashboard after the update. The band above the navigation shows **Am TV eingestellt**, its selected profile name, **Typ: Standard/Mediathek**, this page's TV address and confirmation time. Later differences retain the chosen name and add **Angepasst**. Choosing another entry in the saved-profile editor alone does not apply it; use its **Übernehmen** action. The same preset is marked in Design. A visible authenticated page reads profile settings every five seconds; returning/focusing rechecks immediately. Hidden or signed-out pages stop polling.

If a read fails, exceeds its eight-second deadline or becomes overdue, the last profile is shown as **Zuletzt bestätigt / Stand unbestätigt**, without a positive active-card/connection claim. Recovery is automatic. This reads saved dashboard preferences; it does not prove that Home is currently visible or that a provider is online. Draft fields are independent and are not overwritten by those reads. [Profile behavior](PROFILES.md).

## Direct Home favorites (0.14.0)

After updating, reload the browser and open **Favoriten**. Enable a named HDMI tile, explicitly select its port and choose up to three camera records, then save. Saving does not open a source. Move the reserved favorite slots in Design; hidden cameras also hide their direct favorites. Old orders and source libraries are retained, with new direct tiles opt-in. [Workflow and API](FAVORITES.md).

This release is built/tested locally only. The owner deferred physical-TV installation; neither TV was updated for 0.14.0. Reconnect and inspect the exact target before a later same-signature update. Public builds and private retained-key installation variants are separate; private signing material is never published.
