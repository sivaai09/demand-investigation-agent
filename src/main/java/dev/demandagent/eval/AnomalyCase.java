package dev.demandagent.eval;

import dev.demandagent.App.DailySales;
import dev.demandagent.App.WeeklyPrice;

import java.util.List;

/**
 * One eval case: a (possibly corrupted) sales series, its price history if the
 * case needs it, plus the ground truth that AnomalyInjector attached when it
 * built the case. Everything is baked in ahead of time so a run is
 * reproducible - EvalRunner never calls get_sales/get_sales_price live.
 *
 * trueCause is a short label you invent and stay consistent with, e.g.
 * "STOCKOUT", "PRICE_HIKE", "STORE_DIP", "EVENT_EFFECT", "NONE".
 * EvalRunner will compare the agent's free-text answer against this label,
 * so keep the set of labels small and exact.
 */
public record AnomalyCase(
        String name,             // e.g. "case-1-stockout"
        String item,
        String store,
        String from,
        String to,
        List<DailySales> series,   // the (possibly injected) daily rows the agent will see
        String trueCause,          // ground truth, set by whoever builds the case
        List<WeeklyPrice> prices   // weekly prices, empty for cases that don't need them
) {
    // Convenience constructor for cases that don't involve price - keeps the
    // 4 earlier cases' call sites unchanged.
    public AnomalyCase(String name, String item, String store, String from, String to,
                        List<DailySales> series, String trueCause) {
        this(name, item, store, from, to, series, trueCause, List.of());
    }
}
