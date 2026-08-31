# Privacy Policy

**Chessomnia does not collect, store or transmit any personal data.**

Last updated: 31 August 2026

## What the app collects

Nothing.

There is no account, no registration, no analytics, no advertising, no crash reporting and
no tracking of any kind. The app contains no advertising SDK, no analytics SDK and no
Google Play Services.

## What the app stores

Two things, both in the app's own private storage on your device:

- your settings (clock on/off, learning aids, screen behaviour), and
- the game currently in progress, so it survives an app restart.

Both are removed when you uninstall the app. Neither leaves the device.

If you have Android's own backup enabled, these settings may be included in your personal
device backup — that is a function of your Android account, not of Chessomnia, and the data
never reaches the developer.

## Network access

The app holds **no Android permission that grants it any capability** — in particular no
`INTERNET` permission. It is therefore technically incapable of sending anything anywhere.

To be exact rather than merely reassuring: the manifest contains one `uses-permission`
entry, `name.lechners.chessomnia.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. It is declared
by the AndroidX libraries for the app about itself, so that the app can register an
internal receiver as not-exported. It grants access to nothing and is not visible to you as
a permission request. You can verify all of this in the manifest, which is part of the
published source code.

The *Source code on GitHub* button in Settings hands a web address to your browser. From
that point your browser is talking to GitHub, under GitHub's privacy policy, exactly as if
you had typed the address yourself. Chessomnia makes no request of its own and learns
nothing about the visit.

## Bug reports

The *Report a problem* button assembles a plain-text description of the current game:
the position, the moves played, the moves legal in that position, the app version and the
device model (for example "Samsung SM-X200, Android 15"). It contains no personal data.

The report is handed to Android's standard share sheet. **You** choose where it goes — an
email app, a messenger, a notes app, or nowhere. The app itself sends nothing and has no
means of doing so. You see the full text before you share it.

## Children

The app is suitable for all ages. Because it collects no data whatsoever, it collects no
data from children either.

## Changes

Any change to this policy will be published in this file in the project's public
repository, together with the date above.

## Contact

Questions about this policy: open an issue at
<https://github.com/alramlechner/Chessomnia/issues>.
