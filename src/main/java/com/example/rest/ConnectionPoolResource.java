package com.example.rest;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import javax.sql.DataSource;
import javax.sql.ConnectionPoolDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;

@Path("/connection-pool")
@Stateless
public class ConnectionPoolResource {

    @Resource(lookup = "jdbc/postgres")
    private DataSource dataSource;

    @GET
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public Response testConnectionPool() {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            result.put("status", "success");
            result.put("database", metaData.getDatabaseProductName());
            result.put("version", metaData.getDatabaseProductVersion());
            result.put("driver", metaData.getDriverName());
            result.put("driverVersion", metaData.getDriverVersion());
            result.put("url", metaData.getURL());
            result.put("username", metaData.getUserName());
            result.put("connectionIsValid", conn.isValid(5));
            
            if (dataSource instanceof ConnectionPoolDataSource) {
                result.put("connectionPoolType", "ConnectionPoolDataSource");
            } else {
                result.put("connectionPoolType", dataSource.getClass().getName());
            }
            
        } catch (SQLException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        
        return Response.ok(result).build();
    }

    @GET
    @Path("/stress")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stressTest() {
        Map<String, Object> result = new HashMap<>();
        result.put("test", "Connection Pool Stress Test");
        
        long startTime = System.currentTimeMillis();
        int connectionsToTest = 10;
        
        try {
            Connection[] connections = new Connection[connectionsToTest];
            
            for (int i = 0; i < connectionsToTest; i++) {
                connections[i] = dataSource.getConnection();
                result.put("connection_" + i, "acquired");
                
                connections[i].createStatement().execute("SELECT " + (i + 1));
            }
            
            for (int i = 0; i < connectionsToTest; i++) {
                if (connections[i] != null && !connections[i].isClosed()) {
                    connections[i].close();
                    result.put("connection_" + i + "_closed", true);
                }
            }
            
            long endTime = System.currentTimeMillis();
            result.put("totalTimeMs", endTime - startTime);
            result.put("connectionsTested", connectionsToTest);
            result.put("status", "passed");
            
        } catch (SQLException e) {
            result.put("status", "failed");
            result.put("error", e.getMessage());
        }
        
        return Response.ok(result).build();
    }
}
