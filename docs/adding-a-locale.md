# Adding a Locale

Every place a language has to be named before it works. Most of these fail *silently*: a missed one costs an untranslated screen, not a build error.

Steps 1-7 are the same in all three apps, so this list is a candidate for the shared kit.

## In this repo

1. **`app/src/main/res/values-<qualifier>/strings.xml`**: the 84 translatable strings. Copy the key set from `values/`, minus everything marked `translatable="false"`. Lint's `MissingTranslation` catches a short file, so run `./gradlew :app:lintGplayDebug` before trusting it is complete.
2. **`app/build.gradle.kts`**: add the **language** code to `androidResources.localeFilters`. Not the full BCP-47 tag: the filter works on language, so `zh` keeps `values-b+zh+Hans`. A locale missing here is stripped from the APK with no warning.
3. **`app/src/main/res/xml/app_locales_config.xml`**: the full tag (`zh-Hans`, not `zh`). This puts the language in Android's own per-app language screen.
4. **`AppLanguage.SUPPORTED_TAGS`**: the same full tag, matching the locale config string **exactly**. `LanguagePickerDialog` marks the selected row with `currentTag == tag`, comparing what `LocaleManager` returns against this list. A spelling difference leaves the picker with nothing selected while the language still applies.
5. **`AppLanguageTest`**: add the autonym. `displayName` derives it from the JDK, so the test shows what it actually returns.
6. **`PreviewLocales`**: one more `@Preview`. IDE previews only; the screenshot test references live in `src/screenshotTest` and are not affected.
7. **`docs/<language>-translation-notes.md`**, linked from the CLAUDE.md documentation map.

## Outside this repo

8. **Android-Shared**, if the language should reach library-owned UI. Three modules ship strings: `app-catalog/ui` (the Other apps row actions), `nav3/overlay` (the large-screen dialog's accessibility labels) and `ui/licences` (the Licenses screen), 11 strings in total. That repo has to publish a version before this one can pin it, and the app falls back to English until it does. **Do not add the locale to `previews/matrix`** as part of this. It drives every consumer's reference screenshots, so it belongs with the store-screenshot work.
9. **`apps.<locale>.json` on iboalali.com.** The Other apps card's descriptions and highlights come from the catalog feed, not from either repo. An uncovered locale gets the English feed.
10. **Play Store**, tracked as separate issues: `Play Store/store.json` (`locales` and `localeLabels`), a `Play Store/Listing/<language>/` folder with the three field files, `Play Store/Release Notes/<version>/<language>.txt`, and the store screenshots.

## Picking the qualifier

Use a script subtag when the language is written in more than one script, and a plain language code otherwise. `values-b+zh+Hans` serves Simplified readers in China, Singapore and Malaysia and leaves Traditional readers in Taiwan and Hong Kong on English. That is correct, because otherwise they would get text in the wrong script. The `b+` form needs API 21, so it is safe here.
