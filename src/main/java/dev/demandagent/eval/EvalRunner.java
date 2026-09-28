package dev.demandagent.eval;

import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

/**
 * TODO (today's task, yours to write): for each AnomalyCase, ask the agent
 * what happened, and check whether its answer matches the case's trueCause.
 *
 * This does NOT call get_sales as a live tool here - the case already has its
 * (possibly corrupted) series baked in. You're evaluating "given this data,
 * does the LLM name the right cause", one step removed from the DB. That
 * keeps cases reproducible: same input, same expected label, every run.
 *
 * How you check "did it match" is your call. Simplest: does the answer
 * contain the trueCause label as a substring (case-insensitive)? That's
 * crude but a fine start - you can swap in a second LLM-as-judge call later.
 */
public class EvalRunner {

    public record Result(String caseName, String trueCause, String agentAnswer, boolean correct) {}

    public static List<Result> run(ChatClient.Builder builder, List<AnomalyCase> cases) {
        var chatClient = builder.build();
        var results = new java.util.ArrayList<Result>();

        for (AnomalyCase c : cases) {
            String prompt = buildPrompt(c);   // <- you write this
            String answer = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            results.add(new Result(c.name(), c.trueCause(), answer, scoreAnswer(answer, c.trueCause())));
        }
        return results;
    }

    private static boolean scoreAnswer(String agentAnswer, String trueCause) {
        return agentAnswer.toUpperCase().contains(trueCause.toUpperCase());
    }

    /**
     * Builds the prompt for one case: the series (and prices, if the case has
     * any), plus the exact label vocabulary so scoreAnswer's substring match
     * has something reliable to look for.
     */
    private static String buildPrompt(AnomalyCase c) {
        var sb = new StringBuilder();
        sb.append("Daily sales for item ").append(c.item())
                .append(" at store ").append(c.store())
                .append(" from ").append(c.from()).append(" to ").append(c.to()).append(":\n");
        c.series().forEach(day -> sb.append(day).append("\n"));

        if (!c.prices().isEmpty()) {
            sb.append("\nWeekly sell price over the same period:\n");
            c.prices().forEach(p -> sb.append(p).append("\n"));
        }

        sb.append("""

                What caused any unusual change in sales, if any? Answer with exactly one
                of these labels: STOCKOUT, PRICE_HIKE, STOREWIDEDIP, EVENT_EFFECT, NOCHANGE.
                Then a one-sentence explanation of the evidence you used.
                """);
        return sb.toString();
    }

    public static void printSummary(List<Result> results) {
        long correct = results.stream().filter(Result::correct).count();
        System.out.printf("Score: %d/%d%n", correct, results.size());
        results.forEach(r -> System.out.printf("  [%s] expected=%s got=%s -> %s%n",
                r.caseName(), r.trueCause(), r.agentAnswer(), r.correct() ? "OK" : "WRONG"));
    }
}
