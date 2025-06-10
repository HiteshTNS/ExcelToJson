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
        // Collect all validation error messages
        List<String> errors = exception.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        // Create StandardResponse with the status code, message, and the list of errors
        StandardResponse<List<String>> errorResponse = new StandardResponse<>(
                400,  // HTTP status code for Bad Request
                "Validation Error",  // Message
                errors  // List of validation error messages as data
        );

        return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
    }
}
