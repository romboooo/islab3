package com.example.rest;

import com.example.entity.ImportHistory;
import com.example.service.ImportService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.util.Map;

@Path("/import")
@Produces(MediaType.APPLICATION_JSON)
public class ImportResource {

    @Inject
    private ImportService importService;

    @POST
    @Path("/dragons")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importDragons(@FormParam("file") InputStream fileInputStream,
                                  @FormParam("filename") String filename) {
        try {
            ImportHistory importHistory = importService.importDragonsFromJson(fileInputStream, filename);

            // Краткие сообщения для клиента
            String message;
            if (importHistory.getStatus() == com.example.entity.ImportStatus.SUCCESS) {
                message = "Import successful. Check import history for details.";
            } else if (importHistory.getStatus() == com.example.entity.ImportStatus.PARTIAL_SUCCESS) {
                message = "Import partially completed. Check import history for details.";
            } else {
                message = "Import failed. Check import history for details.";
            }

            return Response.ok(Map.of(
                    "status", importHistory.getStatus(),
                    "importId", importHistory.getId(),
                    "recordsProcessed", importHistory.getRecordsProcessed(),
                    "errorMessage", importHistory.getErrorMessage() != null ? importHistory.getErrorMessage() : "",
                    "message", message
            )).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "error", "Import failed",
                            "message", "Internal server error",
                            "status", "FAILED"
                    )).build();
        }
    }

    @GET
    @Path("/history")
    public Response getImportHistory() {
        try {
            return Response.ok(importService.getImportHistory()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get import history: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/history/{id}")
    public Response getImportHistoryById(@PathParam("id") Long id) {
        try {
            ImportHistory history = importService.getImportHistoryById(id);
            if (history != null) {
                return Response.ok(history).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Import history not found"))
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error getting import history: " + e.getMessage()))
                    .build();
        }
    }
}