package com.example.rest;

import com.example.entity.ImportHistory;
import com.example.entity.ImportStatus;
import com.example.service.ImportService;
import com.example.service.MinioService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Path("/import")
@Produces(MediaType.APPLICATION_JSON)
public class ImportResource {

    @Inject
    private ImportService importService;

    @POST
    @Path("/dragons")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importDragons(@FormDataParam("file") InputStream fileInputStream,
                                  @FormDataParam("file") FormDataContentDisposition fileDetail) {
        try {
            String filename = fileDetail.getFileName();
            long fileSize = fileDetail.getSize();

            ImportHistory importHistory = importService.importDragonsFromJson(
                    fileInputStream, filename, fileSize);

            Map<String, Object> response = new HashMap<>();
            response.put("status", importHistory.getStatus().toString());
            response.put("importId", importHistory.getId());
            response.put("recordsProcessed", importHistory.getRecordsProcessed());
            response.put("filename", importHistory.getFilename());

            if (importHistory.getFileUrl() != null) {
                response.put("fileUrl", importHistory.getFileUrl());
            }

            if (importHistory.getFileSize() != null) {
                response.put("fileSize", importHistory.getFileSize());
            }

            String message;
            switch (importHistory.getStatus()) {
                case SUCCESS:
                    message = "Import successful. " + importHistory.getRecordsProcessed() + " records processed.";
                    break;
                case PARTIAL_SUCCESS:
                    message = "Import partially completed. " + importHistory.getRecordsProcessed() + " records processed.";
                    break;
                case FAILED:
                    message = "Import failed.";
                    break;
                default:
                    message = "Import completed.";
            }
            response.put("message", message);

            if (importHistory.getErrorMessage() != null) {
                response.put("errorMessage", importHistory.getErrorMessage());
            }

            return Response.ok(response).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "error", "Import failed",
                            "message", e.getMessage(),
                            "status", "FAILED"
                    )).build();
        }
    }

    @GET
    @Path("/history/{id}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadImportFile(@PathParam("id") Long id) {
        try {
            byte[] fileContent = importService.getImportFile(id);
            ImportHistory history = importService.getImportHistoryById(id);

            return Response.ok(fileContent)
                    .header("Content-Disposition",
                            "attachment; filename=\"" + history.getFilename() + "\"")
                    .header("Content-Type", "application/json")
                    .header("Content-Length", fileContent.length)
                    .build();
        } catch (FileNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "File not found"))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to download file: " + e.getMessage()))
                    .build();
        }
    }


    @GET
    @Path("/history/{id}/file-info")
    public Response getImportFileInfo(@PathParam("id") Long id) {
        try {
            MinioService.FileMetadata metadata = importService.getImportFileMetadata(id);
            ImportHistory history = importService.getImportHistoryById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("filename", history.getFilename());
            response.put("fileUrl", history.getFileUrl());
            response.put("size", metadata.getSize());
            response.put("contentType", metadata.getContentType());
            response.put("lastModified", metadata.getLastModified());
            response.put("downloadUrl", "/api/import/history/" + id + "/download");

            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get file info: " + e.getMessage()))
                    .build();
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