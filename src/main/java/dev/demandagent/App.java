package dev.demandagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class App {
    // Tool output is LLM prompt input, so cap how many days one call can return.
    static final int MAX_ROWS = 366;

    @Bean
    @Profile("chat-test")
    CommandLineRunner testChat(ChatClient.Builder builder) {
        return args -> {
            var chatClient = builder.build();
            String response = chatClient.prompt()
                    .user("Say hello in one sentence.")
                    .call()
                    .content();
            System.out.println(response);
        };
    }

    public record GetSalesRequest(String item, String store, String from, String to) {}

    public record DailySales(String date, int units, String event, int snap) {}

    public record GetSalePriceRequest(String item, String store, String from, String to) {}
    public record WeeklyPrice(String weekStart, double sellPrice) {}

    public record GetCalendarEventsRequest(String from, String to) {}
    public record CalendarEvent(String date, String name, String type) {}

    @Bean
    ToolCallback getSalesTool(Connection conn) {
        return FunctionToolCallback.builder("get_sales", (GetSalesRequest req) -> querySales(conn, req))
                .description("""
                        Get daily unit sales for one item at one store between two dates (max 366 days). \
                        item is like FOODS_3_090, store is like CA_1, dates are yyyy-MM-dd. \
                        Each row has the date, units sold, the calendar event that day (null if none), \
                        and snap (1 if the store's state had SNAP food-stamp benefits that day).""")
                .inputType(GetSalesRequest.class)
                .build();
    }

    @Bean
    ToolCallback getSalesPriceTool(Connection conn) {
        return FunctionToolCallback.builder("get_sales_price", (GetSalePriceRequest req) -> querySalePrice(conn, req))
                .description("""
                        Get weekly sell price for one item at one store between two dates (max 366 days). \
                        item is like FOODS_3_090, store is like CA_1, dates are yyyy-MM-dd. \
                        Each row has the week's start date and sell price that week.""")
                .inputType(GetSalePriceRequest.class)
                .build();
    }

    @Bean
    ToolCallback getCalendarEventsTool(Connection conn) {
        return FunctionToolCallback.builder("get_calendar_events", (GetCalendarEventsRequest req) -> queryCalendarEvents(conn, req))
                .description("""
                        Get calendar events (holidays, religious and sporting events) between two dates \
                        (max 366 rows). Events apply to every store. dates are yyyy-MM-dd. \
                        Only days that have an event are returned, so an empty list means no events. \
                        Each row has the date, the event name and its type (National, Religious, Cultural, Sporting).""")
                .inputType(GetCalendarEventsRequest.class)
                .build();
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

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
