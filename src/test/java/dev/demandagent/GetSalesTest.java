package dev.demandagent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GetSalesTest {

    @Test
    void returnsOneRowPerDayInRange() throws Exception {
        try (var conn = new DuckDbConfig().duckDbConnection()) {
            var rows = App.querySales(conn,
                    new App.GetSalesRequest("FOODS_3_090", "CA_1", "2015-03-01", "2015-03-30"));

            assertEquals(30, rows.size());
            assertEquals("2015-03-01", rows.get(0).date());
            assertEquals("2015-03-30", rows.get(29).date());
            rows.forEach(r -> assertTrue(r.units() >= 0));
            rows.forEach(r -> assertTrue(r.snap() == 0 || r.snap() == 1));
            rows.forEach(System.out::println);
        }
    }

    @Test
    void capsRowsForHugeRange() throws Exception {
        try (var conn = new DuckDbConfig().duckDbConnection()) {
            var rows = App.querySales(conn,
                    new App.GetSalesRequest("FOODS_3_090", "CA_1", "2011-01-29", "2016-05-22"));
            assertEquals(App.MAX_ROWS, rows.size());
        }
    }
}
