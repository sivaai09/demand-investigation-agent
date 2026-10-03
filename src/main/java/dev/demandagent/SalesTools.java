package dev.demandagent;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class SalesTools {
    static final int MAX_ROWS = 366;

    public record GetSalesRequest(String item, String store, String from, String to) {}

    public record DailySales(String date, int units, String event, int snap) {}

    public record GetSalePriceRequest(String item, String store, String from, String to) {}
    public record WeeklyPrice(String weekStart, double sellPrice) {}

    public record GetCalendarEventsRequest(String from, String to) {}
    public record CalendarEvent(String date, String name, String type) {}

    // Injected by Spring, never shown to the LLM. Only @Tool method parameters become the tool's input schema.
    private final Connection conn;

    public SalesTools(Connection conn) {
        this.conn = conn;
    }

    @Tool(name = "get_sales", description = """
            Get daily unit sales for one item at one store between two dates (max 366 days). \
            Each row has the date, units sold, the calendar event that day (null if none), \
            and snap (1 if the store's state had SNAP food-stamp benefits that day).""")
    public List<DailySales> getSales(
            @ToolParam(description = "item id, like FOODS_3_090") String item,
            @ToolParam(description = "store id, like CA_1") String store,
            @ToolParam(description = "first date, yyyy-MM-dd") String from,
            @ToolParam(description = "last date, yyyy-MM-dd") String to) {
        return querySales(conn, new GetSalesRequest(item, store, from, to));
    }

    @Tool(name = "get_sales_price", description = """
            Get weekly sell price for one item at one store between two dates (max 366 days). \
            Each row has the week's start date and the sell price that week.""")
    public List<WeeklyPrice> getSalesPrice(
            @ToolParam(description = "item id, like FOODS_3_090") String item,
            @ToolParam(description = "store id, like CA_1") String store,
            @ToolParam(description = "first date, yyyy-MM-dd") String from,
            @ToolParam(description = "last date, yyyy-MM-dd") String to) {
        return querySalePrice(conn, new GetSalePriceRequest(item, store, from, to));
    }

    @Tool(name = "get_calendar_events", description = """
            Get calendar events (holidays, religious and sporting events) between two dates \
            (max 366 rows). Events apply to every store. Only days that have an event are returned, \
            so an empty list means no events. Each row has the date, the event name and its type \
            (National, Religious, Cultural, Sporting).""")
    public List<CalendarEvent> getCalendarEvents(
            @ToolParam(description = "first date, yyyy-MM-dd") String from,
            @ToolParam(description = "last date, yyyy-MM-dd") String to) {
        return queryCalendarEvents(conn, new GetCalendarEventsRequest(from, to));
    }

    public static List<DailySales> querySales(Connection conn, GetSalesRequest req) {
        // Filter to the one item-store row first, so only 1 row gets unpivoted, not all 30,490.
        var sql = """
                WITH one_row AS (
                    SELECT * FROM sales WHERE item_id = ? AND store_id = ?
                ),
                long AS (
                    SELECT state_id, day, units
                    FROM one_row
                    UNPIVOT (units FOR day IN (COLUMNS(c -> c LIKE 'd\\_%' ESCAPE '\\')))
                )
                SELECT c.date, l.units, c.event_name_1,
                       CASE l.state_id WHEN 'CA' THEN c.snap_CA
                                       WHEN 'TX' THEN c.snap_TX
                                       WHEN 'WI' THEN c.snap_WI END AS snap
                FROM long l JOIN calendar c ON l.day = c.d
                WHERE c.date BETWEEN ? AND ?
                ORDER BY c.date
                LIMIT ?
                """;

        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, req.item());
            ps.setString(2, req.store());
            ps.setDate(3, Date.valueOf(LocalDate.parse(req.from())));
            ps.setDate(4, Date.valueOf(LocalDate.parse(req.to())));
            ps.setInt(5, MAX_ROWS);
            try (var rs = ps.executeQuery()) {
                var out = new ArrayList<DailySales>();
                while (rs.next()) {
                    out.add(new DailySales(
                            rs.getString("date"),
                            rs.getInt("units"),
                            rs.getString("event_name_1"),   // null on most days, and that is fine
                            rs.getInt("snap")));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);                  // Function can't throw checked exceptions
        }
    }

    public static List<WeeklyPrice> querySalePrice(Connection conn, GetSalePriceRequest req) {
        var sql = """
                SELECT MIN(c.date) AS week_start, sp.sell_price
                FROM calendar c
                JOIN sell_prices sp ON c.wm_yr_wk = sp.wm_yr_wk
                WHERE sp.item_id = ? AND sp.store_id = ?
                  AND c.date BETWEEN ? AND ?
                GROUP BY sp.wm_yr_wk, sp.sell_price
                ORDER BY week_start
                """;

        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, req.item());
            ps.setString(2, req.store());
            ps.setDate(3, Date.valueOf(LocalDate.parse(req.from())));
            ps.setDate(4, Date.valueOf(LocalDate.parse(req.to())));
            try (var rs = ps.executeQuery()) {
                var out = new ArrayList<WeeklyPrice>();
                while (rs.next()) {
                    out.add(new WeeklyPrice(
                            rs.getString("week_start"),
                            rs.getDouble("sell_price")
                    ));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);                  // Function can't throw checked exceptions
        }
    }

    public static List<CalendarEvent> queryCalendarEvents(Connection conn, GetCalendarEventsRequest req) {
        // A day can carry two events (slot 1 and slot 2), so read both slots and merge them.
        var sql = """
                SELECT date, event_name_1 AS name, event_type_1 AS type
                FROM calendar
                WHERE event_name_1 IS NOT NULL AND date BETWEEN ? AND ?
                UNION ALL
                SELECT date, event_name_2, event_type_2
                FROM calendar
                WHERE event_name_2 IS NOT NULL AND date BETWEEN ? AND ?
                ORDER BY date
                LIMIT ?
                """;

        try (var ps = conn.prepareStatement(sql)) {
            var from = Date.valueOf(LocalDate.parse(req.from()));
            var to = Date.valueOf(LocalDate.parse(req.to()));
            ps.setDate(1, from);
            ps.setDate(2, to);
            ps.setDate(3, from);
            ps.setDate(4, to);
            ps.setInt(5, MAX_ROWS);
            try (var rs = ps.executeQuery()) {
                var out = new ArrayList<CalendarEvent>();
                while (rs.next()) {
                    out.add(new CalendarEvent(
                            rs.getString("date"),
                            rs.getString("name"),
                            rs.getString("type")));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
