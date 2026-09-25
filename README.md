# demand-investigation-agent
Investigate & analyse the trends in the change of demand of a product

Uses the M5 dataset from Kaggle: https://www.kaggle.com/competitions/m5-forecasting-accuracy/data

Built in Java 21, Spring Boot, Spring AI, MCP. The deterministic tools compute the signals(price changes, zero sales runs, category trends), 
the LLM plans the investigation and weighs the evidence when causes overlap or the question is open-ended 

It is evaluated against injected, ground-truth anomalies and benchmarked against a deterministic rules baseline


