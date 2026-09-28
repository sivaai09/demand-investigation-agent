package dev.demandagent;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Configuration
public class DuckDbConfig {

    private static final String DB_URL = "jdbc:duckdb:data/m5.duckdb";

    // destroyMethod closes the connection on shutdown (Spring infers "close" for Connection beans)
    @Bean
    public Connection duckDbConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        loadIfMissing(conn, "sales", "data/sales_train_evaluation.csv");
        loadIfMissing(conn, "calendar", "data/calendar.csv");
        loadIfMissing(conn, "sell_prices", "data/sell_prices.csv");
        return conn;
    }

    private void loadIfMissing(Connection conn, String table, String csvPath) throws SQLException {
        try (var check = conn.prepareStatement(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = ?")) {
            check.setString(1, table);
            try (var rs = check.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    return;
                }
            }
        }
        // table/csvPath are constants from this class, never user input
        try (var stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE " + table + " AS SELECT * FROM read_csv_auto('" + csvPath + "')");
        }
    }
}
