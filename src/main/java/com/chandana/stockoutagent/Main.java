package com.chandana.stockoutagent;

import java.sql.DriverManager;
import java.sql.SQLException;

class Main {


    public static void main(String[] args) {

        try (var conn = DriverManager.getConnection("jdbc:duckdb:");
             var stmt = conn.createStatement()) {

            // STEP 1: load the CSV into a real table, once
            stmt.execute("""
        CREATE TABLE sales AS
        SELECT * FROM read_csv_auto('data/sales_train_evaluation.csv')
    """);

            // STEP 2: reshape that table from wide to long, for one item-store pair
            var rs = stmt.executeQuery("""
            SELECT * FROM sales UNPIVOT (units FOR day IN (COLUMNS(c -> c LIKE 'd\\_%' ESCAPE '\\')))
                                WHERE item_id = 'FOODS_3_090' AND store_id = 'CA_1'
                                ORDER BY day
                                LIMIT 30
    """);

            while (rs.next()) {
                System.out.println(rs.getString("day") + " -> " + rs.getInt("units"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}