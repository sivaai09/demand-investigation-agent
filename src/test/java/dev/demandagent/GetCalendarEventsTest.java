package dev.demandagent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GetCalendarEventsTest {

    @Test
    void returnsOnlyDaysWithEvents() throws Exception {
        try (var conn = new DuckDbConfig().duckDbConnection()) {
            var events = App.queryCalendarEvents(conn,
                    new App.GetCalendarEventsRequest("2012-12-20", "2013-01-02"));

            var names = events.stream().map(App.CalendarEvent::name).toList();
            assertTrue(names.contains("Christmas"));
            assertTrue(names.contains("NewYear"));
            assertTrue(events.size() < 14, "should not return one row per day");
            events.forEach(System.out::println);
        }
    }

    @Test
    void quietStretchReturnsEmptyList() throws Exception {
        try (var conn = new DuckDbConfig().duckDbConnection()) {
            // clean 30-day window found earlier for the eval: no calendar events at all
            var events = App.queryCalendarEvents(conn,
                    new App.GetCalendarEventsRequest("2012-09-04", "2012-10-03"));
            assertTrue(events.isEmpty());
        }
    }
}
