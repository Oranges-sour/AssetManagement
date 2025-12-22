package com.orangeserver;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class MyDataBase {

    public static DataSource getDataSource() {
        return data_source;
    }

    private static DataSource buildDataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl(
                "jdbc:mysql://localhost:3306/orange_db?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false");
        config.setUsername("orange");
        config.setPassword("1234567");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);

        logger.info("创建了DataSource");
        return new HikariDataSource(config);
    }

    private static final Logger logger = LoggerFactory.getLogger(MyDataBase.class);

    private static DataSource data_source;

    static {
        data_source = buildDataSource();
    }
}
