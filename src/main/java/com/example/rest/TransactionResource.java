package com.example.rest;

import com.example.dao.DistributedTransactionDao;
import com.example.entity.DistributedTransaction;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/transactions")
@Produces(MediaType.APPLICATION_JSON)
public class TransactionResource {

    @Inject
    private DistributedTransactionDao transactionDao;

    @GET
    public Response getAllTransactions() {
        List<DistributedTransaction> transactions = transactionDao.findAll();
        return Response.ok(transactions).build();
    }
}