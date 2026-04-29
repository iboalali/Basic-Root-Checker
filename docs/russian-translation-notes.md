# Russian Translation Notes

Reference notes documenting the choices made when adding the Russian (`ru`) localization in `app/src/main/res/values-ru/strings.xml`. Useful for future reviewers and for keeping subsequent string additions consistent.

## Translation choices worth flagging for review

- **Term: "root-доступ"** (Latin "root" + Cyrillic suffix) is used throughout. This is the standard Russian tech-press convention and matches how the app already presents the term in other locales. The alternative ("права суперпользователя") was rejected as inconsistent and overly literal.

- **Register: formal / impersonal.** Russian wording uses **"Ваше устройство"**, **"Нажмите"**, **"Выберите"** rather than the informal "ты" form (which the existing German and Arabic translations use). This was an explicit choice for a more professional tone.

- **Disclaimer phrasing is impersonal** ("Это приложение НЕ предоставит…") rather than addressing the user directly, which reads more professional in Russian than a literal translation of the English source.

- **`toast_content_copied` → "Скопировано".** This fixes a bug-by-omission in the existing `values-de` and `values-ar` files, where this string was left as the literal English "Content Copied". Russian translates it properly; consider back-porting fixes to the de/ar files.

- **`app_name` kept as "Basic Root Checker"** (Latin script). Both the German and Arabic files keep it in Latin; it is the marketed product name.

- **Brand / proper-noun preservation:** "TelemetryDeck", "topjohnwu", "libsu", "Android", "iboalali", and all URLs are kept verbatim.

- **"МБ"** is the standard Russian abbreviation for megabytes, used in `update_progress_megabytes`.

- **License texts (Apache 2.0, libsu notice, AndroidDeviceNames notice) were NOT translated** — they remain in English because the source XML marks them `translatable="false"`. This is intentional: legal text must stay verbatim, and translating them would also trigger Lint `ExtraTranslation` errors.

## Things preserved verbatim from the English source

These are not translation choices but are worth noting because they are easy to break in subsequent edits:

- `<![CDATA[…]]>` wrapper and `<b>` / `<br>` HTML tags inside `textView_Disclaimer`.
- `%1$s` / `%2$s` positional placeholders in `update_progress_megabytes` (consumed by the in-app update flow — order must not change).
- The `#` glyph reference in `textView_checkForRoot`, which refers to the on-screen FAB symbol.
- Escape sequences: `\n`, `\"`, `\'`, `&#169;`.
