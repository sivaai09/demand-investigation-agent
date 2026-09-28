package dev.demandagent.explore;

import dev.demandagent.App;
import dev.demandagent.DuckDbConfig;
import org.junit.jupiter.api.Tag;

import org.junit.jupiter.api.Test;

/** One-off: inspect the raw April/May 2015 series behind the 2 failed eval cases. */
@Tag("explore")
class DiagnoseFailuresTest {

    @Test
    void printAprilAndMay() throws Exception {
        try (var conn = new DuckDbConfig().duckDbConnection()) {
            System.out.println("-- April 2015 (case-2, no-change control) --");
            App.querySales(conn, new App.GetSalesRequest("FOODS_3_090", "CA_1", "2015-04-01", "2015-04-30"))
                    .forEach(System.out::println);

            System.out.println("-- May 2015 (case-3, store-wide dip, pre-scaling) --");
            App.querySales(conn, new App.GetSalesRequest("FOODS_3_090", "CA_1", "2015-05-01", "2015-05-30"))
                    .forEach(System.out::println);
        }
    }
}
