# Adding a Locale

Every place a language has to be named before it actually works. The list is longer than it looks,
and most of the entries fail *silently* — a missed one costs you an untranslated screen, not a build
error.

Worth promoting to the shared kit: steps 1-7 are the same in all three apps.

## In this repo

1. **`app/src/main/res/values-<qualifier>/strings.xml`** — the 84 translatable strings. Copy the key
   set from `values/`, minus everything marked `translatable="false"`. Lint's `MissingTranslation`
   catches a short file, so run `./gradlew :app:lintGplayDebug` before believing it is complete.
2. **`app/build.gradle.kts`** — add the **language** code to `androidResources.localeFilters`. Not
   the full BCP-47 tag: the filter works on language, so `zh` keeps `values-b+zh+Hans`. A locale
   missing here is stripped from the APK and nothing warns you.
3. **`app/src/main/res/xml/app_locales_config.xml`** — the full tag (`zh-Hans`, not `zh`). This is
   what puts the language in Android's own per-app language screen.
4. **`AppLanguage.SUPPORTED_TAGS`** — the same full tag, and it must match the locale config string
   **exactly**. `LanguagePickerDialog` marks the selected row with `currentTag == tag`, comparing
   what `LocaleManager` hands back against this list, so a spelling difference leaves the picker with
   nothing selected while the language still applies.
5. **`AppLanguageTest`** — add the autonym. `displayName` derives it from the JDK, so the test is how
   you find out what it actually returns rather than guessing.
6. **`PreviewLocales`** — one more `@Preview`. IDE previews only; these do not touch the screenshot
   test references, which live in `src/screenshotTest`.
7. **`docs/<language>-translation-notes.md`**, linked from the CLAUDE.md documentation map.

## Outside this repo

8. **Android-Shared**, if the language should reach library-owned UI. Three modules ship strings:
   `app-catalog/ui` (the Other apps row actions), `nav3/overlay` (the large-screen dialog's
   accessibility labels) and `ui/licences` (the Licenses screen). 11 strings. That repo has to
   publish a version before this one can pin it, and the app falls back to English until it does.
   **Do not add the locale to `previews/matrix`** as part of this — it drives every consumer's
   reference screenshots, so it belongs with the store-screenshot work instead.
9. **`apps.<locale>.json` on iboalali.com.** The Other apps card's descriptions and highlights come
   from the catalog feed, not from either repo. An uncovered locale gets the English feed.
10. **Play Store**, tracked as its own issues, the way the Russian rollout was: `Play Store/store.json`
    (`locales` and `localeLabels`), a `Play Store/Listing/<language>/` folder with the three field
    files, `Play Store/Release Notes/<version>/<language>.txt`, and the store screenshots.

## Picking the qualifier

Use a script subtag when the language is written in more than one script, and a plain language code
otherwise. `values-b+zh+Hans` serves Simplified readers in China, Singapore and Malaysia while
leaving Traditional readers in Taiwan and Hong Kong on English, which is correct — they would
otherwise be served text in the wrong script. The `b+` form needs API 21, so it is safe here.
