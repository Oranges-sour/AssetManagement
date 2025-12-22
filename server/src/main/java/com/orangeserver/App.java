package com.orangeserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Hello world!
 */
public final class App {
    private App() {
    }

    /**
     * Says hello to the world.
     * 
     * @param args The arguments of the program.
     */
    public static void main(String[] args) {
        System.out.println("Hello World!");

        String url = "jdbc:mysql://localhost:3306/orange_db?serverTimezone=UTC";
        String user = "orange";
        String pass = "1234567";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("连接成功");

            String sql = "DESC department";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                // ps.setInt(1, 1);

                try (ResultSet rs = ps.executeQuery()) {
                    System.out.println(rs.);
                    // while (rs.next()) {
                    //     int id = rs.getInt("id");
                    //     String name = rs.getString("name");
                    //     System.out.println(id + " " + name);
                    // }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
