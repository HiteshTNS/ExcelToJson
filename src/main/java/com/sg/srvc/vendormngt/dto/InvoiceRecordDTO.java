package com.sg.srvc.vendormngt.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class InvoiceRecordDTO {
    private String invoiceNumber;
    private String claimNumber;
    private Map<String, Object> request;  // Replaces RecJsonDTO
    private String status;
    private String statusDescription;
    private String createdBy;
    private LocalDateTime createdDate;

    // Getters and setters
}
