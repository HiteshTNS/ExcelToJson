package com.sg.srvc.vendormngt.dto;

import lombok.Data;

import java.util.List;
@Data
public class VimInvoiceResponseDTO {
    private int vimInvoiceId;
    private String correlationId;
    private List<InvoiceDTO> invoiceList;

    // getters and setters
}