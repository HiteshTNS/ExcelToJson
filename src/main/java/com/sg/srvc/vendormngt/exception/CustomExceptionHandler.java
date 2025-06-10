package com.sg.srvc.vendormngt.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CustomExceptionHandler implements ExceptionMapper<CustomException> {

    @Override
    public Response toResponse(CustomException exception) {
        // Create StandardResponse with status code, message, and exception message as data
        StandardResponse<String> errorResponse = new StandardResponse<>(
                400,  // HTTP status code for Bad Request
                "Error",
                exception.getMessage()  // Set exception message as data
        );

        // Return the response with a structured message
        return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
    }
}
