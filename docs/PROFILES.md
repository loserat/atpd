# Dashboard profiles

[Documentation](README.md) · [Installation](INSTALLATION.md) · [Current status](STATUS.md)

Open **Design → Profile** in the local IP dashboard. Profiles are stored on this TV; no account or external service is required. Applying one preset changes the saved dashboard presentation immediately. Existing installations keep their setup when updated; no profile is applied automatically.

![Profiles in the local settings](images/web-profiles.png)

The image uses synthetic preview data, including the custom profile name. [Phone view](images/web-profiles-mobile.png).

## Built-in profiles

| Profile | Home | Integrations | Appearance |
| --- | --- | --- | --- |
| Standard | Ambilight / Cameras / Hue above Television / Devices / System | Hue and UniFi enabled; ARD/ZDF off | Nordlicht, featured rows, clock/date on, German and default fonts, default color keys |
| Mediathek | Ambilight / ARD / ZDF above Television / Devices / System | Cameras hidden, Hue/UniFi disabled | Same neutral design, clock/date, language/fonts and color-key defaults |

Choose the card to apply it. These two presets remain available and cannot be overwritten, renamed or deleted. Enabling a media tile does not install its provider app or sign in. A missing TV app retains the existing clear unavailable-app behavior. Enabling Hue/UniFi does not configure or pair anything.

The Mediathek preset reproduces the second TV's arrangement and optional features. It deliberately uses Nordlicht rather than copying a location's private name or latest personal theme. All affected settings remain editable afterwards.

## Create and adjust your own profile

1. Apply a preset or start with your current setup.
2. Adjust and **save** the desired Home visibility, integration/app switches, Design/order, language/fonts and color keys in their existing sections.
3. Return to **Design → Profile**, choose **Neues Profil**, enter a name and use **Aktuellen Stand speichern**.
4. Your profile now has its own **Übernehmen** action. **Verwalten** selects it in the editor.

Up to **eight** custom profiles can coexist. Names have 1–40 characters; empty/control-character, duplicate and reserved preset names are rejected. **Namen speichern** renames without replacing settings. To update a saved snapshot, choose it, save your new dashboard settings first, then use **Aktuellen Stand speichern** and confirm replacement. **Profil löschen** removes only the saved snapshot; the current TV setup is unchanged.

A profile captures the **persisted** configuration, not unsaved form drafts. When applying would discard affected layout/visibility/language/shortcut drafts, the dashboard asks for inline confirmation. Unsaved TV-name and camera-stream drafts are retained. Conflicting form saves and duplicate profile actions are blocked during a request; errors retain the last acknowledged UI and release the controls.

The correspondence label is computed from the saved settings. It shows the first matching profile, with built-in presets taking precedence; otherwise it says **Individuell angepasst**. It is not a separate active-profile mode that silently rewrites later edits. Another browser's changes are read when reopening Design or reloading.

## What a profile contains

| Included | Retained separately |
| --- | --- |
| Camera/clock/date visibility | Home heading, dashboard/TV name and sales model |
| Theme, layout and eight-position order | Camera library, stream URLs, default camera and certificates |
| Hue/UniFi feature switches | Bridge/controller addresses, pairing, room/site and encrypted keys |
| ARD/ZDF tile switches | Provider app installation, sign-in and playback |
| Shared language and clock/text fonts | Android system language, Home assignment and installed apps |
| Four color-key assignments | Screensaver settings/activation and Hue standby automation |

Applying a profile sends no light, HDMI, pairing or provider command. The retained Hue standby option becomes eligible again if Hue is re-enabled and the normal later standby conditions occur; profiles do not change that option or guarantee standby delivery. Already-dispatched operations from elsewhere cannot be recalled.

## Storage and API

`dashboard_profiles_v1` is an application-private preference containing at most eight custom `{id,name,config}` snapshots. It contains no provider credentials. Built-ins are generated from allowlisted values, not persisted mutable templates. The strict configuration includes only `display`, `appearance`, `integrations`, `appShortcuts`, `interface` and `remote`; legacy six-item orders normalize without changing relative positions.

| Authenticated route | Complete input / result |
| --- | --- |
| GET `/api/profiles` | Built-ins/custom list, current allowlisted configuration, matching `activeId` and limit |
| POST `/api/profiles/save` | `{name}` to create; `{id,name}` to replace from current saved settings |
| POST `/api/profiles/rename` | `{id,name}`; settings unchanged |
| POST `/api/profiles/delete` | `{id}`; custom entries only |
| POST `/api/profiles/apply` | `{id}`; validated values applied in one preference commit and revision advance |

Client requests cannot upload arbitrary configuration, packages, URLs or provider fields. All routes retain existing session/origin/host guards. Profile operations are serialized with each other. Another authenticated client's later setting write can still supersede the applied snapshot. Custom profiles are local to one installation; export/import, cross-TV sync and automatic GitHub updates are separate work. The normal same-signature update retains their store; uninstalling app data does not.

## Validation

Run `python3 tests/check_profiles.py` and `node tests/check_web.cjs`. The profile suite compiles production validation/persistence classes with in-memory Android/snapshot adapters and checks presets, atomic validation/application, preserved unrelated keys, legacy normalization, CRUD and limits. The web suite covers independent drafts, safe names, confirmation/cancellation, application, update/rename/delete, pending/failure paths and localization. These are separate from the browser and physical-TV evidence in [Current status](STATUS.md).
