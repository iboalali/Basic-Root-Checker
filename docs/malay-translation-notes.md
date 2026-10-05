# Malay Translation Notes

Choices behind the Malay (`ms`) localization in `app/src/main/res/values-ms/strings.xml`, one of Singapore's four official languages (issue #43).

**Not yet reviewed by a native speaker.** It needs a pass before release.

## Translation choices worth flagging for review

- **Malaysian standard spelling**, which Singapore also uses in official text. The one visible Malaysia/Indonesia split here is *aplikasi* vs *apl*. The full word is used throughout, which reads correctly in both.
- **Term: "akses root"**, with "root" in Latin script and the verb written **"me-root"** with a hyphen. Hyphenating a Malay prefix onto a foreign root is the standard spelling rule, and it keeps the English word searchable.
- **Register: `anda`.** The neutral second person of Malay software, right for Singapore and Malaysia alike. `kau` and `engkau` are far too familiar, `tuan` far too formal.
- **`tip_jar_*` → "tip".** Borrowed directly, which is what Malay does with this concept. There is no idiomatic native word for tipping a developer.
- **`support_card_dismiss` → "Ketepikan"**, the word Android itself uses for *dismiss*. "Tutup" (close) is already `content_description_close`, and the two actions differ.
- **`root_provider_other` → "Lain-lain".** The reduplicated form is how Malay writes "other" as a catch-all list entry. The bare "Lain" reads as an adjective waiting for a noun.
- **`app_name` kept as "Basic Root Checker"** (Latin script), matching every other locale.

## Preserved verbatim from the English source

- The `<![CDATA[…]]>` wrapper and the `<b>` / `<br>` tags inside `textView_Disclaimer`.
- `%1$s` / `%2$s` in `update_progress_megabytes` and `root_provider_via_with_version`.
- The `#` glyph in `textView_checkForRoot`, and its `\"` escaping.
- `&#169;` and the `\n` escapes in `about_part1`.
- Brand and proper nouns: "TelemetryDeck", "topjohnwu", "libsu", "Android", "iboalali", "Kitsune Mask", "MB", "ROOT" in the About disclaimer, and all URLs.
