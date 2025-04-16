package com.sg.srvc.vendormngt.response;

import java.util.List;

public class StandardResponse<T> {
    private int responseCode;
    private String responseDiscription;
    private T data;

    // Constructors
    public StandardResponse() {}

    public StandardResponse(int responseCode, String responseDiscription, T data) {
        this.responseCode = responseCode;
        this.responseDiscription = responseDiscription;
        this.data = data;
    }

    // Getters and Setters
    public int getResponseCode() { return responseCode; }
    public void setResponseCode(int responseCode) { this.responseCode = responseCode; }

    public String getResponseDiscription() { return responseDiscription; }
    public void setResponseDiscription(String responseDiscription) { this.responseDiscription = responseDiscription; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
