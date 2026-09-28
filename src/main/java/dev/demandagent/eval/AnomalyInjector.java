package dev.demandagent.eval;

import dev.demandagent.App;
import dev.demandagent.App.DailySales;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO (today's task, yours to write): build 5 AnomalyCases by hand.
 *
 * Start from a real series pulled via App.querySales(conn, ...), then corrupt
 * a copy of it for cases that need corrupting. Leave the real query alone;
 * work on the List<DailySales> you get back.
 *
 * Suggested 5, one method each below (stubs to fill in):
 *   1. stockout      - zero out N consecutive days in the middle of a real series
 *   2. priceHike      - needs sell_prices.csv; pick a week where price jumped,
 *                        drop the units for that week
 *   3. storeWideDip   - scale every day's units down by e.g. 50%
 *   4. eventEffect    - real event day already in the data (e.g. StPatricksDay) -
 *                        maybe no corruption needed if the effect is already there
 *   5. noChange       - untouched series, trueCause = "NOCHANGE" - the control
 *
 * Keep the true-cause label strings small and consistent - EvalRunner will
 * do simple text matching against them.
 */
public class AnomalyInjector {

    public static List<AnomalyCase> buildFiveCases(Connection conn) {
        var cases = new ArrayList<AnomalyCase>();

        // Example shape for case 1 - replace with your own item/store/dates,
        // and decide how many days to zero out and where.
        //
        // var real = App.querySales(conn, new App.GetSalesRequest(
        //         "FOODS_3_090", "CA_1", "2015-03-01", "2015-03-30"));
        // var corrupted = zeroOutDays(real, /* startIndex */ 0, /* count */ 5);
        // cases.add(new AnomalyCase("case-1-stockout", "FOODS_3_090", "CA_1",
        //         "2015-03-01", "2015-03-30", corrupted, "STOCKOUT"));

        var real = App.querySales(conn, new App.GetSalesRequest(
                "FOODS_3_090", "CA_1", "2015-03-01", "2015-03-30"
        ));

        var corrupted = zeroOutDays(real, 0, 5);

        cases.add(new AnomalyCase("case-1-stockout", "FOODS_3_090", "CA_1",
                "2015-04-01", "2015-04-30", corrupted, "STOCKOUT"));

        // 2012-09-04 to 2012-10-03: checked for a 30-day window with no calendar
        // events and no zero-unit days (April/May 2015 both turned out to have
        // real confounds - Easter, and a genuine pre-existing zero run).
        var noChangeSales = App.querySales(conn, new App.GetSalesRequest(
                "FOODS_3_090", "CA_1", "2012-09-04", "2012-10-03"
        ));
        var corrupted1 = noChangeControl(noChangeSales);
        cases.add(new AnomalyCase("case-2-no-change", "FOODS_3_090", "CA_1",
                "2012-09-04", "2012-10-03", corrupted1, "NOCHANGE"));

        // 2013-04-03 to 2013-05-02: also a clean, event-free, zero-free window,
        // different dates from case 2 so a lucky guess can't ace both.
        var storeDipSales = App.querySales(conn, new App.GetSalesRequest(
                "FOODS_3_090", "CA_1", "2013-04-03", "2013-05-02"
        ));
        var corrupted2 = storeWideDip(storeDipSales, 0.5);

        cases.add(new AnomalyCase("case-3-store-wide-dip", "FOODS_3_090", "CA_1",
                "2013-04-03", "2013-05-02", corrupted2, "STOREWIDEDIP"));

        var realEvent = App.querySales(conn, new App.GetSalesRequest(
                "FOODS_3_090", "CA_1", "2012-12-15", "2013-01-05"));

        cases.add(new AnomalyCase("case-4-event-effect", "FOODS_3_090", "CA_1",
                "2012-12-15", "2013-01-05", realEvent, "EVENT_EFFECT"));

        // Case 5: price hike. Real price history has a real jump on 2012-02-18
        // ($1.28 -> $1.44), but the real sales swing around it is too noisy to
        // trust as ground truth (checked by hand: pre/post averages ~95 vs ~70,
        // within the day-to-day noise band). So the price series stays real,
        // and we inject a clean drop on the sales side to give this case an
        // unambiguous true cause.
        var realPriceWindow = App.querySales(conn, new App.GetSalesRequest(
                "FOODS_3_090", "CA_1", "2012-01-14", "2012-03-31"));
        var realPrices = App.querySalePrice(conn, new App.GetSalePriceRequest(
                "FOODS_3_090", "CA_1", "2012-01-14", "2012-03-31"));

        var priceHikeSeries = priceHikeDrop(realPriceWindow, "2012-02-18", 0.6);

        cases.add(new AnomalyCase("case-5-price-hike", "FOODS_3_090", "CA_1",
                "2012-01-14", "2012-03-31", priceHikeSeries, "PRICE_HIKE", realPrices));

        return cases;
    }

    /**
     * Helper: returns a NEW list with `count` consecutive days starting at
     * `startIndex` set to 0 units, everything else unchanged.
     * DailySales is a record, so you can't mutate one - build new ones.
     */
    private static List<DailySales> zeroOutDays(List<DailySales> series, int startIndex, int count) {
        var out = new ArrayList<DailySales>(series.size());
        // TODO: loop with index; if i is within [startIndex, startIndex+count),
        // add new DailySales(day.date(), 0, day.event(), day.snap())
        // else add the original day unchanged
        for (int i=0; i< series.size();i++) {
            var day = series.get(i);
            if (i>=startIndex && i < startIndex + count) {
               out.add(new DailySales(day.date(), 0, day.event(), day.snap()));
            } else {
                out.add(day);
            }
        }

        return out;
    }

    private static List<DailySales> noChangeControl(List<DailySales> series) {
        var out = new ArrayList<DailySales>(series.size());
        out.addAll(series);
        return out;
    }

    private static List<DailySales> storeWideDip(List<DailySales> series, Double scaleFactor) {
        var out = new ArrayList<DailySales>(series.size());
        for (DailySales sale : series) {
            out.add(new DailySales(sale.date(), (int)Math.round(sale.units()*scaleFactor), sale.event(), sale.snap()));
        }

        return out;
    }

    /**
     * Returns a NEW list where units on/after hikeDate are scaled down by
     * dropFactor (e.g. 0.6 = 40% fewer units), and days before hikeDate are
     * left unchanged. Dates are plain "yyyy-MM-dd" strings, so string
     * comparison works for ordering.
     */
    private static List<DailySales> priceHikeDrop(List<DailySales> series, String hikeDate, double dropFactor) {
        var out = new ArrayList<DailySales>(series.size());
        for (DailySales sale : series) {
            if (sale.date().compareTo(hikeDate) >= 0) {
                out.add(new DailySales(sale.date(), (int) Math.round(sale.units() * dropFactor), sale.event(), sale.snap()));
            } else {
                out.add(sale);
            }
        }
        return out;
    }
}
