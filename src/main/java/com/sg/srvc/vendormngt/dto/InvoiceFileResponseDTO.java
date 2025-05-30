package com.sg.srvc.vendormngt.dto;

import lombok.Data;

import java.util.List;
@Data
public class InvoiceFileResponseDTO {
    private int vimInvoiceId;
    private String correlationId;
    private List<InvoiceRecordDTO> invoiceList;

    // getters and setters
}