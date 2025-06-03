package com.sg.srvc.vendormngt.exception;

import lombok.Data;

@Data
public class StandardResponse<T> {
    private int statusCode;
    private String message;
    private T data;

    // Constructor to initialize the response with status code, message, and data
    public StandardResponse(int statusCode, String message, T data) {
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
    }
}
