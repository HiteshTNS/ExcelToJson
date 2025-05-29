package com.sg.srvc.vendormngt.dto;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class InvoiceDTO {
    private String invoiceNumber;
    private String claimNumber;
    private RecJsonDTO recJson;
    private String status;
    private String statusDescription;
    private String createdBy;
    private LocalDateTime createdDate;

    // getters and setters
}