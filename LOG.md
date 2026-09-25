## 2026-09-24

- Setup Maven, added Duck DB JDBC dependency
- Loaded sales_train_evaluation.csv into DuckDB, unpivoted wide -> long for one item store pair, printed 30 days of (day, units)
- Next: add Spring Boot + Spring AI 2.0, expose one MCP tool get_sales(item, store, from, to) wrapping this query
- fixed issues with Maven build
- Wired to llm and ran the app to print response from llm

## 2026-09-25

- Expose MCP tool: get_sales