package dev.demandagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Step 3 check: does the LLM call get_sales with valid arguments and read the result sensibly?
 * Runs on startup of App.main (needs OPENROUTER_API_KEY in the run configuration's environment variables).
 */
@Component
class SalesDemoRunner implements CommandLineRunner {

    private final ChatClient.Builder builder;

    private SalesTools salesTools;



    SalesDemoRunner(ChatClient.Builder builder, SalesTools salesTools) {
        this.builder = builder;
        this.salesTools = salesTools;
    }

    @Override
    public void run(String... args) {
        String question = args.length > 0
                ? String.join(" ", args)
                : "Why did FOODS_3_090 at CA_1 sell 0 units on 2012-12-25?";

        String answer = builder.build().prompt()
                .user(question)
                .tools(salesTools)
                .call()
                .content();

        System.out.println("Q: " + question);
        System.out.println("A: " + answer);
    }
}
