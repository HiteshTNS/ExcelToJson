package com.sg.srvc.vendormngt.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class CustomExceptionHandler implements ExceptionMapper<CustomException> {

    @Override
    public Response toResponse(CustomException exception) {
        StandardResponse<List<String>> errorResponse = new StandardResponse<>(
                400,
                "Error",
                List.of(exception.getMessage())
        );
        return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
    }
}