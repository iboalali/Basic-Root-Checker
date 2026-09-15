# Chinese Translation Notes

Reference notes for the Simplified Chinese localization in
`app/src/main/res/values-b+zh+Hans/strings.xml`. Added in 2.6 as part of covering Singapore's four
official languages (issue #43).

**Not yet reviewed by a native speaker.** Worth a pass before the release goes out; Chinese UI copy
is easy to get subtly wrong in register.

## Why the folder is `values-b+zh+Hans`

The BCP-47 form names the **script**, not a country. It matches `zh-Hans-CN`, `zh-Hans-SG` and
`zh-Hans-MY` — Singapore included, which is the reason it exists — and deliberately does **not**
match `zh-TW` or `zh-HK`, who read Traditional characters and would be served the wrong script by a
plain `values-zh`.

The `b+` qualifier is understood from API 21, so it is safe at this app's minSdk 24. The locale
config entry and `AppLanguage.SUPPORTED_TAGS` both spell it `zh-Hans`, and the platform's per-app
locale API round-trips that tag exactly, which is what keeps the picker's radio button in sync.

## Translation choices worth flagging for review

- **"Root" stays in Latin script** throughout ("Root 权限", "检测 Root"). This is what Chinese
  Android communities write; 超级用户权限 is the formal translation but reads like documentation, and
  users search for the English word.

- **Register: `你`, not `您`.** The informal second person, matching the German, Arabic and Dutch
  files. `您` would be the polite form used by banks and carriers, which is the wrong tone here.

- **`tip_jar_*` → 打赏.** The established term for tipping a creator in Chinese apps. 小费 is a
  restaurant gratuity and would read as out of place in software.

- **Spacing around Latin runs.** A space sits on both sides of embedded Latin or numeric text
  ("Root 权限", "TelemetryDeck 帮助", "Android 版本"). This is the standard Chinese typographic rule
  and its absence looks cramped.

- **Full-width punctuation** throughout: `，` `。` `？` `！` `：` and the corner quotes `“ ”` in
  `textView_checkForRoot`. Because the quotes are full-width, that string needs no `\"` escaping,
  unlike the English source.

- **`app_name` kept as "Basic Root Checker"** (Latin script), matching every other locale.

## Preserved verbatim from the English source

- The `<![CDATA[…]]>` wrapper and the `<b>` / `<br>` tags inside `textView_Disclaimer`.
- `%1$s` / `%2$s` in `update_progress_megabytes` and `root_provider_via_with_version`.
- The `#` glyph in `textView_checkForRoot`, which names the symbol on the FAB.
- `&#169;` and the `\n` escapes in `about_part1`.
- Brand and proper nouns: "TelemetryDeck", "topjohnwu", "libsu", "Android", "iboalali", "Kitsune
  Mask", "MB", and all URLs.
