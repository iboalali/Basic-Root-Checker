# In-app products

The tip jar sells three tiers, each as two one-time products. One folder per tier holds the Play Console copy (`listing.md`) and the store icon for both of its ids:

    small/    → tip_small, tip_small_repeat
    medium/   → tip_medium, tip_medium_repeat
    large/    → tip_large, tip_large_repeat
    pricing/  → every product's per-region price, mirrored down from Play, plus the purchasing-power plan

The record id (`tip_small`) is acknowledged and kept, so owning it records that the user has tipped. The repeat id (`tip_small_repeat`) is consumed after every purchase, so the tier can be tipped again. The app switches from one to the other once the record is owned, so both ids of a tier must carry the same title, description, icon and price. `TipTier` in `billing/TipProduct.kt` is the source of truth for the ids.

There are no subscriptions.

## Copy

Each `listing.md` has the title and description for all nine store locales. The title is the app's own tier label (`R.string.tip_tier_*`), so the purchase sheet names the tier the user just tapped. The description says a tip unlocks nothing; keep it that way unless a tip ever starts to.

The Console needs the copy pasted into both ids of a tier.

## Icons: generated, not hand-drawn

    ./scripts/generate-iap-icons.sh

Writes `icon.png` (512×512, full-bleed, RGB with no alpha) and `icon-rounded.png` (same art, rounded corners, transparent outside) into each tier folder. The glyph is the white heart `favorite_24px`, the mark on the main screen's support card, on the launcher purple `#673AB7`. All three tiers share it. Never hand-edit the PNGs: change the glyph or color in the script and re-run, then re-upload in the Console, which does not pick up a changed file on its own.

## Pricing

    ./scripts/store iap-pricing                          # mirror live prices into pricing/
    ./scripts/store iap-pricing --check                  # exit non-zero if the repo has drifted from Play
    ./scripts/store iap-ppp --refresh-classification     # re-pull World Bank income bands + FX
    ./scripts/store iap-ppp                              # rebuild the purchasing-power plan
    ./scripts/store iap-ppp --check                      # exit non-zero if the plan is stale

| File | What it is | Edit by hand? |
|---|---|---|
| `prices.md`, `tip_*.json` | What Play charges today, and the rollback if a change goes wrong | No, re-run `iap-pricing` |
| `ppp-policy.json` | Tier factors, products in scope, the USD 0.49 floor | **Yes, the only one** |
| `ppp-classification.json` | World Bank income band and GNI per capita per country, plus USD exchange rates | No, `--refresh-classification` |
| `ppp-reference-prices.json` | The price each in-scope region is cut from | Rarely, see its `_README` |
| `ppp-plan.md`, `proposed/*.json` | What we want Play to charge, ready to patch | No, re-run `iap-ppp` |

Low-income regions pay 20% of the Play-generated price and lower-middle-income regions 55%, rounded to .49/.99 (or a round number in currencies Play quotes in whole units). The floor is USD 0.49, so the small tip goes under one dollar or euro in the deepest tier. `ppp-plan.md` lists every region with its old and new price and the commands that apply it.

The World Bank reissues its income bands every 1 July. Re-run `--refresh-classification` before each repricing and read the diff. The full workflow and its traps are in the `play-store-assets` skill.
