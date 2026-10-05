# Russian Translation Notes

Choices behind the Russian (`ru`) localization in `app/src/main/res/values-ru/strings.xml`, for reviewers and for keeping new strings consistent.

## Translation choices worth flagging for review

- **Term: "root-доступ"** (Latin "root" + Cyrillic suffix) throughout. This is the standard Russian tech-press form and matches the other locales. "права суперпользователя" was rejected as overly literal.
- **Register: formal / impersonal.** "Ваше устройство", "Нажмите", "Выберите", not the informal "ты" form the German and Arabic files use. The goal is a more professional tone.
- **The disclaimer is impersonal** ("Это приложение НЕ предоставит…") rather than addressing the user directly, which reads more professional in Russian than a literal translation.
- **`toast_content_copied` → "Скопировано".** Short and idiomatic, rather than a literal "content copied".
- **`app_name` kept as "Basic Root Checker"** (Latin script), matching every other locale. It is the marketed product name.
- **"МБ"** is the standard Russian abbreviation for megabytes, used in `update_progress_megabytes`.
- **License texts are not in this file.** They come from `com.iboalali.ui:licences`, and legal text stays verbatim.

## Preserved verbatim from the English source

Easy to break on a later edit:

- The `<![CDATA[…]]>` wrapper and the `<b>` / `<br>` tags inside `textView_Disclaimer`.
- `%1$s` / `%2$s` in `update_progress_megabytes` (used by the in-app update flow, so the order must not change).
- The `#` glyph in `textView_checkForRoot`, which names the symbol on the FAB.
- Escapes: `\n`, `\"`, `\'`, `&#169;`.
- Brand and proper nouns: "TelemetryDeck", "topjohnwu", "libsu", "Android", "iboalali", and all URLs.
