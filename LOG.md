## 2026-09-24

- Setup Maven, added Duck DB JDBC dependency
- Loaded sales_train_evaluation.csv into DuckDB, unpivoted wide -> long for one item store pair, printed 30 days of (day, units)
- Next: add Spring Boot + Spring AI 2.0, expose one MCP tool get_sales(item, store, from, to) wrapping this query
- fixed issues with Maven build
- Wired to llm and ran the app to print response from llm

## 2026-09-25

- Expose MCP tool: get_sales

## 2026-09-26

- Use ChatClient and make tool call to get an answer

## 2026-09-27

- Built 5 anomaly cases (stockout, no-change control, store-wide dip, event effect, price hike) with injected/checked ground truth
- Added get_sales_price tool + querySalePrice (originally week-2 scope, pulled forward for the price-hike case)
- Wrote EvalRunner: prompts LLM with baked-in case data (no live tool calls), scores by label substring match
- First run 3/5 - 2 misses were eval-design bugs (picked confounded windows: real Easter event, a real pre-existing zero-run), not model bugs. Fixed by finding event-free/zero-free windows via query.
- Second run 4/5 - remaining miss (store-wide dip) looks like a genuine model limitation: a uniform scale-down is indistinguishable from normal fluctuation without a comparison baseline. Matches week-3's planned compare_to_category/store tool - good early signal that tool is necessary, not just nice-to-have.
- Next: week2 starts with understanding how to get price history, get calendar events