package dev.demandagent.explore;

import dev.demandagent.DuckDbConfig;
import org.junit.jupiter.api.Tag;

import org.junit.jupiter.api.Test;

/**
 * One-off: find a clean 30-day window for FOODS_3_090/CA_1 with no calendar
 * events and no zero-unit days, to use for case-2 (control) and case-3
 * (store-wide dip) instead of April/May 2015 which both had confounds.
 */
@Tag("explore")
class CleanWindowFinderTest {

    @Test
    void findCleanMonths() throws Exception {
        try (var conn = new DuckDbConfig().duckDbConnection()) {
            var sql = """
                    WITH one_row AS (
                        SELECT * FROM sales WHERE item_id = 'FOODS_3_090' AND store_id = 'CA_1'
                    ),
                    long AS (
                        SELECT day, units
                        FROM one_row
                        UNPIVOT (units FOR day IN (COLUMNS(c -> c LIKE 'd\\_%' ESCAPE '\\')))
                    ),
                    daily AS (
                        SELECT c.date, l.units, c.event_name_1
                        FROM long l JOIN calendar c ON l.day = c.d
                    ),
                    windows AS (
                        SELECT date AS window_start,
                               MIN(units) OVER (ORDER BY date ROWS BETWEEN CURRENT ROW AND 29 FOLLOWING) AS min_units,
                               SUM(CASE WHEN event_name_1 IS NOT NULL THEN 1 ELSE 0 END)
                                   OVER (ORDER BY date ROWS BETWEEN CURRENT ROW AND 29 FOLLOWING) AS event_count,
                               COUNT(*) OVER (ORDER BY date ROWS BETWEEN CURRENT ROW AND 29 FOLLOWING) AS days_covered
                        FROM daily
                    )
                    SELECT window_start, min_units, event_count
                    FROM windows
                    WHERE days_covered = 30 AND min_units > 0 AND event_count = 0
                    ORDER BY window_start
                    LIMIT 10
                    """;
            try (var stmt = conn.prepareStatement(sql); var rs = stmt.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("clean 30-day window starting %s (min_units=%d, events=%d)%n",
                            rs.getString("window_start"), rs.getInt("min_units"), rs.getInt("event_count"));
                }
                if (!any) System.out.println("No fully clean 30-day window found for this item/store.");
            }
        }
    }
}
