# Spanish Translation Notes

Reference notes documenting the choices made when adding the Spanish (`es`) localization in `app/src/main/res/values-es/strings.xml`. Useful for future reviewers and for keeping subsequent string additions consistent.

## Translation choices worth flagging for review

- **Variant: neutral Spanish (`es`), not `es-rES` or `es-rMX`.** The locale resource folder is plain `values-es`, so both Spain and Latin American users fall through to it. Vocabulary was chosen to minimize regional friction.

- **Register: informal "tú" form.** Imperatives use the second-person singular ("Toca", "Visita", "Consulta", "Elige") rather than the formal "usted" form. This matches the existing German and Arabic informal style; it is also the convention Google uses for most Spanish Android apps.

- **Term: "root".** Kept as the English/Latin word ("acceso root", "estado de root", "rootear") rather than translating it. This is the standard Spanish tech-press convention and matches how the app already presents the term in the other locales.

- **"Ajustes" over "Configuración"** for `action_settings`. "Ajustes" is the term Android itself uses in Spanish system UI; "Configuración" is also understood but less consistent with the platform.

- **"vía"** is used in `root_provider_via` / `root_provider_via_with_version`. The alternative ("a través de") was rejected as too long for a status line.

- **"Comprobar" over "verificar"** for root checks ("Comprobando root…", "Comprobar root"). Slightly less formal, more common in Spanish UI copy.

- **`app_name` kept as "Basic Root Checker"** (Latin script). All other locales also keep it untranslated; it is the marketed product name.

- **Brand / proper-noun preservation:** "TelemetryDeck", "topjohnwu", "libsu", "Android", "iboalali", and all URLs are kept verbatim.

- **"MB"** is kept as the unit abbreviation in `update_progress_megabytes`, which is standard in Spanish.

- **License texts (Apache 2.0, libsu notice, AndroidDeviceNames notice) were NOT translated** — they remain in English because the source XML marks them `translatable="false"`. This is intentional: legal text must stay verbatim, and translating them would also trigger Lint `ExtraTranslation` errors.

## Things preserved verbatim from the English source

These are not translation choices but are worth noting because they are easy to break in subsequent edits:

- `<![CDATA[…]]>` wrapper and `<b>` / `<br>` HTML tags inside `textView_Disclaimer`.
- `%1$s` / `%2$s` positional placeholders in `update_progress_megabytes` (consumed by the in-app update flow — order must not change).
- The `#` glyph reference in `textView_checkForRoot`, which refers to the on-screen FAB symbol.
- Escape sequences: `\n`, `\"`, `\'`, `&#169;`.
