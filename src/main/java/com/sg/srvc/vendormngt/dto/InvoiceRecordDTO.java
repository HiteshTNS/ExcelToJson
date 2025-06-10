package com.sg.srvc.vendormngt.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceRecordDTO {
    private String invoiceNumber;
    private String claimNumber;
    private Map<String, Object> recJson;  // Replaces RecJsonDTO
    private String status;
    private String statusDescription;
    private String createdBy;
    private LocalDateTime createdDate;

    // Getters and setters
}
