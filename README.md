# demand-investigation-agent
Investigate & analyse the trends in the change of demand of a product

Uses the M5 dataset from Kaggle: https://www.kaggle.com/competitions/m5-forecasting-accuracy/data

Built in Java 21, Spring Boot, Spring AI, MCP. The deterministic tools compute the signals(price changes, zero sales runs, category trends), 
the LLM plans the investigation and weighs the evidence when causes overlap or the question is open-ended 

It is evaluated against injected, ground-truth anomalies and benchmarked against a deterministic rules baseline



## Setup

1. Download the M5 data from [Kaggle](https://www.kaggle.com/competitions/m5-forecasting-accuracy/data) (accept the competition rules first).
2. Put `sales_train_evaluation.csv`, `calendar.csv` and `sell_prices.csv` in `data/`. The folder is gitignored; raw data is not committed.
3. Set `OPENROUTER_API_KEY` and run `dev.demandagent.App`. The first start loads the CSVs into `data/m5.duckdb`; later starts reuse it.

`src/test/.../explore/` holds the queries used to pick and sanity-check each eval case. They print findings and are skipped by `mvn test`; run them with `mvn test -DexcludedGroups=`.
