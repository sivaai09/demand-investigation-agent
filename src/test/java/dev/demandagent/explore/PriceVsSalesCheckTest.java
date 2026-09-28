package dev.demandagent.explore;

import dev.demandagent.App;
import dev.demandagent.DuckDbConfig;
import org.junit.jupiter.api.Tag;

import org.junit.jupiter.api.Test;

/**
 * One-off: line up sales and price side by side around the 2012-02-18 hike
 * for FOODS_3_090/CA_1, to check whether the hike actually correlates with
 * a sales drop before building case-5 on it. Not part of the app.
 */
@Tag("explore")
class PriceVsSalesCheckTest {

    @Test
    void printSalesAroundHike() throws Exception {
        try (var conn = new DuckDbConfig().duckDbConnection()) {
            var sales = App.querySales(conn,
                    new App.GetSalesRequest("FOODS_3_090", "CA_1", "2012-01-14", "2012-03-31"));
            var prices = App.querySalePrice(conn,
                    new App.GetSalePriceRequest("FOODS_3_090", "CA_1", "2012-01-14", "2012-03-31"));

            System.out.println("-- weekly prices --");
            prices.forEach(p -> System.out.printf("%s  $%.2f%n", p.weekStart(), p.sellPrice()));

            System.out.println("-- daily units --");
            sales.forEach(s -> System.out.printf("%s  units=%-3d%s%n", s.date(), s.units(),
                    s.date().compareTo("2012-02-18") >= 0 ? "  [post-hike]" : ""));
        }
    }
}
