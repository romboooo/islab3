package com.example.rest;

import com.example.dto.DragonDto;
import com.example.service.DragonService;
import com.example.websocket.DragonWebSocket;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/dragons")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DragonResource {

    @Inject
    private DragonService dragonService;

    @GET
    @Path("/{id}")
    public Response getDragon(@PathParam("id") Long id) {
        DragonDto dragon = dragonService.findById(id);
        if (dragon == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(dragon).build();
    }

    @GET
    public Response getAllDragons(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("name") String name,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder) {

        Map<String, Object> filters = new HashMap<>();
        if (name != null && !name.isEmpty()) {
            filters.put("name", name);
        }

        List<DragonDto> dragons = dragonService.findWithFilters(filters, page, size, sortBy, sortOrder);
        Long totalCount = dragonService.countWithFilters(filters);

        Map<String, Object> response = new HashMap<>();
        response.put("data", dragons);
        response.put("total", totalCount);
        response.put("page", page);
        response.put("size", size);

        return Response.ok(response).build();
    }

    @POST
    public Response createDragon(@Valid DragonDto dragonDto) {
        try {
            DragonDto created = dragonService.save(dragonDto);
            DragonWebSocket.broadcast("CREATE " + created.getId());
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateDragon(@PathParam("id") Long id, @Valid DragonDto dragonDto) {
        dragonDto.setId(id);
        try {
            DragonDto updated = dragonService.save(dragonDto);
            DragonWebSocket.broadcast("UPDATE " + updated.getId());
            return Response.ok(updated).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteDragon(
            @PathParam("id") Long id,
            @QueryParam("newKillerId") Long newKillerId) {
        try {
            dragonService.delete(id, newKillerId);
            DragonWebSocket.broadcast("DELETE " + id);
            return Response.noContent().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/special/sum-ages")
    public Response getSumOfAges() {
        Long sum = dragonService.getSumOfAges();
        return Response.ok(Map.of("sumOfAges", sum)).build();
    }

    @GET
    @Path("/special/weight-greater-than/{weight}")
    public Response getByWeightGreaterThan(@PathParam("weight") Float weight) {
        List<DragonDto> dragons = dragonService.findByWeightGreaterThan(weight);
        return Response.ok(dragons).build();
    }

    @GET
    @Path("/special/unique-ages")
    public Response getUniqueAges() {
        List<Long> ages = dragonService.findUniqueAges();
        return Response.ok(ages).build();
    }
}