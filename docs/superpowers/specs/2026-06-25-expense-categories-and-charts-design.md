# DaySurp Design: Expense Categories, Grouped History, and Chart Improvements

## Goal

Improve DaySurp so that expenses are categorized, existing stored expense data is preserved, the history list is grouped by category with expandable sections, chart statistics are more meaningful, and line charts show point values clearly during interaction.

## Scope

This design covers:

- safe migration of stored transactions to support categories
- predefined category catalog, including empty categories
- automatic categorization of existing stored expenses based on description text
- grouped and collapsible expense history on the main screen
- corrected saving ratio formula in charts
- pie chart for expense totals by category
- clearer value display while interacting with line charts

## Current State

- transactions are stored in `DataStore` as JSON
- `Transaction` currently contains `id`, `amount`, `date`, and `description`
- expenses and incomes are mixed in one transaction list
- the main screen shows a flat history list
- the chart screen shows only line charts
- the displayed saving ratio is currently based on a surplus-oriented formula, not the requested `overall expenses / overall gains`

## Data Design

### Transaction model

Extend `Transaction` with:

- `category: String`
- `type: String` only if needed for display logic; otherwise derive from `amount < 0`

To minimize migration risk, keep the stored JSON shape backward compatible:

- old saved records without `category` must still deserialize successfully
- missing `category` values are normalized after read

### Category catalog

Use a predefined category list so the app has stable grouping, filtering, and charting even when some categories are empty.

Initial categories:

- Food
- Transport
- Home
- Bills
- Health
- Shopping
- Leisure
- Travel
- Work
- Education
- Taxes
- Subscriptions
- Gifts
- Pets
- Other

Income entries remain supported but are excluded from expense-category grouping and expense pie chart totals.

## Migration Strategy

### Compatibility rule

Never delete or discard existing transaction records.

### Read-time normalization

When reading stored transactions:

1. deserialize old and new records
2. for each transaction without category:
3. if `amount >= 0`, treat it as income and assign a stable internal category such as `Income`
4. if `amount < 0`, infer the most plausible expense category from `description`
5. persist the normalized list back to `DataStore`

### Categorization heuristic

Use deterministic keyword matching on a normalized lowercase description string.

Examples:

- groceries, supermarket, coop, lidl, conad -> Food
- bus, train, metro, taxi, fuel, parking -> Transport
- rent, furniture, repair, ikea -> Home
- electricity, gas, water, phone, internet -> Bills
- pharmacy, doctor, dentist -> Health
- amazon, clothes, shoes, electronics -> Shopping
- cinema, netflix, game, restaurant, bar -> Leisure or Subscriptions depending on keyword
- hotel, flight, booking -> Travel
- course, book, exam -> Education
- tax, fine, fee -> Taxes

Fallback:

- ambiguous expense descriptions go to `Other`

This is intentionally rule-based rather than ML-based so behavior stays explainable, stable, and offline.

## Main Screen UX

### Add/Edit flow

For manual transaction entry:

- when adding income, category selection can be hidden or fixed to `Income`
- when subtracting expense, show a category picker using the predefined catalog
- keep description optional

### History grouping

Replace the flat history list with grouped sections for expenses:

- each category row shows category name, total spent, and item count
- each category section is expandable/collapsible
- expanded sections show the individual transactions in reverse chronological order

Also keep income items visible in a separate section so the full transaction history remains understandable.

Suggested order:

1. Income
2. expense categories sorted by total descending or alphabetically

Recommended default:

- sort expense categories by total descending for faster insight

### Empty categories

Show the category catalog in selectors even if a category currently has zero transactions.

Do not clutter the history screen with empty categories by default.

## Chart UX

### Saving ratio correction

Display:

- `Expense Ratio = overall expenses / overall gains`

Optionally also show:

- `Savings Ratio = 1 - expense ratio`

If income is zero, show a safe fallback such as `0.00%` or `N/A` to avoid divide-by-zero confusion.

### Pie chart

Add an expense pie chart on the chart screen:

- one slice per expense category with non-zero total
- label or legend for category names
- percentage contribution per category
- total expense amount available nearby

Useful extra stats:

- top expense category
- top category share percentage
- number of active expense categories in the selected period

### Line chart interaction

Keep the line chart for surplus, expenses, and income.

Improve interaction with:

- visible marker or tooltip on highlight
- formatted X value as date
- formatted Y value as amount in euros
- circles enabled for touch targets if needed

Persistent labels for every point are not recommended because they will overcrowd the chart; the preferred behavior is to show values on point selection/highlight.

## Filtering and Time Range

The existing `daysToDisplay` filter remains the primary time window.

Both grouped history and category statistics must use the same active time window so the main screen and chart screen remain coherent.

## Error Handling and Safety

- if migration fails for one transaction, preserve the record and assign `Other`
- never clear `DataStore` during migration
- keep categorization deterministic so future app launches do not reshuffle categories unpredictably

## Testing Strategy

Focus on targeted regression coverage:

- migration test for old JSON records without `category`
- categorization test for representative descriptions
- chart-stat test verifying `expenses / income`
- grouping test for categorized history transformation

## Implementation Notes

Likely touched files:

- `app/src/main/java/com/example/startapp/data/model/Transaction.kt`
- `app/src/main/java/com/example/startapp/data/CounterDataRepository.kt`
- `app/src/main/java/com/example/startapp/ui/viewmodel/CounterViewModel.kt`
- `app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt`
- `app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt`
- `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`
- `app/src/main/java/com/example/startapp/domain/model/ChartData.kt`

## Recommendation

Implement the feature using:

- predefined categories
- read-time migration with automatic rule-based categorization
- grouped collapsible history
- pie chart by category
- corrected ratio and better line-chart markers

This balances safety, clarity, and usability while preserving all existing stored data.
