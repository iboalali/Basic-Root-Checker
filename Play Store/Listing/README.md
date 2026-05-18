# Play Store Listing

Source-of-truth copies of the Play Store listing text for each locale. Edit here, then paste into the Play Console under **Grow → Store presence → Main store listing**.

## Layout

```
Listing/
├── default/    ← English (en-US in the Play Console)
├── german/     ← de-DE
├── arabic/     ← ar
├── russian/    ← ru-RU
└── spanish/    ← es-ES (used by both es-ES and es-419 unless split later)
```

Each locale folder contains three files:

| File | Play Console field | Character limit |
|---|---|---|
| `app_name.txt` | App name | 30 |
| `short_description.txt` | Short description | 80 |
| `full_description.txt` | Full description | 4000 |

The locale folder names match the convention used by `../Release Notes/` and `../Screenshots/` (language names, not BCP-47 codes).

## Notes

- The app's display name (the launcher label) lives in `app/src/main/res/values*/strings.xml` as `app_name` and is kept as the Latin "Basic Root Checker" in every locale. The Play Console `app_name` field is the *store* name and may differ if ever needed, but currently matches.
- "Full description" supports plain text only — no HTML, no Markdown. Bullet glyphs (`•`) are literal.
- When trimming for length, prioritize the first ~80 characters of the full description: the Play Store shows that snippet on search result cards.
