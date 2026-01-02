package com.example.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import javax.sql.DataSource;

@ApplicationScoped
public class DataSourceProducer {

    @Inject
    private HikariConnectionPool hikariConnectionPool;

    @Produces
    @ApplicationScoped
    public DataSource produceDataSource() {
        return hikariConnectionPool.getDataSource();
    }
}