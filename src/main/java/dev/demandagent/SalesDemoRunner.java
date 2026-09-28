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
    private final ToolCallback getSalesTool;

    SalesDemoRunner(ChatClient.Builder builder, ToolCallback getSalesTool) {
        this.builder = builder;
        this.getSalesTool = getSalesTool;
    }

    @Override
    public void run(String... args) {
        String question = args.length > 0
                ? String.join(" ", args)
                : "How did item FOODS_3_090 sell at store CA_1 in March 2015? Anything unusual?";

        String answer = builder.build().prompt()
                .user(question)
                .toolCallbacks(getSalesTool)
                .call()
                .content();

        System.out.println("Q: " + question);
        System.out.println("A: " + answer);
    }
}
