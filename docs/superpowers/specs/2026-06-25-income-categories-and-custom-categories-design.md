# DaySurp Income Categories And Custom Categories Design

## Goal

Extend DaySurp so that incomes are categorized and grouped just like expenses, support user-defined categories for both income and expense flows, add an income pie chart, show monthly averages under both pie charts, and clean up the repository so it behaves like a proper GitHub project.

## Problem Summary

The current app still has three structural limitations:

- expenses are better categorized now, but incomes are still effectively flat
- users cannot create their own categories when the built-in taxonomy is not enough
- charts only visualize expense categories, while income composition remains hidden

There is also a data-model mismatch in the product:

- the user-configured daily surplus is a real income concept in the app
- but it is not represented as its own income category in chart composition

Finally, the repository should be maintained like a GitHub project with the expected hygiene files and concise documentation.

## Scope

This design covers:

- income categories
- collapsible grouped income history
- custom category creation through `+ New Category`
- pie chart for incomes
- monthly averages under expense and income pie charts
- repository hygiene files such as `README.md`, `.gitignore`, and essential repo structure cleanup

This design does not add:

- nested categories
- category deletion or rename UI
- drag and drop reclassification
- cloud sync

## Income Taxonomy

The app shall support a built-in income taxonomy distinct from the expense taxonomy.

### Built-In Income Categories

The default income catalog shall include:

- `Daily Surplus`
- `Salary`
- `Family`
- `Refund`
- `Sales`
- `Bonus`
- `Other Income`

These categories are the built-in baseline only. Users may extend them with custom categories.

## Expense Taxonomy

The existing cleaned expense taxonomy remains active:

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

Users may extend it with custom categories.

## Custom Categories

### UX Behavior

Both the expense dropdown and the income dropdown shall include a final entry:

- `+ New Category`

When selected:

- a lightweight creation flow opens inline or as a small dialog
- the user types the category name
- the name is normalized and validated
- the category is saved in the correct catalog (`expense` or `income`)
- the newly created category becomes the selected category immediately

### Validation Rules

Custom category creation shall enforce:

- trimmed names
- case-insensitive duplicate prevention
- whitespace normalization
- rejection of empty names
- prevention of duplicates against both built-in and existing custom categories within the same catalog

Examples:

- `Salary` and ` salary ` are the same
- `bonus` and `Bonus` are the same

Expense and income catalogs are separate, so the same label may exist in both domains if intentionally created.

### Persistence

Custom categories shall be stored persistently so that they survive app restarts and are reused in:

- the entry dropdowns
- grouped history
- pie charts
- chart legends and summary rows

Custom income categories and custom expense categories shall be stored separately.

## Income Categorization Rules

### Manual Income Entries

Manual positive transactions shall support explicit income category selection from:

- built-in income categories
- user-created income categories

If an old income transaction has no category, migration shall infer one from its description when possible, otherwise assign `Other Income`.

Expected examples:

- `salary`, `stipendio` -> `Salary`
- `genitori`, `family`, parental support style descriptions -> `Family`
- `refund`, `rimborso`, `rimb assicurazione` -> `Refund`
- `vendita`, `sold`, `ram venduta` -> `Sales`
- `bonus` -> `Bonus`
- unknown positive descriptions -> `Other Income`

### Daily Surplus As Synthetic Income

The daily surplus configured by the user is part of the product's income logic and shall appear in income charts as a synthetic category:

- category label: `Daily Surplus`
- included only in chart/statistical aggregation, not as a fake manual transaction row
- computed coherently over the currently selected visible period

For a selected period with `N` effective days:

- `Daily Surplus Total = configured daily surplus * N`

This synthetic contribution shall be included in:

- income totals shown in the chart section
- the income pie chart
- the per-category income legend/stat rows

It shall not create editable or deletable transaction records in history.

## Grouped History

### Expense History

Expense groups remain grouped and collapsible by category.

### Income History

Income history shall be converted from a flat list into grouped collapsible sections like expenses.

Each income category group shall display:

- category name
- transaction count
- total amount in the period
- collapsible nested transaction rows

The collapse interaction shall match the expense grouping behavior visually and functionally.

### Daily Surplus In History

`Daily Surplus` remains chart-only synthetic income. It shall not appear as repeated synthetic rows in the transaction history unless the app later introduces explicit surplus posting as real transactions.

## Pie Charts

The chart screen shall contain:

- one pie chart for expenses by category
- one pie chart for incomes by category

Both charts shall:

- include custom categories
- use the currently selected visible period
- show full readable legends/stat rows below the chart
- remain consistent with the grouped history taxonomy

### Expense Pie Chart Rows

Each expense category row below the chart shall show:

- category name
- total amount in the selected period
- percentage share
- average monthly amount

### Income Pie Chart Rows

Each income category row below the chart shall show:

- category name
- total amount in the selected period
- percentage share
- average monthly amount

`Daily Surplus` shall appear as one of those income rows.

## Monthly Average Definition

Monthly average must be comparable across arbitrary visible ranges.

For any category total over the selected period:

- compute the covered period length in days
- convert to monthly average using a normalized month length

Recommended definition:

- `monthlyAverage = total / max(coveredDays, 1) * 30.4375`

This avoids misleading raw division by calendar month count when the visible period is not an exact number of months.

The same formula shall be used for both income and expense categories.

## Data Model Changes

The current single-category enum-centric approach should be evolved carefully:

- keep built-in expense taxonomy
- add built-in income taxonomy
- add persisted custom income category list
- add persisted custom expense category list

The app should expose a unified category option list to the UI per domain:

- `expense built-in + expense custom + + New Category`
- `income built-in + income custom + + New Category`

Existing transactions keep their stored category strings. The system should not require every category to be a compile-time enum member once custom categories are supported.

## Migration Rules

Migration must preserve:

- transaction amount
- transaction date
- transaction description

Migration may update only:

- transaction category when legacy or blank
- stored custom-category metadata when newly introduced

For positive legacy transactions:

- infer a built-in income category when possible
- otherwise assign `Other Income`

For negative legacy transactions:

- continue using the cleaned expense taxonomy and legacy-collapsing rules already defined

## UI Flow Changes

### Entry Section

The add/subtract area shall support:

- expense category dropdown with built-in + custom expense categories + `+ New Category`
- income category dropdown with built-in + custom income categories + `+ New Category`

The UI may either show separate dropdowns depending on action or one dynamic dropdown that changes with the chosen action, but the action-specific category list must always be correct.

### Grouped Lists

Both expense and income sections shall be visually grouped and collapsible.

### Chart Section

The chart screen shall show:

- `Expense Ratio (Expenses/Gains)`
- `Saving Ratio (Saved/Gains)`
- expense pie chart
- income pie chart
- per-category rows with total and monthly average

## Repository Hygiene

The project should behave like a clean GitHub repository.

### Required Files

Ensure these files are present and meaningful:

- `README.md`
- `.gitignore`
- `.env.template` only if the project actually uses environment variables

### README Requirements

`README.md` shall be concise but complete and include:

- what DaySurp does
- basic setup instructions
- how to run/build the app
- short note about stored local data behavior
- short feature summary including category charts and grouped history

### Gitignore Requirements

`.gitignore` shall exclude standard Android/Gradle/local IDE artifacts, including examples like:

- `.gradle/`
- `build/`
- local IDE caches
- generated dumps or temporary inspection files
- `.kotlin/`

It shall not exclude source files or intentional project assets.

## Testing Strategy

Add or update focused tests for:

- built-in income category catalog
- custom category normalization and duplicate rejection
- income migration for legacy positive transactions
- grouped income history behavior
- income pie chart aggregation including `Daily Surplus`
- monthly average calculation
- correct separation of income and expense custom catalogs
- continued correct expense taxonomy behavior after introducing custom categories

Tests should cover:

- `+ New Category` persistence logic
- `Daily Surplus` synthetic income contribution
- chart rows showing total and monthly average

## Risks And Mitigations

### Risk: Category model becomes too enum-bound for custom categories

Mitigation:

- use enum-backed built-in catalogs, but allow transaction categories and UI options to remain string-compatible

### Risk: Synthetic daily surplus distorts income interpretation

Mitigation:

- label it explicitly as `Daily Surplus`
- include it only in charts/stats, not fake history rows

### Risk: Monthly average is misleading on short periods

Mitigation:

- normalize by covered days rather than raw month count
- use the same formula consistently for expenses and incomes

### Risk: User-created categories create clutter

Mitigation:

- normalize names
- block duplicates
- separate income and expense custom catalogs

## Success Criteria

This work is successful when:

- incomes are grouped and collapsible by category
- income dropdown supports built-in categories and `+ New Category`
- expense dropdown also supports `+ New Category`
- custom categories persist and appear everywhere relevant
- income pie chart exists and includes `Daily Surplus`
- pie chart rows for both domains show total and monthly average
- legacy positive transactions are categorized safely
- repository contains clean GitHub-style hygiene files and concise documentation
