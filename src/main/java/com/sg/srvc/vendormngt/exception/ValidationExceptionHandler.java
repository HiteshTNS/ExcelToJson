package com.sg.srvc.vendormngt.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;
import java.util.stream.Collectors;

@Provider
public class ValidationExceptionHandler implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<String> errors = exception.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        // Create the StandardResponse with the appropriate constructor
        StandardResponse<List<String>> errorResponse = new StandardResponse<>(
                400,  // HTTP status code
                "Validation Error",  // Error message
                errors  // List of validation error messages
        );

        return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
    }
}
