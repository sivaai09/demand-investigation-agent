package dev.demandagent.explore;

import dev.demandagent.App;
import dev.demandagent.DuckDbConfig;
import org.junit.jupiter.api.Tag;

import org.junit.jupiter.api.Test;

/**
 * One-off exploration: find an (item, store, event day) where sales swing hard
 * versus the 3 days right before it, to pick a strong case-4 (event effect)
 * candidate. Not part of the app - safe to delete once you've picked one.
 */
@Tag("explore")
class EventEffectFinderTest {

    @Test
    void findBigEventSwings() throws Exception {
        try (var conn = new DuckDbConfig().duckDbConnection()) {
            // Restrict to CA_1 first so UNPIVOT only processes ~3k rows, not all 30k.
            var sql = """
                    WITH one_store AS (
                        SELECT * FROM sales WHERE store_id = 'CA_1'
                    ),
                    long AS (
                        SELECT item_id, store_id, day, units
                        FROM one_store
                        UNPIVOT (units FOR day IN (COLUMNS(c -> c LIKE 'd\\_%' ESCAPE '\\')))
                    ),
                    joined AS (
                        SELECT l.item_id, l.store_id, c.date, c.event_name_1, c.event_type_1, l.units,
                               AVG(l.units) OVER (
                                   PARTITION BY l.item_id
                                   ORDER BY c.date
                                   ROWS BETWEEN 3 PRECEDING AND 1 PRECEDING
                               ) AS baseline
                        FROM long l JOIN calendar c ON l.day = c.d
                    )
                    SELECT item_id, store_id, date, event_name_1, event_type_1, units, baseline,
                           (units - baseline) AS swing
                    FROM joined
                    WHERE event_name_1 IS NOT NULL
                      AND baseline >= 10          -- skip near-zero items, swing there is just noise
                    ORDER BY ABS(units - baseline) DESC
                    LIMIT 20
                    """;
            try (var stmt = conn.prepareStatement(sql); var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("%-16s %-6s %-11s %-18s units=%-4d baseline=%-6.1f swing=%+.1f%n",
                            rs.getString("item_id"), rs.getString("store_id"), rs.getString("date"),
                            rs.getString("event_name_1"), rs.getInt("units"), rs.getDouble("baseline"),
                            rs.getDouble("swing"));
                }
            }
        }
    }
}
