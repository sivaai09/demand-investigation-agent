package dev.demandagent.explore;

import dev.demandagent.DuckDbConfig;
import dev.demandagent.SalesTools;
import org.junit.jupiter.api.Tag;

import org.junit.jupiter.api.Test;

/**
 * One-off exploration: eyeball real weekly prices for FOODS_3_090/CA_1 to find
 * a real jump to use as the case-5 (price hike) anchor. Not part of the app.
 */
@Tag("explore")
class PriceCheckTest {

    @Test
    void printWeeklyPrices() throws Exception {
        try (var conn = new DuckDbConfig().duckDbConnection()) {
            var prices = SalesTools.querySalePrice(conn,
                    new SalesTools.GetSalePriceRequest("FOODS_3_090", "CA_1", "2011-01-29", "2016-05-22"));

            double prev = -1;
            for (var p : prices) {
                String flag = (prev >= 0 && p.sellPrice() != prev) ? "  <-- price changed" : "";
                System.out.printf("%s  $%.2f%s%n", p.weekStart(), p.sellPrice(), flag);
                prev = p.sellPrice();
            }
        }
    }
}
