# Social announcements

One folder per released version, holding the announcement post for each channel. The copy is drawn
from that version's `CHANGELOG.md` section, reduced to the few things a stranger would care about.

```
Social/<version>/
  mastodon.txt     @iboalali@mastodon.social
  bluesky.txt      @iboalali.bsky.social
```

## Post it after production, not after the cut

The link points at the production listing, so a post published while the version is still on beta
sends people to the previous release. Wait until the rollout reaches production, then post.

## Character limits differ, and so does the counting

| Channel | Limit | Counting |
| --- | --- | --- |
| Mastodon | 500 | **Every URL counts as 23 characters** regardless of its real length |
| Bluesky | 300 | Graphemes, and a URL counts its **full** length |

The Play link is 74 characters, which is a quarter of the Bluesky budget on its own. Keep the raw
text under the limit anyway rather than relying on Mastodon's URL rule, so the same file can be
pasted into a client that counts differently.

Check before posting:

```bash
python3 - <<'PY'
import pathlib, re
for fn, lim in (('mastodon.txt', 500), ('bluesky.txt', 300)):
    t = (pathlib.Path('Social/2.5')/fn).read_text(encoding='utf-8').strip('\n')
    print(f'{fn}: {len(t)} / {lim}')
PY
```

## One claim to get right

Say **"collects no personal information"**, never "no tracking" or "no data". The app does collect
optional anonymous usage data, on by default with an opt-out in Settings. The About screen's own
wording was corrected in 2.5 for exactly this overstatement, so the announcement must not
reintroduce it.

## What earns a line

Whatever a user would notice, ranked by how many people it affects. For 2.5 that put the Magisk
version string first: it was wrong for every Magisk user on every rooted device, which is a wider
audience than any of the new features. Refactors, module moves and internal fixes belong in the
changelog, not here, however much work they were.
