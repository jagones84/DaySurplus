# DaySurp Category Taxonomy Cleanup Design

## Goal

Replace the current mixed expense taxonomy with a cleaner macro-category model that matches the user's real data and mental model, aggressively drains transactions out of `Other`, and reclassifies stored legacy transactions without changing their amount, date, or description.

## Problem Summary

The current categorization logic still produces conceptually wrong buckets even when the app technically classifies many records:

- `Home` is being used for generic purchases and should not exist as a separate category.
- `Work` is actually being used for digital tools, AI services, software, and online services.
- `Subscriptions` is not meaningful as a standalone category and should be absorbed into `Digital`.
- `Other` still contains too many descriptions that are inferable from real user data.
- Several real descriptions are still mapped to the wrong category:
  - `cena`, `prosecco` should be `Food`
  - `noleggio snow` should be `Leisure`
  - `antenne wireless` should be `Shopping`

The issue is not only missing keywords. The actual taxonomy needs simplification and stronger prioritised rules informed by the stored historical descriptions.

## Final Taxonomy

The final expense category catalog shall be:

- `Food`
- `Transport`
- `Health`
- `Shopping`
- `Gaming`
- `Leisure`
- `Travel`
- `Digital`
- `Education`
- `Taxes`
- `Gifts`
- `Pets`
- `Other`
- `Income`

## Category Collapses

The following legacy categories shall be removed from the active expense taxonomy and collapsed into the new model:

- `Home -> Shopping`
- `Work -> Digital`
- `Subscriptions -> Digital`

These collapses apply to:

- the enum/category catalog shown in the UI
- automatic category inference for new expenses
- migration of previously stored transactions
- grouping in history and charts

## Classification Principles

The classifier shall follow these principles:

1. Prefer a correct macro-category over keeping a legacy category name.
2. Prefer a best-fit concrete category over falling back to `Other`.
3. Use `Other` only for descriptions that are blank, meaningless, or truly not inferable.
4. Prioritise real recurring descriptions extracted from the user's stored data before generic keywords.
5. Keep classification deterministic so the same description always lands in the same category.

## Expected Real-World Classification

The following examples define expected behavior:

- `isber`, `clinica`, `medica`, `ospedale`, `farmacia`, `omega3 e vitamina d3`, `siringhe` -> `Health`
- `temu`, `amazon`, `qmazon lampada puccola`, `antenne wireless`, `caricacell magnetico`, `cavo usb hdmi` -> `Shopping`
- `cena`, `prosecco`, `deliveroo`, `hamburger`, `kebab`, `poke`, `piadina`, and meal-or-drink-only entries -> `Food`
- `serata`, `uscita`, `21 marzo`, `1 maggio`, `sabato 14 marzo`, `capodanno`, `noleggio snow`, `bowling`, `hard rock` -> `Leisure`
- `gita`, `viaggio`, `cervinia`, `bergamo` -> `Travel`
- `taxi`, `area c`, `parcheggio aeroporto` -> `Transport`
- `gaming`, `steam`, `god of war`, `switch`, `gdr` -> `Gaming`
- `gemini`, `trae`, `openrouter`, `google cloud`, `google ai`, `telegram`, `patreon`, `apple tv`, `social app`, `comfyui`, `voxta`, `vam model` -> `Digital`
- `regalo`, `regalo natale cena ufficio` -> `Gifts`

When the description contains both a digital signal and a legacy subscription-style signal, the result shall still be `Digital`.

## Detection Strategy

The classification logic shall be rebuilt in layers:

### 1. Description Normalization

Normalize each description before matching:

- lowercase
- accent removal
- punctuation collapse
- repeated whitespace collapse
- tolerant handling of small typos and mixed formatting

### 2. Prioritized Explicit Overrides

Create a curated priority-ordered rule list based on recurring real descriptions from stored data.

These rules take precedence over generic keyword matching because they capture the user's real naming habits, including:

- short vendor names
- date-like labels that imply leisure events
- typos such as `qmazon`
- specific health providers such as `isber`

### 3. Pattern-Based Heuristics

Add a small number of clear semantic heuristics for repeated patterns that are not single keywords:

- date-like social/event labels such as `21 marzo` or `sabato 14 marzo` -> `Leisure`
- digital service/tool patterns -> `Digital`
- food/drink/social spending terms that imply meals or drinks -> `Food` unless they clearly express an event bucket already covered by `Leisure`

### 4. Generic Category Keywords

After explicit overrides and pattern heuristics, fall back to the general keyword lists for each final macro-category.

### 5. Last Resort Fallback

Only then assign `Other`.

## Data Migration Rules

Stored transactions must be preserved. Migration shall:

- keep `amount` unchanged
- keep `date` unchanged
- keep `description` unchanged
- update only `category`

The migration shall reclassify:

- blank or null legacy categories
- legacy `Other` entries
- legacy `Home` entries into `Shopping`
- legacy `Work` entries into `Digital`
- legacy `Subscriptions` entries into `Digital`

Migration shall run through the same normalized inference path used for new entries so the app does not drift between old and new data.

## UI Impact

### Expense Entry

The expense category dropdown shall expose only the final active taxonomy and must not show removed categories (`Home`, `Work`, `Subscriptions`).

### History Grouping

Grouped expense history shall reflect only the final taxonomy. After migration there should no longer be visible legacy groups named `Home`, `Work`, or `Subscriptions`.

### Charts

Pie chart slices and textual legend shall also reflect only the final taxonomy. This specifically improves readability because generic shopping purchases no longer split between `Home` and `Shopping`, and digital spending no longer splits between `Work` and `Subscriptions`.

## Testing Strategy

Add or update focused tests for:

- taxonomy catalog contents
- collapse behavior of removed categories
- explicit real-world overrides
- migration from `Other`
- migration from `Home`, `Work`, and `Subscriptions`
- representative examples for `Food`, `Shopping`, `Leisure`, and `Digital`
- confirmation that `Other` is used only for genuinely unknown descriptions

Tests should include exact examples drawn from the user's real stored descriptions so regressions are caught immediately.

## Non-Goals

This cleanup does not introduce:

- subcategories
- ML or fuzzy-search libraries
- user-editable category rules
- manual bulk recategorization UI

The goal is a strong deterministic macro-category system with excellent real-world defaults.

## Risks And Mitigations

### Risk: Over-aggressive mapping pushes ambiguous records into the wrong bucket

Mitigation:

- keep rules explicit and priority-ordered
- use stored real examples as contract tests
- reserve `Other` for truly unclear entries rather than deleting it

### Risk: Legacy category names remain visible after migration

Mitigation:

- ensure normalization rewrites old stored categories
- ensure UI dropdown and grouping consume only the final taxonomy

### Risk: Food versus Leisure ambiguity

Mitigation:

- treat meal/drink-only descriptions as `Food`
- treat event/date/social outing descriptions as `Leisure`
- let explicit overrides win where the user's naming pattern is already known

## Success Criteria

This work is successful when all of the following are true:

- `Home`, `Work`, and `Subscriptions` no longer appear as active expense categories
- `Digital` exists and captures software, AI, online services, and subscription-like digital spend
- known real descriptions such as `cena`, `prosecco`, `noleggio snow`, and `antenne wireless` land in the requested categories
- historical stored data is reclassified safely without losing values
- `Other` is reduced to a small residual bucket of genuinely unclear records
- history grouping and charts reflect only the cleaned taxonomy
