# Spanish Translation Notes

Choices behind the Spanish (`es`) localization in `app/src/main/res/values-es/strings.xml`, for reviewers and for keeping new strings consistent.

## Translation choices worth flagging for review

- **Variant: neutral Spanish (`es`), not `es-rES` or `es-rMX`.** The folder is plain `values-es`, so users in Spain and Latin America both land on it. Vocabulary is chosen to minimize regional friction.
- **Register: informal "tú".** Imperatives use the second-person singular ("Toca", "Visita", "Consulta", "Elige"), not "usted". This matches the German and Arabic files and the convention of most Spanish Android apps.
- **Term: "root"** stays English ("acceso root", "estado de root", "rootear"). This is the standard Spanish tech-press form and matches the other locales.
- **"Ajustes" over "Configuración"** for `action_settings`. "Ajustes" is what Android's own Spanish system UI uses.
- **"vía"** in `root_provider_via` / `root_provider_via_with_version`. "a través de" is too long for a status line.
- **"Comprobar" over "verificar"** for root checks ("Comprobando root…", "Comprobar root"). Slightly less formal and more common in Spanish UI copy.
- **`app_name` kept as "Basic Root Checker"** (Latin script), matching every other locale. It is the marketed product name.
- **"MB"** stays as the unit abbreviation in `update_progress_megabytes`, which is standard in Spanish.
- **License texts are not in this file.** They come from `com.iboalali.ui:licences`, and legal text stays verbatim.

## Preserved verbatim from the English source

Easy to break on a later edit:

- The `<![CDATA[…]]>` wrapper and the `<b>` / `<br>` tags inside `textView_Disclaimer`.
- `%1$s` / `%2$s` in `update_progress_megabytes` (used by the in-app update flow, so the order must not change).
- The `#` glyph in `textView_checkForRoot`, which names the symbol on the FAB.
- Escapes: `\n`, `\"`, `\'`, `&#169;`.
- Brand and proper nouns: "TelemetryDeck", "topjohnwu", "libsu", "Android", "iboalali", and all URLs.
