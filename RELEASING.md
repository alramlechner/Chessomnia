# Releasing

How a Chessomnia release is cut and published to Google Play.

The upload itself is automated (step 6); everything before it is not. The steps
are few enough that a script would mostly hide them, and the verification in
step 4 is the part that actually matters — it is what catches a release that
builds but is wrong.

---

## Prerequisites

| | |
|---|---|
| JDK | **17.** Newer JDKs crash the Kotlin compiler on this project. |
| Android SDK | `ANDROID_HOME` set, build-tools 35 available |
| Signing | an untracked `android-app/keystore.properties` — see `keystore.properties.example` |
| Publishing | an untracked `android-app/play-service-account.json` — see `play-service-account.json.example`. Only needed for step 6; every other step works without it. |

The upload key is **not** in this repository and must not be. Losing it does not
lose the app (Play App Signing holds the real signing key), but it does mean
asking Google to reset the upload key before the next release.

---

## 1. Version

`version.properties` at the repository root is the single source. Both numbers
move together:

```properties
VERSION_CODE=13
VERSION_NAME=1.1.3
```

`VERSION_CODE` must be **strictly greater** than anything Play has already seen.
Gaps are fine; going backwards is not, and it cannot be undone — Play remembers
a version code even for a release that was discarded.

Give every build that leaves this machine its own version, including test
builds. Two artifacts carrying the same version is how you end up debugging a
change that was never installed.

## 2. Regenerate what is generated

Only if the corresponding source changed:

```bash
python3 tools/generate_app_icon.py       # icon or mark
python3 tools/generate_wordmark.py …     # wordmark (see tools/README.md)
python3 tools/render_store_assets.py     # always, after either of the above
rsvg-convert store/play-icon-512.svg -w 512  -h 512 -o store/play-icon-512.png
rsvg-convert store/play-feature.svg   -w 1024 -h 500 -o store/play-feature-1024x500.png
```

## 3. Build

```bash
cd android-app
./gradlew test                    # the full unit suite
./gradlew test -DperftDeep=1      # 12.4 M nodes; slow, but the rule engine's real proof
./gradlew bundleRelease
```

The artifact is `app/build/outputs/bundle/release/app-release.aab`.

⚠️ Play wants the **bundle**, not an APK. `assembleRelease` produces an APK for
sideloading onto a test device; it is not what gets uploaded.

## 4. Verify the artifact

Check the thing that will be uploaded, not the thing that was compiled.
"BUILD SUCCESSFUL" is not evidence.

```bash
AAB=app/build/outputs/bundle/release/app-release.aab

# Signed with the upload key, not a debug key
jarsigner -verify -verbose:summary -certs "$AAB" | grep "Signed by"

# Both locales really shipped (a German string must be physically present)
unzip -p "$AAB" base/resources.pb | grep -ac "Springer"

# No networking pulled back in
unzip -p "$AAB" base/dex/classes.dex | grep -ac "okhttp\|okio"    # must be 0
```

And on the merged manifest, which is where a dependency can quietly reintroduce
a permission:

```bash
grep -oE '<uses-permission[^>]*>' \
  app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml
```

The only line should be `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which
AndroidX declares for the app about itself. In particular there must be no
`INTERNET`.

> `android.permission.DUMP` appears elsewhere in that manifest as an
> `android:permission=` **attribute** on AndroidX's `ProfileInstallReceiver`.
> That restricts who may address the receiver; it is not a permission this app
> requests. Do not "fix" it.

For a build that goes onto a real device first, `assembleRelease` and:

```bash
$ANDROID_HOME/build-tools/35.0.0/aapt2 dump badging app/build/outputs/apk/release/app-release.apk \
  | grep -E "^package|targetSdk"
```

## 5. Commit and tag

```bash
git commit -am "Version 1.1.3 (13)"
git tag -a v1.1.3 -m "Version 1.1.3"
git push && git push --tags
```

## 6. Upload

### Automated

Once `android-app/play-service-account.json` exists (see
`play-service-account.json.example` for how to create it):

```bash
cd android-app
./gradlew publishBundle                     # internal track
./gradlew publishBundle --track production  # explicit; never the default
```

The defaults are deliberately harmless. A bare `publishBundle` goes to the
**internal** track — a named list of at most 100 testers, not the store. Without
the key file every ordinary task still works and only the `publish*` tasks fail,
with a message saying so.

⚠️ The Developer API **cannot create an app's first release.** Google requires
one bundle to have been uploaded through the Console by hand for that package
name before the API accepts anything. Until then, use the manual route below.

### Manual

In the Play Console: **Test and release → Production** (or a testing track) →
*Create new release* → upload the `.aab` → release notes → roll out.

Release notes come from `CHANGELOG.md`, shortened. Play allows 500 characters
per language, and both `en-US` and `de-DE` need their own text.

The store listing itself lives in `store/listing-en.md` and `store/listing-de.md`
— including the Data Safety answers and the categorisation. Keep those files in
step with what is actually entered in the Console, or the next release will be
edited from a stale source.

---

## First release only

These happen once and are not part of a routine release:

1. **Identity verification** on the developer account.
2. **Create the app** in the Console: name, default language, "app" vs "game",
   free.
3. **Closed testing with 12 testers over 14 consecutive days** — required for
   new personal developer accounts before production is unlocked. This is the
   slowest step by a wide margin, and it depends on other people rather than on
   code. Start it early.
4. **Play App Signing**: accept it on the first upload. Google then holds the
   app signing key and the key in `keystore.properties` becomes the *upload*
   key. The two are different, and only the upload key ever lives here.
5. **Screenshots**: phone, 7" tablet and 10" tablet. They must show the real
   app, so they have to be taken on a device — see the list at the end of
   `store/listing-en.md`.
