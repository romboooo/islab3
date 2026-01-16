package com.example.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

@Path("/test")
@Produces(MediaType.APPLICATION_JSON)
public class TestResource {

    @GET
    @Path("/fail-minio")
    public Response simulateMinioFailure() {
        System.setProperty("minio.fail", "true");
        return Response.ok(Map.of("status", "MinIO failure simulated")).build();
    }

    @GET
    @Path("/fail-db")
    public Response simulateDbFailure() {
        System.setProperty("db.fail", "true");
        return Response.ok(Map.of("status", "DB failure simulated")).build();
    }

    @GET
    @Path("/fail-business")
    public Response simulateBusinessFailure() {
        System.setProperty("business.fail", "true");
        return Response.ok(Map.of("status", "Business logic failure simulated")).build();
    }

    @GET
    @Path("/reset-failures")
    public Response resetFailures() {
        System.clearProperty("minio.fail");
        System.clearProperty("db.fail");
        System.clearProperty("business.fail");
        return Response.ok(Map.of("status", "All failures reset")).build();
    }
}