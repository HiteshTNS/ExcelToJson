package com.sg.srvc.vendormngt.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CustomefilenotfoundExceptionHandler implements ExceptionMapper<CustomefilenotfoundException> {

    @Override
    public Response toResponse(CustomefilenotfoundException exception) {
        // Assuming StandardResponse is a generic response class you've defined
        StandardResponse<String> errorResponse = new StandardResponse<>(
                404,
                "Error",
                exception.getMessage()
        );

        return Response.status(Response.Status.NOT_FOUND)
                .entity(errorResponse)
                .build();
    }
}
