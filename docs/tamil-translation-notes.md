# Tamil Translation Notes

Choices behind the Tamil (`ta`) localization in `app/src/main/res/values-ta/strings.xml`, one of Singapore's four official languages (issue #43).

**Not yet reviewed by a native speaker, and this is the locale that most needs one.** Tamil UI copy has a wide register range, its technical vocabulary is unsettled, and these strings run longer than any other locale's, which is also a layout risk.

## Translation choices worth flagging for review

- **Transliteration over translation for the technical core.** "ரூட்" (root), "ஆப்ஸ்" (app), "டிப்" (tip) and "ID" are borrowings. Tamil has native coinages for each, but Android users read the borrowed forms, and a purist "root" would leave people unsure what the app checks.
- **Register: `நீங்கள்` (polite plural).** The neutral register for Tamil software, and the one Android itself uses. The singular `நீ` would be rude to a stranger.
- **Short label-style imperatives for buttons and content descriptions** ("நகலெடு", "மூடு", "புதுப்பி"), and the longer `-வும்` forms ("தட்டவும்", "பார்க்கவும்") for full instructing sentences. The split follows string length and role on purpose. Mixing them within one control would read oddly.
- **`settings_theme_title` → "தீம்"** as a borrowing. The native "கருப்பொருள்" means *theme* as in subject or motif, not a color scheme.
- **Length.** Several strings are much longer than the English, in particular `settings_reset_identity_dialog_message` and `root_hidden_manager_hint`. Tamil is agglutinative, so this is expected. Check the Settings rows and the support card for truncation on a small screen.
- **`app_name` kept as "Basic Root Checker"** (Latin script), matching every other locale.

## Preserved verbatim from the English source

- The `<![CDATA[…]]>` wrapper and the `<b>` / `<br>` tags inside `textView_Disclaimer`.
- `%1$s` / `%2$s` in `update_progress_megabytes` and `root_provider_via_with_version`. In `root_provider_via` the placeholder leads the string, because Tamil puts "வழியாக" (*via*) after the noun it governs.
- The `#` glyph in `textView_checkForRoot`, and its `\"` escaping.
- `&#169;` and the `\n` escapes in `about_part1`.
- Brand and proper nouns: "TelemetryDeck", "topjohnwu", "libsu", "Android", "iboalali", "Kitsune Mask", "MB", "ROOT" in the About disclaimer, and all URLs.
