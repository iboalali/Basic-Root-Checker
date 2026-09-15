# Dutch Translation Notes

Reference notes for the Dutch (`nl`) localization in `app/src/main/res/values-nl/strings.xml`. Added
in 2.6 alongside Chinese, Malay and Tamil (issues #42 and #43).

**Not yet reviewed by a native speaker.** The strings are short, standard UI copy, but nothing here
has been read by a Dutch speaker. Worth a pass before the release goes out.

## Translation choices worth flagging for review

- **Register: informal `je` / `jij`,** matching the German and Arabic files rather than the formal
  Russian one. Dutch consumer apps overwhelmingly use `je`, and `u` would read stiff for a hobbyist
  root tool.

- **Term: "root-toegang"** for root access, and the bare verb **"rooten"** for the act. Both are the
  normal Dutch tech-press forms, and the English word is what users search for. "Beheerdersrechten"
  was rejected as describing something else (Windows admin rights).

- **"rootmanager"** written closed, not "root manager". Dutch compounds close up; the open form is a
  common anglicism error.

- **`tip_jar_*` → "fooi".** The literal Dutch word for a gratuity. "Tip" in Dutch primarily means
  *hint*, so using it here would read as "give a hint".

- **`support_card_dismiss` → "Sluiten"** (close), not "Negeren" (ignore). The card closes; nothing
  is being ignored.

- **`about_not_affiliated`** is phrased as one clause with two verbs ("wordt niet ondersteund door en
  is niet gelieerd aan"), because Dutch cannot stack the two English prepositions onto one verb the
  way "endorsed by or affiliated with" does.

- **`app_name` kept as "Basic Root Checker"** (Latin script), matching every other locale. It is the
  marketed product name.

## Preserved verbatim from the English source

Easy to break on a later edit:

- The `<![CDATA[…]]>` wrapper and the `<b>` / `<br>` tags inside `textView_Disclaimer`.
- `%1$s` / `%2$s` in `update_progress_megabytes` and `root_provider_via_with_version` — order must
  not change.
- The `#` glyph in `textView_checkForRoot`, which names the symbol on the FAB.
- Escapes: `\n`, `\"`, `&#169;`.
- Brand and proper nouns: "TelemetryDeck", "topjohnwu", "libsu", "Android", "iboalali", "Kitsune
  Mask", and all URLs.
