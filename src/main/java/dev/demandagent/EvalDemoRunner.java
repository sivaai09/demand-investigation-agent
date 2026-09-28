package dev.demandagent;

import dev.demandagent.eval.AnomalyInjector;
import dev.demandagent.eval.EvalRunner;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.sql.Connection;

/**
 * Runs the 5 hand-built anomaly cases through the LLM and prints the score.
 * Runs on startup of App.main, right after SalesDemoRunner.
 */
@Component
class EvalDemoRunner implements CommandLineRunner {

    private final ChatClient.Builder builder;
    private final Connection conn;

    EvalDemoRunner(ChatClient.Builder builder, Connection conn) {
        this.builder = builder;
        this.conn = conn;
    }

    @Override
    public void run(String... args) {
        var cases = AnomalyInjector.buildFiveCases(conn);
        var results = EvalRunner.run(builder, cases);
        EvalRunner.printSummary(results);
    }
}
